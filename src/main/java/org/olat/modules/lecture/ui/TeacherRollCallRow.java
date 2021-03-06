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
package org.olat.modules.lecture.ui;

import java.util.List;
import java.util.Locale;

import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.id.Identity;
import org.olat.modules.lecture.LectureBlockRollCall;
import org.olat.modules.lecture.ui.component.LectureBlockRollCallStatusItem;
import org.olat.user.UserPropertiesRow;
import org.olat.user.propertyhandlers.UserPropertyHandler;

/**
 * 
 * Initial date: 27 mars 2017<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class TeacherRollCallRow extends UserPropertiesRow {
	
	private Identity identity;
	private FormLink reasonLink;
	private TextElement commentEl;
	private LectureBlockRollCall rollCall;
	private MultipleSelectionElement[] checks;
	private MultipleSelectionElement authorizedAbsence;
	private LectureBlockRollCallStatusItem rollCallStatusEl;
	private FormLayoutContainer authorizedAbsenceCont;
	
	public TeacherRollCallRow(LectureBlockRollCall rollCall, Identity identity, List<UserPropertyHandler> propertyHandlers, Locale locale) {
		super(identity, propertyHandlers, locale);
		this.identity = identity;
		this.rollCall = rollCall;
	}
	
	public Identity getIdentity() {
		return identity;
	}
	
	public LectureBlockRollCall getRollCall() {
		return rollCall;
	}

	public void setRollCall(LectureBlockRollCall rollCall) {
		this.rollCall = rollCall;
	}

	public MultipleSelectionElement[] getChecks() {
		return checks;
	}
	
	public void setChecks(MultipleSelectionElement[] checks) {
		this.checks = checks;
	}
	
	public MultipleSelectionElement getCheck(int pos) {
		if(checks != null && pos >= 0 && pos < checks.length) {
			return checks[pos];
		}
		return null;
	}
	
	public int getIndexOfCheck(MultipleSelectionElement check) {
		if(checks != null && check != null) {
			for(int i=checks.length; i-->0; ) {
				if(checks[i] == check) {
					return i;
				}
			}
		}
		return -1;
	}
	
	public MultipleSelectionElement getAuthorizedAbsence() {
		return authorizedAbsence;
	}

	public void setAuthorizedAbsence(MultipleSelectionElement authorizedAbsence) {
		this.authorizedAbsence = authorizedAbsence;
	}

	public FormLink getReasonLink() {
		return reasonLink;
	}

	public void setReasonLink(FormLink reasonLink) {
		this.reasonLink = reasonLink;
	}

	public FormLayoutContainer getAuthorizedAbsenceCont() {
		return authorizedAbsenceCont;
	}

	public void setAuthorizedAbsenceCont(FormLayoutContainer authorizedAbsenceCont) {
		this.authorizedAbsenceCont = authorizedAbsenceCont;
	}

	public TextElement getCommentEl() {
		return commentEl;
	}
	
	public void setCommentEl(TextElement commentEl) {
		this.commentEl = commentEl;
	}

	public LectureBlockRollCallStatusItem getRollCallStatusEl() {
		return rollCallStatusEl;
	}

	public void setRollCallStatusEl(LectureBlockRollCallStatusItem rollCallStatusEl) {
		this.rollCallStatusEl = rollCallStatusEl;
	}
}
