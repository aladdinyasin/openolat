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

import java.util.ArrayList;
import java.util.List;

import org.olat.core.commons.persistence.DB;
import org.olat.core.id.Identity;
import org.olat.modules.assessment.AssessmentEntry;
import org.olat.modules.portfolio.Binder;
import org.olat.modules.portfolio.PortfolioRoles;
import org.olat.modules.portfolio.model.AssessedBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * Initial date: 16.06.2016<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
@Service
public class SharedWithMeQueries {
	
	@Autowired
	private DB dbInstance;
	
	public List<AssessedBinder> searchSharedBinders(Identity member) {
		StringBuilder sc = new StringBuilder();
		sc.append("select owner, binder, aEntry from pfbinder as binder")
		  .append(" inner join fetch binder.baseGroup as baseGroup")
		  .append(" inner join baseGroup.members as membership")
		  .append(" inner join baseGroup.members as ownership")
		  .append(" inner join ownership.identity as owner")
		  .append(" inner join fetch owner.user as owneruser")
		  .append(" left join fetch binder.entry as entry")//entry -> assessment entry -> owner
		  .append(" left join fetch assessmententry as aEntry on (aEntry.identity.key=owner.key and aEntry.repositoryEntry.key=entry.key)")
		  .append(" where (membership.identity.key=:identityKey and membership.role in ('").append(PortfolioRoles.coach.name()).append("','").append(PortfolioRoles.reviewer.name()).append("'))")
		  .append(" or exists (select section.key from pfsection as section")
		  .append("   inner join section.baseGroup as sectionGroup")
		  .append("   inner join sectionGroup.members as sectionMembership on (sectionMembership.identity.key=:identityKey and sectionMembership.role in ('").append(PortfolioRoles.coach.name()).append("','").append(PortfolioRoles.reviewer.name()).append("'))")
		  .append("   where section.binder.key=binder.key")
		  .append(" )")
		  .append(" or exists (select page.key from pfpage as page")
		  .append("   inner join page.baseGroup as pageGroup")
		  .append("   inner join page.section as pageSection")
		  .append("   inner join pageGroup.members as pageMembership on (pageMembership.identity.key=:identityKey and pageMembership.role in ('").append(PortfolioRoles.coach.name()).append("','").append(PortfolioRoles.reviewer.name()).append("'))")
		  .append("   where pageSection.binder.key=binder.key")
		  .append(" )");
		
		List<Object[]> objects = dbInstance.getCurrentEntityManager()
				.createQuery(sc.toString(), Object[].class)
				.setParameter("identityKey", member.getKey())
				.getResultList();
		List<AssessedBinder> assessedBinders = new ArrayList<>(objects.size());
		for(Object[] object:objects) {
			Identity owner = (Identity)object[0];
			Binder binder = (Binder)object[1];
			AssessmentEntry entry = (AssessmentEntry)object[2];
			assessedBinders.add(new AssessedBinder(owner, binder, entry));
		}
		return assessedBinders;
	}
}