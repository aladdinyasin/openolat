/**
* OLAT - Online Learning and Training<br>
* http://www.olat.org
* <p>
* Licensed under the Apache License, Version 2.0 (the "License"); <br>
* you may not use this file except in compliance with the License.<br>
* You may obtain a copy of the License at
* <p>
* http://www.apache.org/licenses/LICENSE-2.0
* <p>
* Unless required by applicable law or agreed to in writing,<br>
* software distributed under the License is distributed on an "AS IS" BASIS, <br>
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
* See the License for the specific language governing permissions and <br>
* limitations under the License.
* <p>
* Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.
*/

package org.olat.user;

import java.io.File;

import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.modules.bc.FolderConfig;
import org.olat.core.commons.services.image.ImageService;
import org.olat.core.commons.services.image.Size;
import org.olat.core.gui.media.FileMediaResource;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.id.Identity;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.FileUtils;
import org.olat.core.util.StringHelper;

/**
 * Description: <br>
 * TODO: alex Class Description
 * <P>
 * 
 * Initial Date: Sept 08, 2005 <br>
 * @author Alexander Schneider
 */
public class DisplayPortraitManager extends BasicManager implements UserDataDeletable {

	private static DisplayPortraitManager singleton;
	
	private static final String PORTRAIT_BIG_FILENAME = "portrait_big";
	private static final String PORTRAIT_SMALL_FILENAME = "portrait_small";
	// The following class names refer to CSS class names in olat.css 
	public static final String AVATAR_BIG_CSS_CLASS = "o_portrait_avatar";
	public static final String AVATAR_SMALL_CSS_CLASS = "o_portrait_avatar_small";
	public static final String DUMMY_BIG_CSS_CLASS = "o_portrait_dummy";
	public static final String DUMMY_SMALL_CSS_CLASS = "o_portrait_dummy_small";
	public static final String DUMMY_FEMALE_BIG_CSS_CLASS = "o_portrait_dummy_female_big";
	public static final String DUMMY_FEMALE_SMALL_CSS_CLASS = "o_portrait_dummy_female_small";
	public static final String DUMMY_MALE_BIG_CSS_CLASS = "o_portrait_dummy_male_big";
	public static final String DUMMY_MALE_SMALL_CSS_CLASS = "o_portrait_dummy_male_small";
	
	// If you change the following widths, don't forget to change them in basemod.scss as well.
	public static final int WIDTH_PORTRAIT_BIG = 100;  // 4-8 kbytes (jpeg)
	public static final int WIDTH_PORTRAIT_SMALL = 30; // 2-4
	
	/**
	 * [spring]
	 */
	private DisplayPortraitManager() {
		singleton = this;
	}
	
	/**
	 * Singleton pattern
	 * 
	 * @return instance
	 */
	public static DisplayPortraitManager getInstance() {
		return singleton;
	}

	public MediaResource getSmallPortraitResource(String username) {
		return getPortraitResource(username, PORTRAIT_SMALL_FILENAME);
	}
	public MediaResource getSmallPortraitResource(Long identityKey) {
		return getPortraitResource(identityKey, PORTRAIT_SMALL_FILENAME);
	}
	
	public MediaResource getBigPortraitResource(String String) {
		return getPortraitResource(String, PORTRAIT_BIG_FILENAME);
	}
	public MediaResource getBigPortraitResource(Long identityKey) {
		return getPortraitResource(identityKey, PORTRAIT_BIG_FILENAME);
	}
	
	/**
	 * Get the portrait media resource by identity name (username)
	 * @param identity
	 * @return imageResource portrait
	 */
	private MediaResource getPortraitResource(String username, String portraitName) {
		MediaResource imageResource = null;
		File imgFile = getPortraitFile(username, portraitName);
		if (imgFile != null && imgFile.exists()){
			imageResource = new FileMediaResource(imgFile);	
		}
		return imageResource;
	}

	/**
	 * Alternate method to get the portrait resource by identity key. 
	 * @param identityKey
	 * @param portraitName
	 * @return
	 */
	private MediaResource getPortraitResource(Long identityKey, String portraitName) {
		Identity identity = BaseSecurityManager.getInstance().loadIdentityByKey(identityKey);
		if (identity != null) {			
			return getPortraitResource(identity.getName(), portraitName);
		}
		return null;
	}

	
	public File getSmallPortrait(String username) {
		return getPortraitFile(username, PORTRAIT_SMALL_FILENAME);
	}
	
	public File getBigPortrait(String username) {
		return getPortraitFile(username, PORTRAIT_BIG_FILENAME);
	}

	private File getPortraitFile(String username, String prefix) {
		File portraitDir = getPortraitDir(username);
		if(portraitDir != null) {
			for(File file:portraitDir.listFiles()) {
				if(file.getName().startsWith(prefix)) {
					return file;
				}
			}
		}
		return null;
	}
	
	public void setPortrait(File file, String filename, String username) {
		//first remove old ones
		File portraitDir = getPortraitDir(username);
		if(portraitDir != null) {
			for(File currentPortrait:portraitDir.listFiles()) {
				if(currentPortrait.equals(file)) {
					continue;
				}
				currentPortrait.delete();
			}
		}
		
		String extension = FileUtils.getFileSuffix(file.getName());
		if(!StringHelper.containsNonWhitespace(extension)) {
			if(StringHelper.containsNonWhitespace(filename)) {
				extension = FileUtils.getFileSuffix(filename);
			}
			if(!StringHelper.containsNonWhitespace(extension)) {
				extension = "png";
			}
		}
		File pBigFile = new File(portraitDir, DisplayPortraitManager.PORTRAIT_BIG_FILENAME + "." + extension);
		File pSmallFile = new File(portraitDir, DisplayPortraitManager.PORTRAIT_SMALL_FILENAME + "." + extension);
		ImageService imageHelper = CoreSpringFactory.getImpl(ImageService.class);
		Size size = imageHelper.scaleImage(file, extension, pBigFile, DisplayPortraitManager.WIDTH_PORTRAIT_BIG, DisplayPortraitManager.WIDTH_PORTRAIT_BIG, false);
		if(size != null){
			size = imageHelper.scaleImage(file, extension, pSmallFile, DisplayPortraitManager.WIDTH_PORTRAIT_SMALL, DisplayPortraitManager.WIDTH_PORTRAIT_SMALL, false);
		}
	}
	
	public void deletePortrait(Identity identity) {
		FileUtils.deleteDirsAndFiles(getPortraitDir(identity.getName()), true, true);
	}
	
	/**
	 * 
	 * @param identity
	 * @return imageResource portrait
	 */
	public MediaResource getPortrait(File uploadDir, String portraitName){
		MediaResource imageResource = null;
		File imgFile = new File(uploadDir, portraitName);
		if (imgFile.exists()){
			imageResource = new FileMediaResource(imgFile);	
		}
		return imageResource;
	}
	
	/**
	 * 
	 * @param identity
	 * @return
	 */
	public File getPortraitDir(String identityName){
		String portraitPath = FolderConfig.getCanonicalRoot() + 
				FolderConfig.getUserHomePage(identityName) + "/portrait"; 
		File portraitDir = new File(portraitPath);
		portraitDir.mkdirs();
		return portraitDir;
	}
	
	/**
	 * Delete home-page config-file of a certain user.
	 * @see org.olat.user.UserDataDeletable#deleteUserData(org.olat.core.id.Identity)
	 */
	public void deleteUserData(Identity identity, String newDeletedUserName) {
		deletePortrait(identity);
		logDebug("Homepage-config file deleted for identity=" + identity);
	}
}