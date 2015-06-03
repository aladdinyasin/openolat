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
package org.olat.course;

import org.olat.core.id.IdentityEnvironment;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.resource.OresHelper;
import org.olat.core.util.vfs.NamedContainerImpl;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.filters.VFSItemFilter;

/**
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
class CoursefolderWebDAVNamedContainer extends NamedContainerImpl {
	
	private static final OLog log = Tracing.createLoggerFor(CoursefolderWebDAVNamedContainer.class);
	
	private OLATResourceable res;
	private VFSContainer parentContainer;
	private IdentityEnvironment identityEnv;
	
	public CoursefolderWebDAVNamedContainer(String courseTitle, OLATResourceable res, IdentityEnvironment identityEnv) {
		super(courseTitle, null);
		this.res = OresHelper.clone(res);
		this.identityEnv = identityEnv;
	}

	@Override
	public void setDefaultItemFilter(VFSItemFilter defaultFilter) {
		//
	}

	@Override
	public VFSContainer getDelegate() {
		if(super.getDelegate() == null) {
			try {
				ICourse course = CourseFactory.loadCourse(res.getResourceableId());
				VFSContainer courseFolder = course.getCourseFolderContainer(identityEnv);
				setDelegate(courseFolder);
				if(parentContainer != null) {
					super.setParentContainer(parentContainer);
					parentContainer = null;
				}
			} catch (Exception e) {
				log.error("Error loading course: " + res, e);
			}
		}
		return super.getDelegate();
	}

	@Override
	public VFSItemFilter getDefaultItemFilter() {
		return null;
	}

	@Override
	public void setParentContainer(VFSContainer parentContainer) {
		if(super.getDelegate() == null) {
			this.parentContainer = parentContainer;
		} else {
			super.setParentContainer(parentContainer);
		}
	}
}
