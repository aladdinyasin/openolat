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
package org.olat.selenium.page.course;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.olat.selenium.page.graphene.OOGraphene;
import org.olat.selenium.page.group.MembersWizardPage;
import org.olat.user.restapi.UserVO;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * 
 * Drive the members management page in course
 * 
 * 
 * Initial date: 12.02.2015<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class MembersPage {

	@Drone
	private WebDriver browser;
	
	public MembersPage() {
		//
	}
	
	public MembersPage(WebDriver browser) {
		this.browser = browser;
	}
	
	public MembersWizardPage addMember() {
		By addMemberBy = By.className("o_sel_course_add_member");
		WebElement addMemberButton = browser.findElement(addMemberBy);
		addMemberButton.click();
		OOGraphene.waitBusy(browser);
		return new MembersWizardPage(browser);
	}
	
	public MembersPage selectBusinessGroups() {
		By groupsItemBy = By.cssSelector("li.o_sel_membersmgt_groups a");
		browser.findElement(groupsItemBy).click();
		OOGraphene.waitBusy(browser);
		return this;
	}
	
	public MembersPage createBusinessGroup(String name, String description,
			int maxParticipants, boolean waitingList, boolean auto) {
		By createBy = By.className("o_sel_course_new_group");
		browser.findElement(createBy).click();
		OOGraphene.waitBusy(browser);
		
		//fill the form
		By nameBy = By.cssSelector(".o_sel_group_edit_title input[type='text']");
		browser.findElement(nameBy).sendKeys(name);
		OOGraphene.tinymce(description, browser);
		
		By maxParticipantBy = By.cssSelector(".o_sel_group_edit_max_members input[type='text']");
		browser.findElement(maxParticipantBy).sendKeys(Integer.toString(maxParticipants));
		
		if(waitingList) {
			By waitingListBy = By.cssSelector(".o_sel_group_edit_waiting_list input[type='checkbox']");
			browser.findElement(waitingListBy).click();
			OOGraphene.waitBusy(browser);
		}
		if(auto) {
			By autoBy = By.cssSelector(".o_sel_group_edit_auto_close_ranks input[type='checkbox']");
			browser.findElement(autoBy).click();
			OOGraphene.waitBusy(browser);
		}
		
		//save
		By submitBy = By.cssSelector(".o_sel_group_edit_group_form button.btn-primary");
		WebElement submitButton = browser.findElement(submitBy);
		submitButton.click();
		OOGraphene.waitBusy(browser);
		
		
		return this;
	}
	
	/**
	 * Add a user by username as participant
	 * 
	 * @param user
	 */
	public void quickAdd(UserVO user) {
		addMember()
			.searchMember(user, true)
			.next().next().next().finish();
	}
	
	/**
	 * Click back to the course
	 * 
	 * @return
	 */
	public CoursePageFragment clickToolbarBack() {
		By toolbarBackBy = By.cssSelector("li.o_breadcrumb_back>a");
		browser.findElement(toolbarBackBy).click();
		OOGraphene.waitBusy(browser);
		return new CoursePageFragment(browser);
	}

}
