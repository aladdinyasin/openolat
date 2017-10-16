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
package org.olat.selenium.page.lecture;

import java.util.List;

import org.olat.selenium.page.graphene.OOGraphene;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * 
 * Initial date: 15 août 2017<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class LectureRepositoryAdminPage {
	
	private WebDriver browser;
	
	public LectureRepositoryAdminPage(WebDriver browser) {
		this.browser = browser;
	}
	
	public LectureRepositoryAdminPage assertOnAdminPage() {
		By adminBy = By.cssSelector("div.o_sel_repo_lectures_admin");
		OOGraphene.waitElement(adminBy, browser);
		return this;
	}
	
	public LectureRepositorySettingsPage settings() {
		By enableBy = By.xpath("//label/input[@name='lecture.admin.enabled' and @value='on']");
		List<WebElement> enableEls = browser.findElements(enableBy);
		if(enableEls.isEmpty()) {
			By settingBy = By.xpath("//div[contains(@class,'o_sel_repo_lectures_admin')]//a[contains(@onclick,'repo.settings')]");
			browser.findElement(settingBy).click();
			OOGraphene.waitBusy(browser);
			OOGraphene.waitElement(enableBy, browser);
		}
		return new LectureRepositorySettingsPage(browser);
	}
	
	public LectureListRepositoryPage lectureList() {
		By lecturesBy = By.xpath("//div[contains(@class,'o_sel_repo_lectures_admin')]//a[contains(@onclick,'repo.lectures.block')]");
		browser.findElement(lecturesBy).click();
		OOGraphene.waitBusy(browser);
		return new LectureListRepositoryPage(browser)
				.asssertOnLectureList();
	}


}
