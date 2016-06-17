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
package org.olat.modules.portfolio.manager;

import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.olat.core.commons.persistence.DB;
import org.olat.core.id.Identity;
import org.olat.modules.portfolio.Binder;
import org.olat.modules.portfolio.Page;
import org.olat.modules.portfolio.PortfolioRoles;
import org.olat.modules.portfolio.PortfolioService;
import org.olat.modules.portfolio.Section;
import org.olat.modules.portfolio.model.AccessRights;
import org.olat.modules.portfolio.model.SectionImpl;
import org.olat.test.JunitTestHelper;
import org.olat.test.OlatTestCase;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * 
 * Initial date: 07.06.2016<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class PortfolioServiceTest extends OlatTestCase {
	
	@Autowired
	private DB dbInstance;
	@Autowired
	private PortfolioService portfolioService;
	
	@Test
	public void createNewOwnedPorfolio() {
		Identity id = JunitTestHelper.createAndPersistIdentityAsRndUser("port-u-1");
		String title = "My portfolio";
		String summary = "My live";
		
		Binder binder = portfolioService.createNewBinder(title, summary, null, id);
		dbInstance.commitAndCloseSession();
		Assert.assertNotNull(binder);
		Assert.assertNotNull(binder.getKey());
		Assert.assertNotNull(binder.getCreationDate());
		Assert.assertNotNull(binder.getLastModified());
		Assert.assertEquals(title, binder.getTitle());
		Assert.assertEquals(summary, binder.getSummary());
		
		List<Binder> ownedBinders = portfolioService.searchOwnedBinders(id);
		Assert.assertNotNull(ownedBinders);
		Assert.assertEquals(1, ownedBinders.size());
		Binder ownedBinder = ownedBinders.get(0);
		Assert.assertNotNull(ownedBinder);
		Assert.assertEquals(binder, ownedBinder);
	}
	
	@Test
	public void binderAccessRights() {
		Identity owner = JunitTestHelper.createAndPersistIdentityAsRndUser("port-u-2");
		String title = "My private binder";
		String summary = "My live";
		Binder binder = portfolioService.createNewBinder(title, summary, null, owner);
		dbInstance.commitAndCloseSession();
		
		// load right
		List<AccessRights> rights = portfolioService.getAccessRights(binder);
		Assert.assertNotNull(rights);
		Assert.assertEquals(1, rights.size());
		AccessRights ownerRight = rights.get(0);
		Assert.assertEquals(binder.getKey(), ownerRight.getBinderKey());
		Assert.assertEquals(owner, ownerRight.getIdentity());
		Assert.assertEquals(PortfolioRoles.owner, ownerRight.getRole());
	}

	@Test
	public void binderAndSectionAndPageAccessRights() {
		Identity owner = JunitTestHelper.createAndPersistIdentityAsRndUser("port-u-3");
		Identity coach = JunitTestHelper.createAndPersistIdentityAsRndUser("port-u-4");
		Identity reviewer = JunitTestHelper.createAndPersistIdentityAsRndUser("port-u-5");
		String title = "My published binder";
		String summary = "My live";
		Binder binder = portfolioService.createNewBinder(title, summary, null, owner);
		dbInstance.commit();
		portfolioService.appendNewSection("Section", "Coached section", null, null, binder);
		dbInstance.commit();
		List<Section> sections = portfolioService.getSections(binder);
		Section section = sections.get(0);
		portfolioService.appendNewPage(owner, "Reviewed page", "", null, section);
		portfolioService.addAccessRights(section, coach, PortfolioRoles.coach);
		
		dbInstance.commit();
		List<Page> pages = portfolioService.getPages(section);
		Page page = pages.get(0);
		portfolioService.addAccessRights(page, reviewer, PortfolioRoles.reviewer);

		// load right
		List<AccessRights> rights = portfolioService.getAccessRights(binder);
		Assert.assertNotNull(rights);
		Assert.assertEquals(3, rights.size());
		
		boolean foundOwner = false;
		boolean foundCoach = false;
		boolean foundReviewer = false;
		
		for(AccessRights right:rights) {
			if(PortfolioRoles.owner.equals(right.getRole()) && owner.equals(right.getIdentity())) {
				foundOwner = true;
			} else if(PortfolioRoles.coach.equals(right.getRole()) && coach.equals(right.getIdentity())) {
				foundCoach = true;
			} else if(PortfolioRoles.reviewer.equals(right.getRole()) && reviewer.equals(right.getIdentity())) {
				foundReviewer = true;
			}
		}
		
		Assert.assertTrue(foundOwner);
		Assert.assertTrue(foundCoach);
		Assert.assertTrue(foundReviewer);
	}
	
	@Test
	public void binderAndSectionAndPageAccessRights_byIdentity() {
		Identity owner = JunitTestHelper.createAndPersistIdentityAsRndUser("port-u-5");
		Identity identity = JunitTestHelper.createAndPersistIdentityAsRndUser("port-u-6");
		String title = "My published binder";
		String summary = "My live";
		Binder binder = portfolioService.createNewBinder(title, summary, null, owner);
		dbInstance.commit();
		portfolioService.appendNewSection("Section", "Coached section", null, null, binder);
		dbInstance.commit();
		List<Section> sections = portfolioService.getSections(binder);
		Section section = sections.get(0);
		portfolioService.appendNewPage(owner, "Reviewed page", "", null, section);
		portfolioService.addAccessRights(section, identity, PortfolioRoles.coach);
		
		dbInstance.commit();
		List<Page> pages = portfolioService.getPages(section);
		Page page = pages.get(0);
		portfolioService.addAccessRights(page, identity, PortfolioRoles.reviewer);

		// load right
		List<AccessRights> rights = portfolioService.getAccessRights(binder, identity);
		Assert.assertNotNull(rights);
		Assert.assertEquals(2, rights.size());
	}
	
	@Test
	public void assignTemplate() {
		Identity owner = JunitTestHelper.createAndPersistIdentityAsRndUser("port-u-7");
		Identity id = JunitTestHelper.createAndPersistIdentityAsRndUser("port-u-8");
		
		//create a template
		String title = "Template binder";
		String summary = "Binder used as a template";
		Binder template = portfolioService.createNewBinder(title, summary, null, owner);
		dbInstance.commit();
		for(int i=0; i<4; i++) {
			portfolioService.appendNewSection("Section " + i, "Section " + i, null, null, template);
			dbInstance.commit();
		}
		dbInstance.commitAndCloseSession();
		List<Section> templateSections = portfolioService.getSections(template);
		Assert.assertNotNull(templateSections);
		Assert.assertEquals(4, templateSections.size());
		
		
		//user copy the template
		Binder binder = portfolioService.assignBinder(id, template, null, null, new Date());
		dbInstance.commitAndCloseSession();
		Assert.assertNotNull(binder);
		Assert.assertNotNull(binder.getKey());
		Assert.assertNotNull(binder.getCopyDate());
		Assert.assertNotNull(template.getTitle(), binder.getTitle());
		
		List<Section> reloadedSections = portfolioService.getSections(binder);
		Assert.assertNotNull(reloadedSections);
		Assert.assertEquals(4, reloadedSections.size());
		Assert.assertEquals(templateSections.get(0).getTitle(), reloadedSections.get(0).getTitle());
		Assert.assertEquals("Section 1", reloadedSections.get(1).getTitle());
		Assert.assertEquals(templateSections.get(2).getTitle(), reloadedSections.get(2).getTitle());
		Assert.assertEquals("Section 3", reloadedSections.get(3).getTitle());
		
		Assert.assertEquals(templateSections.get(0), ((SectionImpl)reloadedSections.get(0)).getTemplateReference());
		Assert.assertEquals(templateSections.get(1), ((SectionImpl)reloadedSections.get(1)).getTemplateReference());
		Assert.assertEquals(templateSections.get(2), ((SectionImpl)reloadedSections.get(2)).getTemplateReference());
		Assert.assertEquals(templateSections.get(3), ((SectionImpl)reloadedSections.get(3)).getTemplateReference());
	}
}