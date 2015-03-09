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
package org.olat.selenium.page.repository;

import java.util.List;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.junit.Assert;
import org.olat.selenium.page.graphene.OOGraphene;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Drives the catalog administration
 * 
 * Initial date: 08.07.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class CatalogAdminPage {
	
	@Drone
	private WebDriver browser;
	
	public CatalogAdminPage() {
		//
	}
	
	public CatalogAdminPage(WebDriver browser) {
		this.browser = browser;
	}
	
	/**
	 * Add a category to the catalog.
	 * 
	 * @param title
	 * @param description
	 * @return
	 */
	public CatalogAdminPage addCatalogNode(String title, String description) {
		//click in toolbox
		By addNodeBy = By.className("o_sel_catalog_add_category");
		WebElement addNodeLink = browser.findElement(addNodeBy);
		addNodeLink.click();
		OOGraphene.waitingALittleBit();
		
		//fill the form
		By titleBy = By.cssSelector(".o_sel_catalog_add_category_popup input[type='text']");
		OOGraphene.waitElement(titleBy);
		WebElement titleEl = browser.findElement(titleBy);
		titleEl.sendKeys(title);
		
		OOGraphene.tinymce(description, browser);
		
		//save
		By saveBy = By.cssSelector(".o_sel_catalog_add_category_popup .o_sel_catalog_entry_form_buttons button.btn-primary");
		WebElement saveButton = browser.findElement(saveBy);
		saveButton.click();
		OOGraphene.waitBusy();
		return this;
	}
	
	/**
	 * Select a node to navigate
	 * 
	 * @param title
	 * @return
	 */
	public CatalogAdminPage selectNode(String title) {
		By nodeBy = By.cssSelector("div.o_meta > h4.o_title > a");
		List<WebElement> nodes = browser.findElements(nodeBy);
		WebElement selectedNode = null;
		for(WebElement node:nodes) {
			if(node.getText().contains(title)) {
				selectedNode = node;
			}
		}
		Assert.assertNotNull(selectedNode);
		selectedNode.click();
		OOGraphene.waitBusy();
		return this;
	}
}