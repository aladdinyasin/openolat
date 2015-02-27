/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */

package org.olat.ldap.manager;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.commons.lang.ArrayUtils;
import org.olat.admin.user.delete.service.UserDeletionManager;
import org.olat.basesecurity.Authentication;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityModule;
import org.olat.basesecurity.Constants;
import org.olat.basesecurity.GroupRoles;
import org.olat.basesecurity.SecurityGroup;
import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.services.taskexecutor.TaskExecutorManager;
import org.olat.core.gui.control.Event;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.core.id.User;
import org.olat.core.id.UserConstants;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.StringHelper;
import org.olat.core.util.WorkThreadInformations;
import org.olat.core.util.coordinate.Coordinator;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.event.GenericEventListener;
import org.olat.core.util.mail.MailHelper;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupManagedFlag;
import org.olat.group.BusinessGroupService;
import org.olat.group.manager.BusinessGroupRelationDAO;
import org.olat.group.model.SearchBusinessGroupParams;
import org.olat.ldap.LDAPConstants;
import org.olat.ldap.LDAPError;
import org.olat.ldap.LDAPEvent;
import org.olat.ldap.LDAPLoginManager;
import org.olat.ldap.LDAPLoginModule;
import org.olat.ldap.LDAPSyncConfiguration;
import org.olat.ldap.model.LDAPGroup;
import org.olat.ldap.model.LDAPUser;
import org.olat.ldap.ui.LDAPAuthenticationController;
import org.olat.user.UserManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Description: This manager handles  communication between LDAP and OLAT. LDAP access is done by JNDI.
 * The synching is done only on node 1 of a cluster.
 * <p>
 * LDAPLoginMangerImpl
 * <p>
 * 
 * @author Maurus Rohrer
 */
@Service("org.olat.ldap.LDAPLoginManager")
public class LDAPLoginManagerImpl implements LDAPLoginManager, GenericEventListener {
	
	private static final OLog log = Tracing.createLoggerFor(LDAPLoginManagerImpl.class);

	private static final String TIMEOUT_KEY = "com.sun.jndi.ldap.connect.timeout";
	private static boolean batchSyncIsRunning = false;
	private static Date lastSyncDate = null; // first sync is always a full sync
	
	private Coordinator coordinator;
	private TaskExecutorManager taskExecutorManager;
	
	@Autowired
	private DB dbInstance;
	@Autowired
	private LDAPDAO ldapDao;
	@Autowired
	private UserManager userManager;
	@Autowired
	private BaseSecurity securityManager;
	@Autowired
	private LDAPLoginModule ldapLoginModule;
	@Autowired
	private LDAPSyncConfiguration syncConfiguration;
	@Autowired
	private UserDeletionManager userDeletionManager;
	@Autowired
	private BusinessGroupService businessGroupService;
	@Autowired
	private BusinessGroupRelationDAO businessGroupRelationDao;

	@Autowired
	public LDAPLoginManagerImpl(CoordinatorManager coordinatorManager, TaskExecutorManager taskExecutorManager) {
		this.coordinator = coordinatorManager.getCoordinator();
		this.taskExecutorManager = taskExecutorManager;
		coordinator.getEventBus().registerFor(this, null, ldapSyncLockOres);
	}

	@Override
	public void event(Event event) {
		if(event instanceof LDAPEvent) {
			if(LDAPEvent.SYNCHING.equals(event.getCommand())) {
				batchSyncIsRunning = true;
			} else if(LDAPEvent.SYNCHING_ENDED.equals(event.getCommand())) {
				batchSyncIsRunning = false;
				lastSyncDate = ((LDAPEvent)event).getTimestamp();
			} else if(LDAPEvent.DO_SYNCHING.equals(event.getCommand())) {
				doHandleBatchSync(false);
			} else if(LDAPEvent.DO_FULL_SYNCHING.equals(event.getCommand())) {
				doHandleBatchSync(true);
			}
		}
	}
	
	private void doHandleBatchSync(final boolean full) {
		//fxdiff: also run on nodes != 1 as nodeid = tomcat-id in fx-environment
//		if(WebappHelper.getNodeId() != 1) return;
		
		Runnable batchSyncTask = new Runnable() {
			public void run() {
				LDAPError errors = new LDAPError();
				doBatchSync(errors, full);
			}				
		};
		taskExecutorManager.execute(batchSyncTask);		
	}

