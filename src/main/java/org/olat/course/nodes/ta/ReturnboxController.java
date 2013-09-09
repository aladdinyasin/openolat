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

package org.olat.course.nodes.ta;

import java.io.File;

import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.modules.bc.FolderRunController;
import org.olat.core.commons.modules.bc.vfs.OlatNamedContainerImpl;
import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.id.Identity;
import org.olat.core.util.Util;
import org.olat.core.util.notifications.ContextualSubscriptionController;
import org.olat.core.util.notifications.SubscriptionContext;
import org.olat.core.util.vfs.callbacks.ReadOnlyCallback;
import org.olat.course.nodes.CourseNode;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.modules.ModuleConfiguration;
import org.olat.user.UserManager;

/**
 * Initial Date:  02.09.2004
 *
 * @author Mike Stock
 * 
 */

public class ReturnboxController extends BasicController {

	private static final String PACKAGE = Util.getPackageName(ReturnboxController.class);
	private static final String VELOCITY_ROOT = Util.getPackageVelocityRoot(PACKAGE);
	public static final String RETURNBOX_DIR_NAME = "returnboxes";
	
	// config
	
	private VelocityContainer myContent;
	private FolderRunController returnboxFolderRunController;
	private SubscriptionContext subsContext;
	private ContextualSubscriptionController contextualSubscriptionCtr;
	
	private final UserManager userManager;
	
	/**
	 * Implements a dropbox.
	 * @param ureq
	 * @param wControl
	 * @param config
	 * @param node
	 * @param userCourseEnv
	 * @param previewMode
	 */
	public ReturnboxController(UserRequest ureq, WindowControl wControl, ModuleConfiguration config, CourseNode node, UserCourseEnvironment userCourseEnv, boolean previewMode) {
		this(ureq, wControl, config, node, userCourseEnv, previewMode, true);
	}

	protected ReturnboxController(UserRequest ureq, WindowControl wControl, ModuleConfiguration config, CourseNode node, UserCourseEnvironment userCourseEnv, boolean previewMode, boolean doInit) {
		super(ureq, wControl);
		userManager = CoreSpringFactory.getImpl(UserManager.class);
		
		this.setBasePackage(ReturnboxController.class);
		if (doInit) {
			initReturnbox(ureq, wControl, config, node, userCourseEnv, previewMode);
		}
	}

	protected void initReturnbox(UserRequest ureq, WindowControl wControl, ModuleConfiguration config, CourseNode node, UserCourseEnvironment userCourseEnv, boolean previewMode) {
		// returnbox display
		myContent = createVelocityContainer("returnbox");
		OlatRootFolderImpl rootFolder = new OlatRootFolderImpl(getReturnboxPathFor(userCourseEnv.getCourseEnvironment(), node, ureq.getIdentity()) , null);
		String fullName = userManager.getUserDisplayName(getIdentity());
		OlatNamedContainerImpl namedContainer = new OlatNamedContainerImpl(fullName, rootFolder);
		namedContainer.setLocalSecurityCallback(new ReadOnlyCallback());
		returnboxFolderRunController = new FolderRunController(namedContainer, false, ureq, wControl);
		returnboxFolderRunController.addControllerListener(this);
		myContent.put("returnbox", returnboxFolderRunController.getInitialComponent());
		// notification
		if ( !previewMode && !ureq.getUserSession().getRoles().isGuestOnly()) {
			// offer subscription, but not to guests
			subsContext = ReturnboxFileUploadNotificationHandler.getSubscriptionContext(userCourseEnv, node, ureq.getIdentity());
			if (subsContext != null) {
				contextualSubscriptionCtr = AbstractTaskNotificationHandler.createContextualSubscriptionController(ureq, wControl, getReturnboxPathFor(
						userCourseEnv.getCourseEnvironment(), node,ureq.getIdentity()), subsContext, ReturnboxController.class);
				myContent.put("subscription", contextualSubscriptionCtr.getInitialComponent());
				myContent.contextPut("hasNotification", Boolean.TRUE);
			}
		} else {
			myContent.contextPut("hasNotification", Boolean.FALSE);
		}
		putInitialPanel(myContent);
	}

	public String getReturnboxPathFor(CourseEnvironment courseEnv, CourseNode cNode, Identity identity) {
  	return getReturnboxPathRelToFolderRoot(courseEnv, cNode) + File.separator + identity.getName();
	}
	
	/**
	 * Returnbox path relative to folder root.
	 * @param courseEnv
	 * @param cNode
	 * @return Returnbox path relative to folder root.
	 */
	public static String getReturnboxPathRelToFolderRoot(CourseEnvironment courseEnv, CourseNode cNode) {
		return courseEnv.getCourseBaseContainer().getRelPath() + File.separator + RETURNBOX_DIR_NAME + File.separator + cNode.getIdent();
	}
	
	
	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	public void event(UserRequest ureq, Component source, Event event) {
		if (source == myContent) {
      if (event.getCommand().equals("cc")) {
				getWindowControl().pop();
				myContent.setPage(VELOCITY_ROOT + "/dropbox.html");
			}
		}

	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	public void event(UserRequest ureq, Controller source, Event event) {
	}
	
	
	
	/**
	 * 
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	protected void doDispose() {
	}
}
