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
package org.olat.selenium.page.core;

import java.util.List;

import org.junit.Assert;
import org.olat.selenium.page.graphene.OOGraphene;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * 
 * This page drive the booking configuration.
 * 
 * Initial date: 24.02.2015<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class BookingPage {
	
	private static final By tokenIconBy = By.className("o_ac_token_icon");
	private static final By addMethodLinksBy = By.cssSelector("fieldset.o_ac_configuration ul.dropdown-menu a");
	
	private WebDriver browser;
	
	public BookingPage(WebDriver browser) {
		this.browser = browser;
	}
	
	/**
	 * Open the dropdown to add an access control method.
	 * @return This page
	 */
	public BookingPage openAddDropMenu() {
		By addDropMenuBy = By.className("o_sel_accesscontrol_create");
		browser.findElement(addDropMenuBy).click();
		return this;
	}
	
	/**
	 * In the dropdown to add an access control method, choose
	 * the method by secret token.
	 * 
	 * @return This page
	 */
	public BookingPage addTokenMethod() {
		return addMethod(tokenIconBy);
	}
	
	private BookingPage addMethod(By iconBy) {
		List<WebElement> links = browser.findElements(addMethodLinksBy);
		WebElement tokenLink = null;
		for(WebElement link:links) {
			List<WebElement> icons = link.findElements(iconBy);
			if(icons.size() > 0) {
				tokenLink = link;
			}
		}
		Assert.assertNotNull(tokenLink);
		tokenLink.click();
		OOGraphene.waitBusy(browser);
		return this;
	}
	
	public BookingPage configureTokenMethod(String token, String description) {
		By descriptionBy = By.cssSelector(".o_sel_accesscontrol_token_form .o_sel_accesscontrol_description textarea");
		browser.findElement(descriptionBy).sendKeys(description);		
		By tokenBy = By.cssSelector(".o_sel_accesscontrol_token_form .o_sel_accesscontrol_token input[type='text']");
		browser.findElement(tokenBy).sendKeys(token);

		By submitBy = By.cssSelector(".o_sel_accesscontrol_token_form button.btn-primary");
		browser.findElement(submitBy).click();
		OOGraphene.waitBusy(browser);
		return this;
	}
	
	public BookingPage assertOnToken(String token) {
		boolean found = false;
		By infosBy = By.className("o_ac_infos");
		List<WebElement> infos = browser.findElements(infosBy);
		for(WebElement info:infos) {
			if(info.getText().contains(token)) {
				found = true;
			}
		}
		Assert.assertTrue(found);
		return this;
	}
	
	public void bookToken(String token) {
		By tokenEntryBy = By.cssSelector(".o_sel_accesscontrol_token_entry input[type='text']");
		browser.findElement(tokenEntryBy).sendKeys(token);
		
		By submitBy = By.cssSelector(".o_sel_accesscontrol_form button.btn-primary");
		browser.findElement(submitBy).click();
		OOGraphene.waitBusy(browser);
	}
	
	public void save() {
		By saveButtonBy = By.cssSelector("form button.btn-primary");
		browser.findElement(saveButtonBy).click();
		OOGraphene.waitBusy(browser);
	}
}