	/**
	 * Connect to the LDAP server with System DN and Password
	 * 
	 * Configuration: LDAP URL = ldapContext.xml (property=ldapURL) System DN =
	 * ldapContext.xml (property=ldapSystemDN) System PW = ldapContext.xml
	 * (property=ldapSystemPW)
	 * 
	 * @return The LDAP connection (LdapContext) or NULL if connect fails
	 * 
	 * @throws NamingException
	 */
	public LdapContext bindSystem() {
		// set LDAP connection attributes
		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, ldapLoginModule.getLdapUrl());
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, ldapLoginModule.getLdapSystemDN());
		env.put(Context.SECURITY_CREDENTIALS, ldapLoginModule.getLdapSystemPW());
		if(ldapLoginModule.getLdapConnectionTimeout() != null) {
			env.put(TIMEOUT_KEY, ldapLoginModule.getLdapConnectionTimeout().toString());
		}

		// check ssl
		if (ldapLoginModule.isSslEnabled()) {
			enableSSL(env);
		}

		try {
			InitialLdapContext ctx = new InitialLdapContext(env, new Control[]{});
			ctx.getConnectControls();
			return ctx;
		} catch (NamingException e) {
			log.error("NamingException when trying to bind system with DN::" + ldapLoginModule.getLdapSystemDN() + " and PW::"
					+ ldapLoginModule.getLdapSystemPW() + " on URL::" + ldapLoginModule.getLdapUrl(), e);
			return null;
		} catch (Exception e) {
			log.error("Exception when trying to bind system with DN::" + ldapLoginModule.getLdapSystemDN() + " and PW::"
					+ ldapLoginModule.getLdapSystemPW() + " on URL::" + ldapLoginModule.getLdapUrl(), e);
			return null;
		}

	}

	/**
	 * 
	 * Connect to LDAP with the User-Name and Password given as parameters
	 * 
	 * Configuration: LDAP URL = ldapContext.xml (property=ldapURL) LDAP Base =
	 * ldapContext.xml (property=ldapBase) LDAP Attributes Map =
	 * ldapContext.xml (property=userAttrs)
	 * 
	 * 
	 * @param uid The users LDAP login name (can't be null)
	 * @param pwd The users LDAP password (can't be null)
	 * 
	 * @return After successful bind Attributes otherwise NULL
	 * 
	 * @throws NamingException
	 */
	@Override
	public Attributes bindUser(String uid, String pwd, LDAPError errors) {
		// get user name, password and attributes
		String ldapUrl = ldapLoginModule.getLdapUrl();
		String[] userAttr = syncConfiguration.getUserAttributes();

		if (uid == null || pwd == null) {
			if (log.isDebug()) log.debug("Error when trying to bind user, missing username or password. Username::" + uid + " pwd::" + pwd);
			errors.insert("Username and password must be selected");
			return null;
		}
		
		LdapContext ctx = bindSystem();
		if (ctx == null) {
			errors.insert("LDAP connection error");
			return null;
		}
		String userDN = ldapDao.searchUserDN(uid, ctx);
		if (userDN == null) {
			log.info("Error when trying to bind user with username::" + uid + " - user not found on LDAP server"
					+ (ldapLoginModule.isCacheLDAPPwdAsOLATPwdOnLogin() ? ", trying with OLAT login provider" : ""));
			errors.insert("Username or password incorrect");
			return null;
		}
		
		// Ok, so far so good, user exists. Now try to fetch attributes using the
		// users credentials
		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, ldapUrl);
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, userDN);
		env.put(Context.SECURITY_CREDENTIALS, pwd);
		if(ldapLoginModule.getLdapConnectionTimeout() != null) {
			env.put(TIMEOUT_KEY, ldapLoginModule.getLdapConnectionTimeout().toString());
		}
		if(ldapLoginModule.isSslEnabled()) {
			enableSSL(env);
		}

		try {
			Control[] connectCtls = new Control[]{};
			LdapContext userBind = new InitialLdapContext(env, connectCtls);
			Attributes attributes = userBind.getAttributes(userDN, userAttr);
			userBind.close();
			return attributes;
		} catch (AuthenticationException e) {
			log.info("Error when trying to bind user with username::" + uid + " - invalid LDAP password");
			errors.insert("Username or password incorrect");
			return null;
		} catch (NamingException e) {
			log.error("NamingException when trying to get attributes after binding user with username::" + uid, e);
			errors.insert("Username or password incorrect");
			return null;
		}
	}
	
	/**
	 * Change the password on the LDAP server.
	 * @see org.olat.ldap.LDAPLoginManager#changePassword(org.olat.core.id.Identity, java.lang.String, org.olat.ldap.LDAPError)
	 */
	@Override
	public boolean changePassword(Identity identity, String pwd, LDAPError errors) {
		String uid = identity.getName();
		String ldapUserPasswordAttribute = syncConfiguration.getLdapUserPasswordAttribute();
		try {
			DirContext ctx = bindSystem();
			String dn = ldapDao.searchUserDN(uid, ctx);
			
			ModificationItem [] modificationItems = new ModificationItem [ 1 ];
			
			Attribute userPasswordAttribute;
			if(ldapLoginModule.isActiveDirectory()) {
				//active directory need the password enquoted and unicoded (but little-endian)
				String quotedPassword = "\"" + pwd + "\"";
				char unicodePwd[] = quotedPassword.toCharArray();
				byte pwdArray[] = new byte[unicodePwd.length * 2];
				for (int i=0; i<unicodePwd.length; i++) {
					pwdArray[i*2 + 1] = (byte) (unicodePwd[i] >>> 8);
					pwdArray[i*2 + 0] = (byte) (unicodePwd[i] & 0xff);
				}
				userPasswordAttribute = new BasicAttribute ( ldapUserPasswordAttribute, pwdArray );
			} else {
				userPasswordAttribute = new BasicAttribute ( ldapUserPasswordAttribute, pwd );
			}

			modificationItems [ 0 ] = new ModificationItem ( DirContext.REPLACE_ATTRIBUTE, userPasswordAttribute );
			ctx.modifyAttributes ( dn, modificationItems );
			ctx.close();
			return true;
		} catch (NamingException e) {
			log.error("NamingException when trying to change password with username::" + uid, e);
			errors.insert("Cannot change the password");
			return false;
		} catch(Exception e) {
			log.error("Unexpected exception when trying to change password with username::" + uid, e);
			errors.insert("Cannot change the password");
			return false;
		}
	}

	/**
	 * Delete all Identities in List and removes them from LDAPSecurityGroup
	 * 
	 * @param identityList List of Identities to delete
	 */
	@Override
	public void deletIdentities(List<Identity> identityList) {
		SecurityGroup secGroup = securityManager.findSecurityGroupByName(LDAPConstants.SECURITY_GROUP_LDAP);
		
		for (Identity identity:  identityList) {
			securityManager.removeIdentityFromSecurityGroup(identity, secGroup);
			userDeletionManager.deleteIdentity(identity);
			dbInstance.intermediateCommit();
		}
	}

	/**
	 * Sync all OLATPropertys in Map of Identity
	 * 
	 * @param olatPropertyMap Map of changed OLAT properties
	 *          (OLATProperty,LDAPValue)
	 * @param identity Identity to sync
	 */
	@Override
	public void syncUser(Map<String, String> olatPropertyMap, Identity identity) {
		if (identity == null) {
			log.warn("Identiy is null - should not happen", null);
			return;
		}
		User user = identity.getUser();
		// remove user identifyer - can not be changed later
		olatPropertyMap.remove(LDAPConstants.LDAP_USER_IDENTIFYER);
		// remove attributes that are defined as sync-only-on-create
		Set<String> syncOnlyOnCreateProperties = syncConfiguration.getSyncOnlyOnCreateProperties();
		if (syncOnlyOnCreateProperties != null) {
			for (String syncOnlyOnCreateKey : syncOnlyOnCreateProperties) {
				olatPropertyMap.remove(syncOnlyOnCreateKey);
			}			
		}

		for(Map.Entry<String, String> keyValuePair : olatPropertyMap.entrySet()) {
			String propName = keyValuePair.getKey();
			String value = keyValuePair.getValue();
			if(value == null) {
				if(user.getProperty(propName, null) != null) {
					log.debug("removed property " + propName + " for identity " + identity);
					user.setProperty(propName, value);
				}
			} else {
				user.setProperty(propName, value);
			}
		}

		// Add static user properties from the configuration
		Map<String, String> staticProperties = syncConfiguration.getStaticUserProperties();
		if (staticProperties != null && staticProperties.size() > 0) {
			for (Map.Entry<String, String> staticProperty : staticProperties.entrySet()) {
				user.setProperty(staticProperty.getKey(), staticProperty.getValue());
			}
		}
		userManager.updateUser(user);
	}

	/**
	 * Creates User in OLAT and ads user to LDAP securityGroup Required Attributes
	 * have to be checked before this method.
	 * 
	 * @param userAttributes Set of LDAP Attribute of User to be created
	 */
	@Override
	public Identity createAndPersistUser(Attributes userAttributes) {
		// Get and Check Config
		String[] reqAttrs = syncConfiguration.checkRequestAttributes(userAttributes);
		if (reqAttrs != null) {
			log.warn("Can not create and persist user, the following attributes are missing::" + ArrayUtils.toString(reqAttrs), null);
			return null;
		}
		
		String uid = getAttributeValue(userAttributes.get(syncConfiguration
				.getOlatPropertyToLdapAttribute(LDAPConstants.LDAP_USER_IDENTIFYER)));
		String email = getAttributeValue(userAttributes.get(syncConfiguration.getOlatPropertyToLdapAttribute(UserConstants.EMAIL)));
		// Lookup user
		if (securityManager.findIdentityByName(uid) != null) {
			log.error("Can't create user with username='" + uid + "', this username does already exist in OLAT database", null);
			return null;
		}
		if (!MailHelper.isValidEmailAddress(email)) {
			// needed to prevent possibly an AssertException in findIdentityByEmail breaking the sync!
			log.error("Cannot try to lookup user " + uid + " by email with an invalid email::" + email, null);
			return null;
		}
		if (userManager.userExist(email) ) {
			log.error("Can't create user with email='" + email + "', a user with that email does already exist in OLAT database", null);
			return null;
		}
		
		// Create User (first and lastname is added in next step)
		User user = userManager.createUser(null, null, email);
		// Set User Property's (Iterates over Attributes and gets OLAT Property out
		// of olatexconfig.xml)
		NamingEnumeration<? extends Attribute> neAttr = userAttributes.getAll();
		try {
			while (neAttr.hasMore()) {
				Attribute attr = neAttr.next();
				String olatProperty = mapLdapAttributeToOlatProperty(attr.getID());
				if (!attr.getID().equalsIgnoreCase(syncConfiguration.getOlatPropertyToLdapAttribute(LDAPConstants.LDAP_USER_IDENTIFYER)) ) {
					String ldapValue = getAttributeValue(attr);
					if (olatProperty == null || ldapValue == null) continue;
					user.setProperty(olatProperty, ldapValue);
				} 
			}
			// Add static user properties from the configuration
			Map<String, String> staticProperties = syncConfiguration.getStaticUserProperties();
			if (staticProperties != null && staticProperties.size() > 0) {
				for (Entry<String, String> staticProperty : staticProperties.entrySet()) {
					user.setProperty(staticProperty.getKey(), staticProperty.getValue());
				}
			}
		} catch (NamingException e) {
			log.error("NamingException when trying to create and persist LDAP user with username::" + uid, e);
			return null;
		} catch (Exception e) {
			// catch any exception here to properly log error
			log.error("Unknown exception when trying to create and persist LDAP user with username::" + uid, e);
			return null;
		}

		// Create Identity
		Identity identity = securityManager.createAndPersistIdentityAndUser(uid, null, user, LDAPAuthenticationController.PROVIDER_LDAP, uid);
		// Add to SecurityGroup LDAP
		SecurityGroup secGroup = securityManager.findSecurityGroupByName(LDAPConstants.SECURITY_GROUP_LDAP);
		securityManager.addIdentityToSecurityGroup(identity, secGroup);
		// Add to SecurityGroup OLATUSERS
		secGroup = securityManager.findSecurityGroupByName(Constants.GROUP_OLATUSERS);
		securityManager.addIdentityToSecurityGroup(identity, secGroup);
		log.info("Created LDAP user username::" + uid);
		return identity;
	}
	
	

	/**
	 * Checks if LDAP properties are different then OLAT properties of a User. If
	 * they are different a Map (OlatPropertyName,LDAPValue) is returned.
	 * 
	 * @param attributes Set of LDAP Attribute of Identity
	 * @param identity Identity to compare
	 * 
	 * @return Map(OlatPropertyName,LDAPValue) of properties Identity, where
	 *         property has changed. NULL is returned it no attributes have to be synced
	 */
	@SuppressWarnings("unchecked")
	public Map<String, String> prepareUserPropertyForSync(Attributes attributes, Identity identity) {
		Map<String, String> olatPropertyMap = new HashMap<String, String>();
		User user = identity.getUser();
		NamingEnumeration<Attribute> neAttrs = (NamingEnumeration<Attribute>) attributes.getAll();
		try {
			while (neAttrs.hasMore()) {
				Attribute attr = neAttrs.next();
				String olatProperty = mapLdapAttributeToOlatProperty(attr.getID());
				if(olatProperty == null) {
					continue;
				}
				String ldapValue = getAttributeValue(attr);
				String olatValue = user.getProperty(olatProperty, null);
				if (olatValue == null) {
					// new property or user ID (will always be null, pseudo property)
					olatPropertyMap.put(olatProperty, ldapValue);
				} else {
					if (ldapValue.compareTo(olatValue) != 0) {
						olatPropertyMap.put(olatProperty, ldapValue);
					}
				}
			}
			if (olatPropertyMap.size() == 1 && olatPropertyMap.get(LDAPConstants.LDAP_USER_IDENTIFYER) != null) {
				log.debug("propertymap for identity " + identity.getName() + " contains only userID, NOTHING TO SYNC!");
				return null;
			} else {
				log.debug("propertymap for identity " + identity.getName() + " contains " + olatPropertyMap.size() + " items (" + olatPropertyMap.keySet() + ") to be synced later on");
				return olatPropertyMap;
			}

		} catch (NamingException e) {
			log.error("NamingException when trying to prepare user properties for LDAP sync", e);
			return null;
		}
	}
	
	/**
	 * Maps LDAP Attributes to the OLAT Property 
	 * 
	 * Configuration: LDAP Attributes Map = ldapContext.xml (property=userAttrs)
	 * 
	 * @param attrID LDAP Attribute
	 * @return OLAT Property
	 */
	private String mapLdapAttributeToOlatProperty(String attrID) {
		Map<String, String> userAttrMapper = syncConfiguration.getUserAttributeMap();
		String olatProperty = userAttrMapper.get(attrID);
		return olatProperty;
	}
	
	/**
	 * Extracts Value out of LDAP Attribute
	 * 
	 * 
	 * @param attribute LDAP Naming Attribute 
	 * @return String value of Attribute, null on Exception
	 * 
	 * @throws NamingException
	 */
	private String getAttributeValue(Attribute attribute) {
		try {
			String attrValue = (String)attribute.get();
			return attrValue;
		} catch (NamingException e) {
			log.error("NamingException when trying to get attribute value for attribute::" + attribute, e);
			return null;
		}
	}

	/**
	 * Searches for Identity in OLAT.
	 * 
	 * @param uid Name of Identity
	 * @param errors LDAPError Object if user exits but not member of
	 *          LDAPSecurityGroup
	 * 
	 * @return Identity if it's found and member of LDAPSecurityGroup, null
	 *         otherwise (if user exists but not managed by LDAP, error Object is
	 *         modified)
	 */
	public Identity findIdentyByLdapAuthentication(String uid, LDAPError errors) {
		Identity identity = securityManager.findIdentityByName(uid);
		if (identity == null) {
			return null;
		} else {
			SecurityGroup ldapGroup = securityManager.findSecurityGroupByName(LDAPConstants.SECURITY_GROUP_LDAP);
			if (ldapGroup == null) {
				log.error("Error getting user from OLAT security group '" + LDAPConstants.SECURITY_GROUP_LDAP + "' : group does not exist", null);
				return null;
			}
			if (securityManager.isIdentityInSecurityGroup(identity, ldapGroup)) {
				Authentication ldapAuth = securityManager.findAuthentication(identity, LDAPAuthenticationController.PROVIDER_LDAP);
				if(ldapAuth == null) {
					//BUG Fixe: update the user and test if it has a ldap provider
					securityManager.createAndPersistAuthentication(identity, LDAPAuthenticationController.PROVIDER_LDAP, identity.getName(), null, null);
				}
				return identity;
			}
			else {
				if (ldapLoginModule.isConvertExistingLocalUsersToLDAPUsers()) {
					// Add user to LDAP security group and add the ldap provider
					securityManager.createAndPersistAuthentication(identity, LDAPAuthenticationController.PROVIDER_LDAP, identity.getName(), null, null);
					securityManager.addIdentityToSecurityGroup(identity, ldapGroup);
					log.info("Found identity by LDAP username that was not yet in LDAP security group. Converted user::" + uid
							+ " to be an LDAP managed user");
					return identity;
				} else {
					errors.insert("findIdentyByLdapAuthentication: User with username::" + uid + " exist but not Managed by LDAP");
					return null;
				}
			}
		}
	}

	/**
	 * 
	 * Creates list of all OLAT Users which have been deleted out of the LDAP
	 * directory but still exits in OLAT
	 * 
	 * Configuration: Required Attributes = ldapContext.xml (property=reqAttrs)
	 * LDAP Base = ldapContext.xml (property=ldapBase)
	 * 
	 * @param syncTime The time to search in LDAP for changes since this time.
	 *          SyncTime has to formatted: JJJJMMddHHmm
	 * @param ctx The LDAP system connection, if NULL or closed NamingExecpiton is
	 *          thrown
	 * 
	 * @return Returns list of Identity from the user which have been deleted in
	 *         LDAP
	 * 
	 * @throws NamingException
	 */
	public List<Identity> getIdentitysDeletedInLdap(LdapContext ctx) {
		if (ctx == null) return null;
		// Find all LDAP Users
		String userID = syncConfiguration.getOlatPropertyToLdapAttribute(LDAPConstants.LDAP_USER_IDENTIFYER);
		String userFilter = syncConfiguration.getLdapUserFilter();
		final List<String> ldapList = new ArrayList<String>();
		
		ldapDao.searchInLdap(new LDAPVisitor() {
			@Override
			public void visit(SearchResult result) throws NamingException {
				Attributes attrs = result.getAttributes();
				NamingEnumeration<? extends Attribute> aEnum = attrs.getAll();
				while (aEnum.hasMore()) {
					Attribute attr = aEnum.next();
					// use lowercase username
					ldapList.add(attr.get().toString().toLowerCase());
				}
			}
		}, (userFilter == null ? "" : userFilter), new String[] { userID }, ctx);

		if (ldapList.isEmpty()) {
			log.warn("No users in LDAP found, can't create deletionList!!", null);
			return null;
		}

		// Find all User in OLAT, members of LDAPSecurityGroup
		SecurityGroup ldapGroup = securityManager.findSecurityGroupByName(LDAPConstants.SECURITY_GROUP_LDAP);
		if (ldapGroup == null) {
			log.error("Error getting users from OLAT security group '" + LDAPConstants.SECURITY_GROUP_LDAP + "' : group does not exist", null);
			return null;
		}

		List<Identity> identityListToDelete = new ArrayList<Identity>();
		List<Identity> olatListIdentity = securityManager.getIdentitiesOfSecurityGroup(ldapGroup);
		for (Identity ida:olatListIdentity) {
			// compare usernames with lowercase
			if (!ldapList.contains(ida.getName().toLowerCase())) {
				identityListToDelete.add(ida);
			}
		}
		return identityListToDelete;
	}

	/**
	 * Execute Batch Sync. Will update all Attributes of LDAP users in OLAt, create new users and delete users in OLAT.
	 * Can be configured in ldapContext.xml
	 * 
	 * @param LDAPError
	 * 
	 */
	@Override
	public boolean doBatchSync(LDAPError errors, boolean full) {
		//fxdiff: also run on nodes != 1 as nodeid = tomcat-id in fx-environment
//		if(WebappHelper.getNodeId() != 1) {
//			log.warn("Sync happens only on node 1", null);
//			return false;
//		}
		
		// o_clusterNOK
		// Synchronize on class so that only one thread can read the
		// batchSyncIsRunning flag Only this read operation is synchronized to not
		// block the whole execution of the do BatchSync method. The method is used
		// in automatic cron scheduler job and also in GUI controllers that can't
		// wait for the concurrent running request to finish first, an immediate
		// feedback about the concurrent job is needed. -> only synchronize on the
		// property read.
		synchronized (LDAPLoginManagerImpl.class) {
			if (batchSyncIsRunning) {
				// don't run twice, skip this execution
				log.info("LDAP user doBatchSync started, but another job is still running - skipping this sync");
				errors.insert("BatchSync already running by concurrent process");
				return false;
			}
		}
		
		WorkThreadInformations.setLongRunningTask("ldapSync");
		
		coordinator.getEventBus().fireEventToListenersOf(new LDAPEvent(LDAPEvent.SYNCHING), ldapSyncLockOres);
		
		if(full) {
			lastSyncDate = null;
		}
		
		LdapContext ctx = null;
		boolean success = false;
		try {
			acquireSyncLock();
			ctx = bindSystem();
			if (ctx == null) {
				errors.insert("LDAP connection ERROR");
				log.error("Error in LDAP batch sync: LDAP connection empty", null);
				freeSyncLock();
				success = false;
				return success;
			}
			Date timeBeforeSync = new Date();

			//check server capabilities
			// Get time before sync to have a save sync time when sync is successful
			String sinceSentence = (lastSyncDate == null ? " (full sync)" : " since last sync from " + lastSyncDate);
			doBatchSyncDeletedUsers(ctx, sinceSentence);
			// bind again to use an initial unmodified context. lookup of server-properties might fail otherwise!
			ctx.close();
			ctx = bindSystem();
			Map<String,LDAPUser> dnToIdentityKeyMap = new HashMap<>();
			List<LDAPUser> ldapUsers = doBatchSyncNewAndModifiedUsers(ctx, sinceSentence, dnToIdentityKeyMap, errors);
			ctx.close();
			ctx = bindSystem();
			//sync groups by LDAP groups or attributes
			doBatchSyncGroups(ctx, ldapUsers, dnToIdentityKeyMap, errors);
			//sync roles
			doBatchSyncRoles(ctx, ldapUsers, dnToIdentityKeyMap, errors);
			
			// update sync time and set running flag
			lastSyncDate = timeBeforeSync;
			
			ctx.close();
			success = true;
			return success;
		} catch (Exception e) {

			errors.insert("Unknown error");
			log.error("Error in LDAP batch sync, unknown reason", e);
			success = false;
			return success;
		} finally {
			WorkThreadInformations.unsetLongRunningTask("ldapSync");
			freeSyncLock();
			if(ctx != null) {
				try {
					ctx.close();
				} catch (NamingException e) {
					//try but failed silently
				}
			}
			LDAPEvent endEvent = new LDAPEvent(LDAPEvent.SYNCHING_ENDED);
			endEvent.setTimestamp(new Date());
			endEvent.setSuccess(success);
			endEvent.setErrors(errors);
			coordinator.getEventBus().fireEventToListenersOf(endEvent, ldapSyncLockOres);
		}
	}
	
	private void doBatchSyncGroups(LdapContext ctx, List<LDAPUser> ldapUsers, Map<String,LDAPUser> dnToIdentityKeyMap, LDAPError errors)
	throws NamingException {
		ctx.close();
		ctx = bindSystem();
		//sync groups by LDAP groups or attributes
		if(syncConfiguration.syncGroupWithLDAPGroup()) {
			doSyncLDAPGroups(ctx, dnToIdentityKeyMap, errors);
		}
		if(syncConfiguration.syncGroupWithAttribute()) {
			doSyncGroupAttribute(ldapUsers);
		}
	}
	
	private void doBatchSyncRoles(LdapContext ctx, List<LDAPUser> ldapUsers, Map<String,LDAPUser> dnToIdentityKeyMap, LDAPError errors)
	throws NamingException {
		ctx.close();
		ctx = bindSystem();
		
		//authors
		if(syncConfiguration.getAuthorsGroupBase() != null && syncConfiguration.getAuthorsGroupBase().size() > 0) {
			List<LDAPGroup> authorGroups = ldapDao.searchGroups(ctx, syncConfiguration.getAuthorsGroupBase());
			syncRole(ctx, authorGroups, Constants.GROUP_AUTHORS, dnToIdentityKeyMap, errors);
		}
		//user managers
		if(syncConfiguration.getUserManagersGroupBase() != null && syncConfiguration.getUserManagersGroupBase().size() > 0) {
			List<LDAPGroup> userManagerGroups = ldapDao.searchGroups(ctx, syncConfiguration.getUserManagersGroupBase());
			syncRole(ctx, userManagerGroups, Constants.GROUP_USERMANAGERS, dnToIdentityKeyMap, errors);
		}
		//group managers
		if(syncConfiguration.getGroupManagersGroupBase() != null && syncConfiguration.getGroupManagersGroupBase().size() > 0) {
			List<LDAPGroup> groupManagerGroups = ldapDao.searchGroups(ctx, syncConfiguration.getGroupManagersGroupBase());
			syncRole(ctx, groupManagerGroups, Constants.GROUP_GROUPMANAGERS, dnToIdentityKeyMap, errors);
		}
		//question pool managers
		if(syncConfiguration.getQpoolManagersGroupBase() != null && syncConfiguration.getQpoolManagersGroupBase().size() > 0) {
			List<LDAPGroup> qpoolManagerGroups = ldapDao.searchGroups(ctx, syncConfiguration.getQpoolManagersGroupBase());
			syncRole(ctx, qpoolManagerGroups, Constants.GROUP_POOL_MANAGER, dnToIdentityKeyMap, errors);
		}
		//learning resource manager
		if(syncConfiguration.getLearningResourceManagersGroupBase() != null && syncConfiguration.getLearningResourceManagersGroupBase().size() > 0) {
			List<LDAPGroup> resourceManagerGroups = ldapDao.searchGroups(ctx, syncConfiguration.getLearningResourceManagersGroupBase());
			syncRole(ctx, resourceManagerGroups, Constants.GROUP_INST_ORES_MANAGER, dnToIdentityKeyMap, errors);
		}
		
		
		int count = 0;
		boolean syncAuthor = StringHelper.containsNonWhitespace(syncConfiguration.getAuthorRoleAttribute())
				&& StringHelper.containsNonWhitespace(syncConfiguration.getAuthorRoleValue());
		boolean syncUserManager = StringHelper.containsNonWhitespace(syncConfiguration.getUserManagerRoleAttribute())
				&& StringHelper.containsNonWhitespace(syncConfiguration.getUserManagerRoleValue());
		boolean syncGroupManager = StringHelper.containsNonWhitespace(syncConfiguration.getGroupManagerRoleAttribute())
				&& StringHelper.containsNonWhitespace(syncConfiguration.getGroupManagerRoleValue());
		boolean syncQpoolManager = StringHelper.containsNonWhitespace(syncConfiguration.getQpoolManagerRoleAttribute())
				&& StringHelper.containsNonWhitespace(syncConfiguration.getQpoolManagerRoleValue());
		boolean syncLearningResourceManager = StringHelper.containsNonWhitespace(syncConfiguration.getLearningResourceManagerRoleAttribute())
				&& StringHelper.containsNonWhitespace(syncConfiguration.getLearningResourceManagerRoleValue());
		
		for(LDAPUser ldapUser:ldapUsers) {
			if(syncAuthor && ldapUser.isAuthor()) {
				syncRole(ldapUser, Constants.GROUP_AUTHORS);
				count++;
			}
			if(syncUserManager && ldapUser.isUserManager()) {
				syncRole(ldapUser, Constants.GROUP_USERMANAGERS);
				count++;
			}
			if(syncGroupManager && ldapUser.isGroupManager()) {
				syncRole(ldapUser, Constants.GROUP_GROUPMANAGERS);
				count++;
			}
			if(syncQpoolManager && ldapUser.isQpoolManager()) {
				syncRole(ldapUser, Constants.GROUP_POOL_MANAGER);
				count++;
			}
			if(syncLearningResourceManager && ldapUser.isLearningResourceManager()) {
				syncRole(ldapUser, Constants.GROUP_INST_ORES_MANAGER);
				count++;
			}
			if(count > 20) {
				dbInstance.commitAndCloseSession();
				count = 0;
			}
		}

		dbInstance.commitAndCloseSession();
	}
	
	private void syncRole(LdapContext ctx, List<LDAPGroup> groups, String role,
			Map<String,LDAPUser> dnToIdentityKeyMap, LDAPError errors) {
		if(groups == null || groups.isEmpty()) return;
		
		for(LDAPGroup group:groups) {
			List<String> members = group.getMembers();
			if(members != null && members.size() > 0) {
				for(String member:members) {
					LDAPUser ldapUser = getLDAPUser(ctx, member, dnToIdentityKeyMap, errors);
					if(ldapUser != null && ldapUser.getCachedIdentity() != null) {
						syncRole(ldapUser, role);
					}
				}
			}
			dbInstance.commitAndCloseSession();
		}	
	}

	private void syncRole(LDAPUser ldapUser, String role) {
		Identity identity = ldapUser.getCachedIdentity();
		List<String> roleList = securityManager.getRolesAsString(identity);
		if(!roleList.contains(role)) {
			Roles roles = securityManager.getRoles(identity);
			switch(role) {
				case Constants.GROUP_AUTHORS:
					roles = new Roles(roles.isOLATAdmin(), roles.isUserManager(), roles.isGroupManager(), true,
									false, roles.isInstitutionalResourceManager(), roles.isPoolAdmin(), false);
					securityManager.updateRoles(null, identity, roles);
					break;
				case Constants.GROUP_USERMANAGERS:
					roles = new Roles(roles.isOLATAdmin(), true, roles.isGroupManager(), roles.isAuthor(),
							false, roles.isInstitutionalResourceManager(), roles.isPoolAdmin(), false);
					securityManager.updateRoles(null, identity, roles);
					break;
				case Constants.GROUP_GROUPMANAGERS:
					roles = new Roles(roles.isOLATAdmin(), roles.isUserManager(), true, roles.isAuthor(),
							false, roles.isInstitutionalResourceManager(), roles.isPoolAdmin(), false);
					securityManager.updateRoles(null, identity, roles);
					break;
				case Constants.GROUP_POOL_MANAGER:
					roles = new Roles(roles.isOLATAdmin(), roles.isUserManager(), roles.isGroupManager(), roles.isAuthor(),
							false, roles.isInstitutionalResourceManager(), true, false);
					securityManager.updateRoles(null, identity, roles);
					break;
				case Constants.GROUP_INST_ORES_MANAGER:
					roles = new Roles(roles.isOLATAdmin(), roles.isUserManager(), roles.isGroupManager(), roles.isAuthor(),
							false, true, roles.isPoolAdmin(), false);
					securityManager.updateRoles(null, identity, roles);
					break;
			}
		}
	}
	
	private void doBatchSyncDeletedUsers(LdapContext ctx, String sinceSentence) {
		// create User to Delete List
		List<Identity> deletedUserList = getIdentitysDeletedInLdap(ctx);
		// delete old users
		if (deletedUserList == null || deletedUserList.size() == 0) {
			log.info("LDAP batch sync: no users to delete" + sinceSentence);
		} else {
			if (ldapLoginModule.isDeleteRemovedLDAPUsersOnSync()) {
				// check if more not more than the defined percentages of
				// users managed in LDAP should be deleted
				// if they are over the percentage, they will not be deleted
				// by the sync job
				SecurityGroup ldapGroup = securityManager.findSecurityGroupByName(LDAPConstants.SECURITY_GROUP_LDAP);
				List<Identity> olatListIdentity = securityManager.getIdentitiesOfSecurityGroup(ldapGroup);
				if (olatListIdentity.isEmpty())
					log.info("No users managed by LDAP, can't delete users");
				else {
					int prozente = (int) (((float)deletedUserList.size() / (float) olatListIdentity.size())*100);
					if (prozente >= ldapLoginModule.getDeleteRemovedLDAPUsersPercentage()) {
						log.info("LDAP batch sync: more than "
										+ ldapLoginModule.getDeleteRemovedLDAPUsersPercentage()
										+ "% of LDAP managed users should be deleted. Please use Admin Deletion Job. Or increase deleteRemovedLDAPUsersPercentage. "
										+ prozente
										+ "% tried to delete.");
					} else {
						// delete users
						deletIdentities(deletedUserList);
						log.info("LDAP batch sync: "
								+ deletedUserList.size() + " users deleted"
								+ sinceSentence);
					}
				}
			} else {
				// Do nothing, only log users to logfile
				StringBuilder users = new StringBuilder();
				for (Identity toBeDeleted : deletedUserList) {
					users.append(toBeDeleted.getName()).append(',');
				}
				log.info("LDAP batch sync: "
					+ deletedUserList.size()
					+ " users detected as to be deleted"
					+ sinceSentence
					+ ". Automatic deleting is disabled in LDAPLoginModule, delete these users manually::["
					+ users.toString() + "]");
			}
		}
	}
	
	private List<LDAPUser> doBatchSyncNewAndModifiedUsers(LdapContext ctx, String sinceSentence, Map<String,LDAPUser> dnToIdentityKeyMap, LDAPError errors) {
		// Get new and modified users from LDAP
		int count = 0;
		List<LDAPUser> ldapUserList = ldapDao.getUserAttributesModifiedSince(lastSyncDate, ctx);
		
		// Check for new and modified users
		List<LDAPUser> newLdapUserList = new ArrayList<LDAPUser>();
		Map<Identity, Map<String, String>> changedMapIdentityMap = new HashMap<Identity, Map<String, String>>();
		for (LDAPUser ldapUser: ldapUserList) {
			String user = null;
			try {
				Attributes userAttrs = ldapUser.getAttributes();
				String uidProp = syncConfiguration.getOlatPropertyToLdapAttribute(LDAPConstants.LDAP_USER_IDENTIFYER);
				user = getAttributeValue(userAttrs.get(uidProp));
				Identity identity = findIdentyByLdapAuthentication(user, errors);
				if (identity != null) {
					Map<String, String> changedAttrMap = prepareUserPropertyForSync(userAttrs, identity);
					if (changedAttrMap != null) {
						changedMapIdentityMap.put(identity, changedAttrMap);
					}
					if(StringHelper.containsNonWhitespace(ldapUser.getDn())) {
						dnToIdentityKeyMap.put(ldapUser.getDn(), ldapUser);
						ldapUser.setCachedIdentity(identity);
					}
				} else if (errors.isEmpty()) {
					String[] reqAttrs = syncConfiguration.checkRequestAttributes(userAttrs);
					if (reqAttrs == null) {
						newLdapUserList.add(ldapUser);
					} else {
						log.warn("Error in LDAP batch sync: can't create user with username::" + user + " : missing required attributes::"
							+ ArrayUtils.toString(reqAttrs), null);
					}
				} else {
					log.warn(errors.get(), null);
				}
				
				if(++count % 20 == 0) {
					dbInstance.intermediateCommit();
				}
			} catch (Exception e) {
				// catch here to go on with other users on exeptions!
				log.error("some error occured in looping over set of changed user-attributes, actual user " + user + ". Will still continue with others.", e);
			}
		}
		
		// sync existing users
		if (changedMapIdentityMap == null || changedMapIdentityMap.isEmpty()) {
			log.info("LDAP batch sync: no users to sync" + sinceSentence);
		} else {
			for (Identity ident : changedMapIdentityMap.keySet()) {
				// sync user is exception save, no try/catch needed
				syncUser(changedMapIdentityMap.get(ident), ident);
				//REVIEW Identity are not saved???
				if(++count % 20 == 0) {
					dbInstance.intermediateCommit();
				}
			}
			log.info("LDAP batch sync: " + changedMapIdentityMap.size() + " users synced" + sinceSentence);
		}
		
		// create new users
		if (newLdapUserList.isEmpty()) {
			log.info("LDAP batch sync: no users to create" + sinceSentence);
		} else {			
			for (LDAPUser ldapUser: newLdapUserList) {
				Attributes userAttrs = ldapUser.getAttributes();
				try {
					Identity identity = createAndPersistUser(userAttrs);
					if(++count % 20 == 0) {
						dbInstance.intermediateCommit();
					}
					
					if(StringHelper.containsNonWhitespace(ldapUser.getDn())) {
						dnToIdentityKeyMap.put(ldapUser.getDn(), ldapUser);
						ldapUser.setCachedIdentity(identity);
					}
				} catch (Exception e) {
					// catch here to go on with other users on exeptions!
					log.error("some error occured while creating new users, actual userAttribs " + userAttrs + ". Will still continue with others.", e);
				}
			}
			log.info("LDAP batch sync: " + newLdapUserList.size() + " users created" + sinceSentence);
		}
		
		dbInstance.intermediateCommit();
		return ldapUserList;
	}
	
	private void doSyncGroupAttribute(List<LDAPUser> ldapUsers) {
		Map<String,List<LDAPUser>> externelIdToGroupMap = new HashMap<>();
		for(LDAPUser ldapUser:ldapUsers) {
			List<String> groupIds = ldapUser.getGroupIds();
			if(groupIds != null && groupIds.size() > 0) {
				Identity identity = ldapUser.getCachedIdentity();
				if(identity == null) {
					log.error("Identity with dn=" + ldapUser.getDn() + " not found");
				} else {
					for(String groupId:groupIds) {
						if(!externelIdToGroupMap.containsKey(groupId)) {
							externelIdToGroupMap.put(groupId, new ArrayList<LDAPUser>());
						}
						externelIdToGroupMap.get(groupId).add(ldapUser);
					}
				}
			}
		}
		
		for(Map.Entry<String, List<LDAPUser>> groupEntry:externelIdToGroupMap.entrySet()) {
			String externalId = groupEntry.getKey();
			List<LDAPUser> members = groupEntry.getValue();
			BusinessGroup managedGroup = getManagerBusinessGroup(externalId);
			if(managedGroup != null) {
				syncBusinessGroup(managedGroup, members);
			}
			dbInstance.commitAndCloseSession();
		}
	}
	
	private void syncBusinessGroup(BusinessGroup businessGroup, List<LDAPUser> members) {
		List<Identity> currentMembers = businessGroupRelationDao
				.getMembers(businessGroup, GroupRoles.coach.name(), GroupRoles.participant.name());
		
		for(LDAPUser member:members) {
			Identity memberIdentity = member.getCachedIdentity();
			syncMembership(businessGroup, memberIdentity, member.isCoach());
			currentMembers.remove(memberIdentity);
		}
		
		for(Identity currentMember:currentMembers) {
			List<String> roles = businessGroupRelationDao.getRoles(currentMember, businessGroup);
			for(String role:roles) {
				businessGroupRelationDao.removeRole(currentMember, businessGroup, role);
			}
		}
	}
	
	private void syncMembership(BusinessGroup businessGroup, Identity identity, boolean coach) {
		if(identity != null) {
			List<String> roles = businessGroupRelationDao.getRoles(identity, businessGroup);
			if(roles.isEmpty()) {
				if(coach) {
					businessGroupRelationDao.addRole(identity, businessGroup, GroupRoles.coach.name());
				} else {
					businessGroupRelationDao.addRole(identity, businessGroup, GroupRoles.participant.name());
				}
			} else if(coach && roles.size() == 1 && roles.contains(GroupRoles.coach.name())) {
				//coach and only coach, do nothing
			} else if(!coach && roles.size() == 1 && roles.contains(GroupRoles.participant.name())) {
				//participant and only participant, do nothing
			} else {
				boolean already = false;
				String mainRole = coach ? GroupRoles.coach.name() : GroupRoles.participant.name();
				for(String role:roles) {
					if(mainRole.equals(role)) {
						already = true;
					} else {
						businessGroupRelationDao.removeRole(identity, businessGroup, role);
					}
				}
				
				if(!already) {
					businessGroupRelationDao.addRole(identity, businessGroup, mainRole);
				}
			}
		}
	}
	
	private void doSyncLDAPGroups(LdapContext ctx, Map<String,LDAPUser> dnToIdentityKeyMap, LDAPError errors) {
		List<String> groupDNs = syncConfiguration.getLdapGroupBases();
		List<LDAPGroup> ldapGroups = ldapDao.searchGroups(ctx, groupDNs);

		for(LDAPGroup ldapGroup:ldapGroups) {
			String externalId = ldapGroup.getCommonName();
			BusinessGroup managedGroup = getManagerBusinessGroup(externalId);
			if(managedGroup != null) {
				syncBusinessGroup(ctx, ldapGroup, managedGroup, dnToIdentityKeyMap, errors);
			}
			dbInstance.commitAndCloseSession();
		}
	}
	
	private BusinessGroup getManagerBusinessGroup(String externalId) {
		SearchBusinessGroupParams params = new SearchBusinessGroupParams();
		params.setExternalId(externalId);
		List<BusinessGroup> businessGroups = businessGroupService.findBusinessGroups(params, null, 0, -1);
		
		BusinessGroup managedBusinessGroup;
		if(businessGroups.size() == 0) {
			String managedFlags = BusinessGroupManagedFlag.membersmanagement.name() + "," + BusinessGroupManagedFlag.delete.name();
			managedBusinessGroup = businessGroupService
					.createBusinessGroup(null, externalId, externalId, externalId, managedFlags, null, null, false, false, null);

		} else if(businessGroups.size() == 1) {
			managedBusinessGroup = businessGroups.get(0);
		} else {
			log.error(businessGroups.size() + " managed groups found with the following external id: " + externalId);
			managedBusinessGroup = null;
		}
		return managedBusinessGroup;
	}
	
	private void syncBusinessGroup(LdapContext ctx, LDAPGroup ldapGroup, BusinessGroup businessGroup, Map<String,LDAPUser> dnToIdentityKeyMap, LDAPError errors) {
		List<String> members = ldapGroup.getMembers();
		List<Identity> currentMembers = businessGroupRelationDao
				.getMembers(businessGroup, GroupRoles.coach.name(), GroupRoles.participant.name());

		int count = 0;
		for(String member:members) {
			LDAPUser ldapUser = getLDAPUser(ctx, member, dnToIdentityKeyMap, errors);
			if(ldapUser != null) {
				Identity identity = ldapUser.getCachedIdentity();
				syncMembership(businessGroup, identity, ldapUser.isCoach());
				currentMembers.remove(identity);
				if(++count % 20 == 0) {
					dbInstance.commitAndCloseSession();
				}
			}
		}
		
		for(Identity currentMember:currentMembers) {
			List<String> roles = businessGroupRelationDao.getRoles(currentMember, businessGroup);
			for(String role:roles) {
				businessGroupRelationDao.removeRole(currentMember, businessGroup, role);
				if(++count % 20 == 0) {
					dbInstance.commitAndCloseSession();
				}
			}
		}
	}
	
	private LDAPUser getLDAPUser(LdapContext ctx, String member, Map<String,LDAPUser> dnToIdentityKeyMap, LDAPError errors) {
		LDAPUser ldapUser = dnToIdentityKeyMap.get(member);

		Identity identity = ldapUser == null ? null : ldapUser.getCachedIdentity();
		if(identity == null) {
			String userFilter = syncConfiguration.getLdapUserFilter();
			String uidProp = syncConfiguration.getOlatPropertyToLdapAttribute(LDAPConstants.LDAP_USER_IDENTIFYER);

			String userDN = member;
			LDAPUserVisitor visitor = new LDAPUserVisitor(syncConfiguration);
			ldapDao.search(visitor, userDN, userFilter, syncConfiguration.getUserAttributes(), ctx);
			
			List<LDAPUser> ldapUserList = visitor.getLdapUserList();
			if(ldapUserList.size() == 1) {
				ldapUser = ldapUserList.get(0);
				Attributes userAttrs = ldapUser.getAttributes();
				String user = getAttributeValue(userAttrs.get(uidProp));
				identity = findIdentyByLdapAuthentication(user, errors);
				if(identity != null) {
					dnToIdentityKeyMap.put(userDN, ldapUser);
				}
			}
		}
		return ldapUser;
	}

	@Override
	public void doSyncSingleUser(Identity ident){
		LdapContext ctx = bindSystem();
		if (ctx == null) {
			log.error("could not bind to ldap", null);
		}		
		String userDN = ldapDao.searchUserDN(ident.getName(), ctx);

		final List<Attributes> ldapUserList = new ArrayList<Attributes>();
		// TODO: use userDN instead of filter to get users attribs
		ldapDao.searchInLdap(new LDAPVisitor() {
			@Override
			public void visit(SearchResult result) {
				Attributes resAttribs = result.getAttributes();
				log.debug("        found : " + resAttribs.size() + " attributes in result " + result.getName());
				ldapUserList.add(resAttribs);
			}
		}, userDN, syncConfiguration.getUserAttributes(), ctx);
		
		Attributes attrs = ldapUserList.get(0);
		Map<String, String> olatProToSync = prepareUserPropertyForSync(attrs, ident);
		if (olatProToSync != null) {
			syncUser(olatProToSync, ident);
		}
	}

	/**
	 * @see org.olat.ldap.LDAPLoginManager#getLastSyncDate()
	 */
	@Override
	public Date getLastSyncDate() {
		return lastSyncDate;
	}

	/**
	 * Internal helper to add the SSL protocol to the environment
	 * 
	 * @param env
	 */
	private void enableSSL(Hashtable<String, String> env) {
		env.put(Context.SECURITY_PROTOCOL, "ssl");
		if(StringHelper.containsNonWhitespace(ldapLoginModule.getTrustStoreLocation())) {
			System.setProperty("javax.net.ssl.trustStore", ldapLoginModule.getTrustStoreLocation());
		}
	}
	
	/**
	 * Acquire lock for administration jobs
	 * 
	 */
	@Override
	public synchronized boolean acquireSyncLock(){
		if(batchSyncIsRunning){
			return false;
		}
		batchSyncIsRunning=true;
		return true;
	}
	
	/**
	 * Release lock for administration jobs
	 * 
	 */
	@Override
	public synchronized void freeSyncLock() {
		batchSyncIsRunning = false;
	}

	/**
	 * remove all cached authentications for fallback-login. useful if users logged in first with a default pw and changed it outside in AD/LDAP, but OLAT doesn't know about.
	 * removing fallback-auths means login is only possible by AD/LDAP and if server is reachable!
	 * see FXOLAT-284
	 */
	@Override
	public void removeFallBackAuthentications() {
		if (ldapLoginModule.isCacheLDAPPwdAsOLATPwdOnLogin()){
			SecurityGroup ldapGroup = securityManager.findSecurityGroupByName(LDAPConstants.SECURITY_GROUP_LDAP);
			if (ldapGroup == null) {
				log.error("Error getting user from OLAT security group '" + LDAPConstants.SECURITY_GROUP_LDAP + "' : group does not exist", null);
			}
			List<Identity> ldapIdents = securityManager.getIdentitiesOfSecurityGroup(ldapGroup);
			log.info("found " + ldapIdents.size() + " identies in ldap security group");
			int count=0;
			for (Identity identity : ldapIdents) {
				Authentication auth = securityManager.findAuthentication(identity, BaseSecurityModule.getDefaultAuthProviderIdentifier());				
				if (auth!=null){
					securityManager.deleteAuthentication(auth);
					count++;
				}
				if (count % 20 == 0){
					dbInstance.intermediateCommit();
				}
			}
			log.info("removed cached authentications (fallback login provider: " + BaseSecurityModule.getDefaultAuthProviderIdentifier() + ") for " + count + " users.");			
		}
	}

	@Override
	public boolean isIdentityInLDAPSecGroup(Identity ident) {
		SecurityGroup ldapSecurityGroup = securityManager.findSecurityGroupByName(LDAPConstants.SECURITY_GROUP_LDAP);
		return ldapSecurityGroup != null && securityManager.isIdentityInSecurityGroup(ident, ldapSecurityGroup);
	}
}