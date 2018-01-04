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
package org.olat.modules.qpool.ui.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.components.stack.TooledStackedPanel;
import org.olat.core.gui.components.tree.DnDTreeModel;
import org.olat.core.gui.components.tree.GenericTreeModel;
import org.olat.core.gui.components.tree.GenericTreeNode;
import org.olat.core.gui.components.tree.TreeNode;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.core.util.Util;
import org.olat.core.util.nodes.INode;
import org.olat.group.BusinessGroup;
import org.olat.modules.qpool.Pool;
import org.olat.modules.qpool.QPoolSecurityCallback;
import org.olat.modules.qpool.QPoolService;
import org.olat.modules.qpool.QuestionItemCollection;
import org.olat.modules.qpool.security.QPoolSecurityCallbackFactory;
import org.olat.modules.qpool.ui.QuestionPoolMainEditorController;
import org.olat.modules.taxonomy.TaxonomyLevel;

/**
 * 
 * Initial date: 15.02.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class QuestionPoolMenuTreeModel extends GenericTreeModel implements DnDTreeModel {

	private static final long serialVersionUID = -665560407090871912L;

	private final TooledStackedPanel stackPanel;
	private final Identity identity;
	private final Roles roles;
	private final QPoolService qpoolService;
	private final QPoolTaxonomyTreeBuilder qpoolTaxonomyTreeBuilder;
	private final Translator translator;
	private final QPoolSecurityCallback securityCallback;
	
	private TreeNode myNode;
	private TreeNode myQuestionsNode;
	private GenericTreeNode reviewNode;
	private GenericTreeNode finalNode;
	private TreeNode sharesNode;
	
	public QuestionPoolMenuTreeModel(TooledStackedPanel stackPanel, Identity identity, Roles roles, Locale locale) {
		this(	stackPanel,
				identity,
				roles,
				Util.createPackageTranslator(QuestionPoolMainEditorController.class, locale),
				CoreSpringFactory.getImpl(QPoolService.class),
				CoreSpringFactory.getImpl(QPoolTaxonomyTreeBuilder.class),
				CoreSpringFactory.getImpl(QPoolSecurityCallbackFactory.class)
				);
	}
	
	public QuestionPoolMenuTreeModel(TooledStackedPanel stackPanel, Identity identity, Roles roles, Translator translator,
			QPoolService qpoolService, QPoolTaxonomyTreeBuilder qpoolTaxonomyTreeBuilder, QPoolSecurityCallbackFactory qPoolSecurityCallbackFactory) {
		super();
		this.stackPanel = stackPanel;
		this.identity = identity;
		this.roles = roles;
		this.securityCallback = qPoolSecurityCallbackFactory.createQPoolSecurityCallback(roles);
		this.translator = translator;
		this.qpoolService = qpoolService;
		this.qpoolTaxonomyTreeBuilder = qpoolTaxonomyTreeBuilder;
		buildTreeModel();
	}

	public TreeNode getMyNode() {
		return myNode;
	}

	public TreeNode getMyQuestionsNode() {
		return myQuestionsNode;
	}

	public TreeNode getSharesNode() {
		return sharesNode;
	}
	
	public Collection<String> getDefaultOpenNodeIds() {
		Collection<String> openNodeIds = new ArrayList<>(4);
		if (myNode != null) {
			openNodeIds.add(myNode.getIdent());
		}
		if (reviewNode != null) {
			openNodeIds.add(reviewNode.getIdent());
		}
		if (finalNode != null) {
			openNodeIds.add(finalNode.getIdent());
		}
		if (sharesNode != null) {
			openNodeIds.add(sharesNode.getIdent());
		}
		return openNodeIds;
	}

	@Override
	public boolean isNodeDroppable(TreeNode node) {
		if (node instanceof MyQuestionsTreeNode
				|| node instanceof MarkedQuestionsTreeNode
				|| node instanceof CollectionTreeNode
				|| node instanceof PoolTreeNode
				|| node instanceof BusinessGroupTreeNode) {
			return true;
		}
		return false;
	}

	@Override
	public boolean isNodeDraggable(TreeNode node) {
		return false;
	}
	
	public void reloadReviewCounts() {
		if (reviewNode != null) {
			reloadReviewCount(reviewNode);
		}
	}

	private void reloadReviewCount(INode node) {
		for(int i=node.getChildCount(); i-->0; ) {
			INode child = node.getChildAt(i);
			if (child instanceof ReviewTreeNode) {
				ReviewTreeNode reviewTreeNode = (ReviewTreeNode) child;
				reviewTreeNode.reloadCount();
			}
			reloadReviewCount(child);
		}
	}
	
	private void buildTreeModel() {
		TreeNode rootNode = new GenericTreeNode(translator.translate("topnav.qpool"));
		setRootNode(rootNode);
		
		//question database
		myNode = new PresentationTreeNode(translator.translate("menu.database"));
		rootNode.addChild(myNode);
		buildMySubTreeModel();
		
		//review process
		buildReviewSubTreeModel(rootNode);
		buildFinalSubTreeModel(rootNode);
		
		//pools + shares
		if (securityCallback.canUsePools() || securityCallback.canUseGroups()) {
			sharesNode = new SharesTreeNode(translator.translate("menu.share"));
			rootNode.addChild(sharesNode);	
			buildShareSubTreeModel();
			setFirstChildAsDelegate(sharesNode);
		}
		
		//administration
		if(securityCallback.canAdmin()) {
			TreeNode adminNode = new AdministrationTreeNode(translator.translate("menu.admin"));
			rootNode.addChild(adminNode);
			buildAdminSubTreeModel(adminNode);
			setFirstChildAsDelegate(adminNode);
		}
	}
	
	public void buildMySubTreeModel() {
		myNode.removeAllChildren();
		buildMyTreeNode(myNode);
		buildMarkedTreeNode(myNode);
		buildMyTaxonomyNodes(myNode);
		buildCollectionTreeNodes(myNode);
	}

	private void buildMyTreeNode(TreeNode parentNode) {
		myQuestionsNode = new MyQuestionsTreeNode(stackPanel, securityCallback, translator.translate("menu.database.my"));
		parentNode.addChild(myQuestionsNode);
	}

	private void buildMarkedTreeNode(TreeNode parentNode) {
		TreeNode node = new MarkedQuestionsTreeNode(stackPanel, securityCallback, translator.translate("menu.database.favorit"));
		parentNode.addChild(node);
	}
	
	private void buildMyTaxonomyNodes(TreeNode parentNode) {
		if (!securityCallback.canUseReviewProcess()) return;
		
		qpoolTaxonomyTreeBuilder.loadTaxonomyLevelsMy(identity);
		List<TaxonomyLevel> taxonomyLevels = qpoolTaxonomyTreeBuilder.getTreeTaxonomyLevels();
		for(TaxonomyLevel taxonomyLevel:taxonomyLevels) {
			TreeNode node = new MyTaxonomyLevelTreeNode(stackPanel, securityCallback, taxonomyLevel);
			parentNode.addChild(node);
		}
	}

	private void buildCollectionTreeNodes(TreeNode parentNode) {
		if (!securityCallback.canUseCollections()) return;
		
		List<QuestionItemCollection> collections = qpoolService.getCollections(identity);
		for(QuestionItemCollection coll: collections) {
			TreeNode node = new CollectionTreeNode(stackPanel, securityCallback, coll);
			parentNode.addChild(node);
		}
	}
	
	public void buildReviewSubTreeModel(TreeNode rootNode) {
		if (!securityCallback.canUseReviewProcess()) return;
		
		qpoolTaxonomyTreeBuilder.loadTaxonomyLevelsReview(identity);
		List<TaxonomyLevel> taxonomyLevels = qpoolTaxonomyTreeBuilder.getTreeTaxonomyLevels();
		if(!taxonomyLevels.isEmpty()) {
			reviewNode = new GenericTreeNode(translator.translate("menu.review"));
			reviewNode.setTitle(translator.translate("menu.review"));
			rootNode.addChild(reviewNode);
			
			for(TaxonomyLevel taxonomyLevel:taxonomyLevels) {
				TreeNode node = new ReviewTreeNode(stackPanel, securityCallback, taxonomyLevel, identity, roles);
				reviewNode.addChild(node);
			}
			setFirstChildAsDelegate(reviewNode);
		}
	}
	
	public void buildFinalSubTreeModel(TreeNode rootNode) {
		if (!securityCallback.canUseReviewProcess()) return;

		qpoolTaxonomyTreeBuilder.loadTaxonomyLevelsFinal(identity);
		List<TaxonomyLevel> taxonomyLevels = qpoolTaxonomyTreeBuilder.getTreeTaxonomyLevels();
		if (!taxonomyLevels.isEmpty()) {
			finalNode = new GenericTreeNode(translator.translate("menu.final"));
			finalNode.setTitle(translator.translate("menu.final"));
			rootNode.addChild(finalNode);
			
			for(TaxonomyLevel taxonomyLevel:taxonomyLevels) {
				TreeNode node = new FinalTreeNode(stackPanel, securityCallback, taxonomyLevel);
				finalNode.addChild(node);
			}
			setFirstChildAsDelegate(finalNode);
		}
	}
	
	public void buildShareSubTreeModel() {
		sharesNode.removeAllChildren();
		buildPoolTreeNodes(sharesNode);
		buildBusinessGroupTreeNodes(sharesNode);
	}

	private void buildPoolTreeNodes(TreeNode parentNode) {
		if (!securityCallback.canUsePools()) return;

		List<Pool> pools = qpoolService.getPools(identity, roles);
		for(Pool pool:pools) {
			TreeNode node = new PoolTreeNode(stackPanel, securityCallback, pool);
			parentNode.addChild(node);
		}
	}

	private void buildBusinessGroupTreeNodes(TreeNode parentNode) {
		if (!securityCallback.canUseGroups()) return;

		List<BusinessGroup> groups = qpoolService.getResourcesWithSharedItems(identity);
		for(BusinessGroup group:groups) {
			TreeNode node = new BusinessGroupTreeNode(stackPanel, securityCallback, group);
			parentNode.addChild(node);
		}
	}
	
	private void buildAdminSubTreeModel(TreeNode adminNode) {
		adminNode.removeAllChildren();
		
		TreeNode node = new AllQuestionsTreeNode(stackPanel, securityCallback, translator.translate("menu.all.questions"));
		adminNode.addChild(node);
		
		node = new TaxonomyAdminTreeNode(translator.translate("menu.admin.studyfields"));
		adminNode.addChild(node);
		
		node = new PoolsAdminTreeNode(translator.translate("menu.admin.pools"));
		adminNode.addChild(node);
		
		node = new QItemTypesAdminTreeNode(translator.translate("menu.admin.types"));
		adminNode.addChild(node);
		
		node = new QEducationalContextsAdminTreeNode(translator.translate("menu.admin.levels"));
		adminNode.addChild(node);

		node = new QLicensesAdminTreeNode(translator.translate("menu.admin.licenses"));
		adminNode.addChild(node);
	}
	
	private void setFirstChildAsDelegate(INode node) {
		if (node.getChildCount() > 0) {
			INode childNode = node.getChildAt(0);
			if (node instanceof GenericTreeNode && childNode instanceof TreeNode) {
				GenericTreeNode parent = (GenericTreeNode) node;
				TreeNode child = (TreeNode) childNode;
				parent.setDelegate(child);
			}
		}
	}
		
}
