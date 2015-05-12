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
package org.olat.modules.reminder.manager;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.velocity.VelocityContext;
import org.olat.core.helpers.Settings;
import org.olat.core.id.Identity;
import org.olat.core.id.User;
import org.olat.core.id.UserConstants;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.Formatter;
import org.olat.core.util.StringHelper;
import org.olat.core.util.mail.ContactList;
import org.olat.core.util.mail.MailBundle;
import org.olat.core.util.mail.MailContext;
import org.olat.core.util.mail.MailContextImpl;
import org.olat.core.util.mail.MailManager;
import org.olat.core.util.mail.MailTemplate;
import org.olat.core.util.mail.MailerResult;
import org.olat.core.util.xml.XStreamHelper;
import org.olat.modules.reminder.Reminder;
import org.olat.modules.reminder.ReminderRule;
import org.olat.modules.reminder.ReminderService;
import org.olat.modules.reminder.SentReminder;
import org.olat.modules.reminder.model.ReminderImpl;
import org.olat.modules.reminder.model.ReminderInfos;
import org.olat.modules.reminder.model.ReminderRuleImpl;
import org.olat.modules.reminder.model.ReminderRules;
import org.olat.modules.reminder.rule.DateRuleSPI;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryRef;
import org.olat.user.UserManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.thoughtworks.xstream.XStream;

/**
 * 
 * Initial date: 08.04.2015<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
@Service
public class ReminderServiceImpl implements ReminderService {
	
	private static final OLog log = Tracing.createLoggerFor(ReminderServiceImpl.class);
	private static final XStream ruleXStream = XStreamHelper.createXStreamInstance();
	static {
		ruleXStream.alias("rule", org.olat.modules.reminder.model.ReminderRuleImpl.class);
		ruleXStream.alias("rules", org.olat.modules.reminder.model.ReminderRules.class);
	}
	
	@Autowired
	private ReminderDAO reminderDao;
	@Autowired
	private MailManager mailManager;
	@Autowired
	private UserManager userManager;
	@Autowired
	private ReminderRuleEngine ruleEngine;
	
	@Override
	public Reminder createReminder(RepositoryEntry entry, Identity creator) {
		return reminderDao.createReminder(entry, creator);
	}
	
	@Override
	public Reminder save(Reminder reminder) {
		//start optimization
		optimizeStartDate(reminder);
		return reminderDao.save(reminder);
	}
	
	private void optimizeStartDate(Reminder reminder) {
		Date startDate = null;
		String configuration = reminder.getConfiguration();
		if(StringHelper.containsNonWhitespace(configuration)) {
			ReminderRules rules = toRules(configuration);
			for(ReminderRule rule:rules.getRules()) {
				if(ReminderRuleEngine.DATE_RULE_TYPE.equals(rule.getType()) && rule instanceof ReminderRuleImpl) {
					ReminderRuleImpl r = (ReminderRuleImpl)rule;
					if(DateRuleSPI.AFTER.equals(r.getOperator()) && StringHelper.containsNonWhitespace(r.getRightOperand())) {
						try {
							Date date = Formatter.parseDatetime(r.getRightOperand());
							if(startDate == null) {
								startDate = date;
							} else if(startDate.compareTo(date) > 0) {
								startDate = date;
							}
						} catch (ParseException e) {
							log.error("", e);
						}
					}
				}
			}
		}		
		((ReminderImpl)reminder).setStartDate(startDate);
	}
	
	@Override
	public Reminder loadByKey(Long key) {
		return reminderDao.loadByKey(key);
	}
	
	@Override
	public List<ReminderInfos> getReminderInfos(RepositoryEntryRef entry) {
		return reminderDao.getReminderInfos(entry);
	}
	
	@Override
	public Reminder duplicate(Reminder toCopy) {
		return reminderDao.duplicate(toCopy);
	}
	
	@Override
	public void delete(Reminder reminder) {
		reminderDao.delete(reminder);
	}

	@Override
	public List<SentReminder> getSentReminders(Reminder reminder) {
		return reminderDao.getSendReminders(reminder);
	}

	@Override
	public List<SentReminder> getSentReminders(RepositoryEntryRef entry) {
		return reminderDao.getSendReminders(entry);
	}

	@Override
	public String toXML(ReminderRules rules) {
		return ruleXStream.toXML(rules);
	}
	
	@Override
	public ReminderRules toRules(String rulesXml) {
		return (ReminderRules)ruleXStream.fromXML(rulesXml);
	}
	
	@Override
	public void remindAll() {
		Date now = new Date();
		List<Reminder> reminders = reminderDao.getReminders(now);
		for(Reminder reminder:reminders) {
			sendReminder(reminder);
		}
	}

	@Override
	public MailerResult sendReminder(Reminder reminder) {
		List<Identity> identitiesToRemind = ruleEngine.evaluate(reminder, false);
		return sendReminder(reminder, identitiesToRemind);
	}
	
	@Override
	public MailerResult sendReminder(Reminder reminder, List<Identity> identitiesToRemind) {
		RepositoryEntry entry = reminder.getEntry();
		ContactList contactList = new ContactList("Infos");
		contactList.addAllIdentites(identitiesToRemind);
		
		MailContext context = new MailContextImpl("[RepositoryEntry:" + entry.getKey() + "]");
		String subject = "Reminder";
		String body = reminder.getEmailBody();
		String metaId = UUID.randomUUID().toString();
		String url = Settings.getServerContextPathURI() + "/url/RepositoryEntry/" + entry.getKey();

		MailerResult overviewResult = new MailerResult();
		ReminderTemplate template = new ReminderTemplate(subject, body, url, entry);

		for(Identity identityToRemind:identitiesToRemind) {
			String status;
			MailBundle bundle = mailManager.makeMailBundle(context, identityToRemind, template, null, metaId, overviewResult);
			MailerResult result = mailManager.sendMessage(bundle);
			overviewResult.append(result);
			
			List<Identity> failedIdentities = result.getFailedIdentites();
			if(failedIdentities != null && failedIdentities.contains(identityToRemind)) {
				status = "error";
			} else {
				status = "ok";
			}
			reminderDao.markAsSend(reminder, identityToRemind, status);
		}
		
		return overviewResult;
	}
	
	private class ReminderTemplate extends MailTemplate {
		
		private final String url;
		private final RepositoryEntry entry;
		
		public ReminderTemplate(String subjectTemplate, String bodyTemplate, String url, RepositoryEntry entry) {
			super(subjectTemplate, bodyTemplate, null);
			this.url = url;
			this.entry = entry;
		}

		@Override
		public void putVariablesInMailContext(VelocityContext vContext, Identity recipient) {
			User user = recipient.getUser();
			vContext.put("firstname", user.getProperty(UserConstants.FIRSTNAME, null));
			vContext.put(UserConstants.FIRSTNAME, user.getProperty(UserConstants.FIRSTNAME, null));
			vContext.put("lastname", user.getProperty(UserConstants.LASTNAME, null));
			vContext.put(UserConstants.LASTNAME, user.getProperty(UserConstants.LASTNAME, null));
			String fullName = userManager.getUserDisplayName(recipient);
			vContext.put("fullname", fullName);
			vContext.put("fullName", fullName);
			vContext.put("login", user.getProperty(UserConstants.EMAIL, null));
			// Put variables from greater context
			if(entry != null) {
				vContext.put("courseurl", url);
				vContext.put("coursename", entry.getDisplayname());
				vContext.put("coursedescription", entry.getDescription());
			}
		}
	}
}
