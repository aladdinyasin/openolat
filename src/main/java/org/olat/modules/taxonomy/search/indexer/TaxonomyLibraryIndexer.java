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
package org.olat.modules.taxonomy.search.indexer;

import java.io.IOException;
import java.util.List;

import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.modules.taxonomy.Taxonomy;
import org.olat.modules.taxonomy.TaxonomyCompetenceTypes;
import org.olat.modules.taxonomy.TaxonomyLevel;
import org.olat.modules.taxonomy.TaxonomyLevelType;
import org.olat.modules.taxonomy.TaxonomyRef;
import org.olat.modules.taxonomy.TaxonomyService;
import org.olat.modules.taxonomy.model.TaxonomyRefImpl;
import org.olat.search.service.SearchResourceContext;
import org.olat.search.service.indexer.AbstractHierarchicalIndexer;
import org.olat.search.service.indexer.FolderIndexerAccess;
import org.olat.search.service.indexer.FolderIndexerWorker;
import org.olat.search.service.indexer.OlatFullIndexer;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * Initial date: 20 oct. 2017<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
@Service("taxonomyLibraryIndexer")
public class TaxonomyLibraryIndexer extends AbstractHierarchicalIndexer implements InitializingBean {
	
	@Autowired
	private TaxonomyService taxonomyService;
	@Autowired
	private TaxonomyLevelLibraryIndexer taxonomyLevelLibraryIndexer;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		addIndexer(taxonomyLevelLibraryIndexer);
	}

	@Override
	public String getSupportedTypeName() {
		return "Taxonomy";
	}

	@Override
	public void doIndex(SearchResourceContext parentResourceContext, Object object, OlatFullIndexer indexerWriter)
			throws IOException, InterruptedException {
		List<Taxonomy> taxonomyList = taxonomyService.getRootTaxonomyList();
		for(Taxonomy taxonomy:taxonomyList) {
			if(taxonomy.isDocumentsLibraryEnabled()) {
				SearchResourceContext searchResourceContext = new SearchResourceContext(parentResourceContext);
				searchResourceContext.setBusinessControlFor(taxonomy);
				searchResourceContext.setTitle(taxonomy.getDisplayName());
				searchResourceContext.setDescription(taxonomy.getDescription());
				searchResourceContext.setLastModified(taxonomy.getLastModified());
				searchResourceContext.setCreatedDate(taxonomy.getCreationDate());
				doIndexTaxonomyLibrary(searchResourceContext, taxonomy, indexerWriter);
			}
		}
	}
	
	private void doIndexTaxonomyLibrary(SearchResourceContext searchResourceContext, Taxonomy taxonomy, OlatFullIndexer indexerWriter)
	throws InterruptedException  {
		VFSContainer templatesContainer = taxonomyService.getDocumentsLibrary(taxonomy);
		if(templatesContainer != null) {
			FolderIndexerWorker runnableFolderIndexer = new  FolderIndexerWorker();
			runnableFolderIndexer.setAccessRule(FolderIndexerAccess.FULL_ACCESS);
			runnableFolderIndexer.setParentResourceContext(searchResourceContext);
			runnableFolderIndexer.setContainer(templatesContainer);
			runnableFolderIndexer.setIndexWriter(indexerWriter);
			runnableFolderIndexer.setFilePath("");
			indexerWriter.submit(runnableFolderIndexer);
		}
		
		List<TaxonomyLevel> levels = taxonomyService.getTaxonomyLevels(taxonomy);
		for(TaxonomyLevel level:levels) {
			TaxonomyLevelType type = level.getType();
			if(type != null && hasDocumentLibraryEnabled(type)) {
				try {
					taxonomyLevelLibraryIndexer.doIndex(searchResourceContext, level, indexerWriter);
				} catch(InterruptedException e) {
					throw e;
				} catch (Exception e) {
					logError("", e);
				}
			}
		}
	}
	
	private boolean hasDocumentLibraryEnabled(TaxonomyLevelType type) {
		return type.isDocumentsLibraryManageCompetenceEnabled()
				|| type.isDocumentsLibraryTeachCompetenceReadEnabled()
				|| type.isDocumentsLibraryTeachCompetenceWriteEnabled()
				|| type.isDocumentsLibraryHaveCompetenceReadEnabled()
				|| type.isDocumentsLibraryTargetCompetenceReadEnabled();
	}

	@Override
	public boolean checkAccess(ContextEntry contextEntry, BusinessControl businessControl, Identity identity, Roles roles) {
		if(roles.isOLATAdmin()) return true;
		
		List<ContextEntry> entries = businessControl.getEntriesDownTheControls();
		if(entries.size() == 1) {
			TaxonomyRef taxonomy = new TaxonomyRefImpl(contextEntry.getOLATResourceable().getResourceableId());
			return taxonomyService.hasCompetence(taxonomy, identity, TaxonomyCompetenceTypes.manage, TaxonomyCompetenceTypes.teach);
		}
		return super.checkAccess(businessControl, identity, roles);
	}
}