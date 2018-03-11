package de.htwk.autolat.BBautOLAT;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.stack.BreadcrumbPanel;
import org.olat.core.gui.components.stack.TooledStackedPanel;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.tabbable.TabbableController;
import org.olat.core.id.Identity;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.util.ExportUtil;
import org.olat.core.util.Util;
import org.olat.core.util.ValidationStatus;
import org.olat.core.util.WebappHelper;
import org.olat.course.ICourse;
import org.olat.course.assessment.ui.tool.AssessmentCourseNodeController;
import org.olat.course.assessment.ui.tool.IdentityListCourseNodeController;
import org.olat.course.editor.CourseEditorEnv;
import org.olat.course.editor.NodeEditController;
import org.olat.course.editor.StatusDescription;
import org.olat.course.export.CourseEnvironmentMapper;
import org.olat.course.nodes.AbstractAccessableCourseNode;
import org.olat.course.nodes.AssessableCourseNode;
import org.olat.course.nodes.CourseNode;
import org.olat.course.nodes.StatusDescriptionHelper;
import org.olat.course.nodes.CourseNode.Processing;
import org.olat.course.run.navigation.NodeRunConstructionResult;
import org.olat.course.run.scoring.AssessmentEvaluation;
import org.olat.course.run.scoring.ScoreEvaluation;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.group.BusinessGroup;
import org.olat.modules.ModuleConfiguration;
import org.olat.modules.assessment.Role;
import org.olat.modules.assessment.model.AssessmentRunStatus;
import org.olat.modules.assessment.ui.AssessmentToolContainer;
import org.olat.modules.assessment.ui.AssessmentToolSecurityCallback;
import org.olat.repository.RepositoryEntry;

import de.htwk.autolat.Configuration.Configuration;
import de.htwk.autolat.Configuration.ConfigurationManager;
import de.htwk.autolat.Configuration.ConfigurationManagerImpl;
import de.htwk.autolat.Student.Student;
import de.htwk.autolat.Student.StudentManagerImpl;
import de.htwk.autolat.TaskConfiguration.TaskConfigurationManagerImpl;
import de.htwk.autolat.TaskInstance.TaskInstance;
import de.htwk.autolat.TaskResult.TaskResult;
import de.htwk.autolat.TaskType.TaskTypeManagerImpl;
import de.htwk.autolat.tools.ImportExport.AutOlatNodeExporter;
import de.htwk.autolat.tools.ImportExport.AutOlatNodeImporter;

/**
 * Description: <br>
 * 
 * @author Felix Jost
 */
public class BBautOLATCourseNode extends AbstractAccessableCourseNode implements AssessableCourseNode {

	public static final String TYPE = "autOLAT";

	/**
	 * Default constructor for course node of type single page
	 */
	public BBautOLATCourseNode() {
		super(TYPE);		
		updateModuleConfigDefaults(true);		
	}

	@Override
	public void updateModuleConfigDefaults(boolean isNewNode) {
		ModuleConfiguration config = getModuleConfiguration();
		if (isNewNode) {
			// use defaults for new course building blocks
			config.setBooleanEntry(NodeEditController.CONFIG_STARTPAGE, false);
			config.setConfigurationVersion(1);
			// create the needed values in the config for controlling the edit process
			config.setBooleanEntry("ServerConnectionSet", false);
			config.setBooleanEntry("TaskTypeValid", false);
			config.setBooleanEntry("GradingTimeSet", false); 
		}
	}
	
	@Override
	public TabbableController createEditController(UserRequest ureq,
			WindowControl wControl, BreadcrumbPanel stackPanel,
			ICourse course, UserCourseEnvironment euce) {
		// TODO Auto-generated method stub
		BBautOLATEditController childTabCntrllr = new BBautOLATEditController(getModuleConfiguration(), ureq, wControl, this, course, euce);
		CourseNode chosenNode = course.getEditorTreeModel().getCourseNode(euce.getCourseEditorEnv().getCurrentCourseNodeId());
		return new NodeEditController(ureq, wControl, course.getEditorTreeModel(), course, chosenNode, euce, childTabCntrllr);
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#createEditController(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl, org.olat.course.ICourse)
	 */
	public TabbableController createEditController(UserRequest ureq, WindowControl wControl, ICourse course, UserCourseEnvironment euce) {
		
		BBautOLATEditController childTabCntrllr = new BBautOLATEditController(getModuleConfiguration(), ureq, wControl, this, course, euce);
		CourseNode chosenNode = course.getEditorTreeModel().getCourseNode(euce.getCourseEditorEnv().getCurrentCourseNodeId());
		return new NodeEditController(ureq, wControl, course.getEditorTreeModel(), course, chosenNode, euce, childTabCntrllr);

	}

	/**
	 * @see org.olat.course.nodes.CourseNode#createNodeRunConstructionResult(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.run.userview.UserCourseEnvironment,
	 *      org.olat.course.run.userview.NodeEvaluation)
	 */
	@Override
	public NodeRunConstructionResult createNodeRunConstructionResult(UserRequest ureq, WindowControl wControl, UserCourseEnvironment userCourseEnv, NodeEvaluation ne, String nodecmd) {		
		return new NodeRunConstructionResult(new BBautOLATRunController(wControl, ureq, this, userCourseEnv, ne, false));
	}

	@Override
	public Controller createPreviewController(UserRequest ureq, WindowControl wControl, UserCourseEnvironment userCourseEnv, NodeEvaluation ne) {
		return new BBautOLATRunController(wControl, ureq, this, userCourseEnv, ne, true);
	}
	
	/**
	 * @see org.olat.course.nodes.CourseNode#isConfigValid()
	 */
	@Override
	public StatusDescription isConfigValid() {
		
		StatusDescription sd = StatusDescription.NOERROR;
		String key_short = "", key_long = "";
		boolean connectionError = false;
		
		ModuleConfiguration config = this.getModuleConfiguration();
		
		//no grading time is set
		if(!config.getBooleanSafe("GradingTimeSet")) {
			key_short = "error.coursenode.missinggradingtime_short";
			key_long = "error.coursenode.missinggradingtime_long";
		}
		
		//no tasktype is set in the configuration
		if(!config.getBooleanSafe("TaskTypeValid")) {
			key_short = "error.coursenode.missingtasktype_short";
			key_long = "error.coursenode.missingtasktype_long";
		}
		
		//no server connection could be set
		if(!config.getBooleanSafe("ServerConnectionSet")) {
			key_short = "error.coursenode.missingserverconnection_short";
			key_long = "error.coursenode.missingserverconnection_long";
			connectionError = true;
		}
		
		if(key_short!="") {
			String translatorStr = Util.getPackageName(BBautOLATCourseNode.class);
			sd = new StatusDescription(ValidationStatus.ERROR, key_short, key_long, new String[]{this.getShortTitle()}, translatorStr);
			sd.setDescriptionForUnit(getIdent());
			if(connectionError) 
				sd.setActivateableViewIdentifier(BBautOLATEditController.PANE_KEY_SERVERCONFIG);
			else
				sd.setActivateableViewIdentifier(BBautOLATEditController.PANE_KEY_TASKCONFIG);
		}
		return sd;
	}


	/**
	 * @see org.olat.course.nodes.CourseNode#isConfigValid(org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public StatusDescription[] isConfigValid(CourseEditorEnv cev) {
		oneClickStatusCache = null;
		//only here we know which translator to take for translating condition error messages
		String translatorStr = Util.getPackageName(BBautOLATCourseNode.class);
		List sds = isConfigValidWithTranslator(cev, translatorStr,getConditionExpressions());
		oneClickStatusCache = StatusDescriptionHelper.sort(sds);
		return oneClickStatusCache;
	}
	
	
	/**
	 * @see org.olat.course.nodes.CourseNode#getReferencedRepositoryEntry()
	 */
	@Override
	public RepositoryEntry getReferencedRepositoryEntry() {
		//TODO
		return null;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#needsReferenceToARepositoryEntry()
	 */
	@Override
	public boolean needsReferenceToARepositoryEntry() {
		//TODO
		return false;
	}
	
	//the following methods belong to the assessable course node

	@Override
	public Float getCutValueConfiguration() {
		return null;
	}

	@Override
	public String getDetailsListView(UserCourseEnvironment userCourseEnvironment) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDetailsListViewHeaderKey() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Float getMaxScoreConfiguration() {
		return null;
	}

	@Override
	public Float getMinScoreConfiguration() {
		return null;
	}

	@Override
	public Integer getUserAttempts(UserCourseEnvironment userCourseEnvironment) {
		
		//not needed, because the attempts are not just a counter but a limiter
		try {
			Long courseID = userCourseEnvironment.getCourseEnvironment().getCourseResourceableId();
			Long courseNodeID = Long.valueOf(getIdent());
			Student student = StudentManagerImpl.getInstance().getStudentByIdentity(userCourseEnvironment.getIdentityEnvironment().getIdentity());
			TaskInstance taskInstance = student.getTaskInstanceByConfiguration(ConfigurationManagerImpl.getInstance().getConfigurationByCourseID(courseID, courseNodeID));	
			return (int) taskInstance.getLivingInstanceCounter();
		}
		catch (Exception e) {
			return 0;
		}
	}

	@Override
	public String getUserCoachComment(UserCourseEnvironment userCourseEnvironment) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getUserLog(UserCourseEnvironment userCourseEnvironment) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AssessmentEvaluation getUserScoreEvaluation(UserCourseEnvironment userCourseEnv) {
		AssessmentEvaluation scoreEvaluation = new AssessmentEvaluation(0f, false);
		
		try {
			Long courseID = userCourseEnv.getCourseEnvironment().getCourseResourceableId();
			Long courseNodeID = Long.valueOf(getIdent());
			Student student = StudentManagerImpl.getInstance().getStudentByIdentity(userCourseEnv.getIdentityEnvironment().getIdentity());
			TaskInstance taskInstance = student.getTaskInstanceByConfiguration(ConfigurationManagerImpl.getInstance().getConfigurationByCourseID(courseID, courseNodeID));	
			TaskResult taskResult = taskInstance.getResult();
			if(taskResult!=null) {
				scoreEvaluation = new AssessmentEvaluation(Float.valueOf(String.valueOf(taskResult.getMaxScore())), true);
			}
		}
		catch (Exception e) {};
		
		return scoreEvaluation;
	}

	@Override
	public String getUserUserComment(UserCourseEnvironment userCourseEnvironment) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasAttemptsConfigured() {
		return true;
	}

	@Override
	public boolean hasCommentConfigured() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasDetails() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasPassedConfigured() {
		
		return true;
	}

	@Override
	public boolean hasScoreConfigured() {
		
		return true;
	}

	@Override
	public boolean hasStatusConfigured() {

		return false;
	}

	@Override
	public void incrementUserAttempts(UserCourseEnvironment userCourseEnvironment, Role doneBy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isEditableConfigured() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void updateUserAttempts(Integer userAttempts, UserCourseEnvironment userCourseEnvironment, Identity coachingIdentity, Role doneBy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateUserCoachComment(String coachComment, UserCourseEnvironment userCourseEnvironment) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateUserScoreEvaluation(ScoreEvaluation scoreEvaluation, UserCourseEnvironment userCourseEnvironment,
			Identity coachingIdentity, boolean incrementAttempts, Role doneBy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateUserUserComment(String userComment, UserCourseEnvironment userCourseEnvironment, Identity coachingIdentity) {
		// TODO Auto-generated method stub
		
	}
	
	private String getExportFilename() {
		return "autolatExport_"+this.getIdent()+".xml";
	}

	@Override
	public void exportNode(File exportDirectory, ICourse course) {		
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			long courseNodeID = Long.valueOf(getIdent());
			Configuration conf = ConfigurationManagerImpl.getInstance().getConfigurationByCourseID(course.getResourceableId(), courseNodeID);
			AutOlatNodeExporter exporter = new AutOlatNodeExporter(conf);
			exporter.exportNode(baos);
			ExportUtil.writeContentToFile(getExportFilename(), baos.toString(), exportDirectory, WebappHelper.getDefaultCharset());	
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	@Override
	public void importNode(File importDirectory, ICourse course, Identity identity, Locale locale, boolean withReferences)
	{		
		ModuleConfiguration config = getModuleConfiguration();
		File importFile = new File(importDirectory, getExportFilename());
		
		Configuration conf = ConfigurationManagerImpl.getInstance().getConfigurationByCourseID(course.getResourceableId(), Long.valueOf(getIdent()));
		
		if (importFile.exists()) {
			AutOlatNodeImporter importer = new AutOlatNodeImporter(conf);
			try {																	
				importer.importFromFile(importFile);
					
				//ServerConnectionManagerImpl.getInstance().saveOrUpdateServerConnection(conf.getServerConnection());
				TaskTypeManagerImpl.getInstance().saveOrUpdateTaskType(conf.getTaskConfiguration().getTaskType());
				TaskConfigurationManagerImpl.getInstance().saveOrUpdateTaskConfiguration(conf.getTaskConfiguration());
				ConfigurationManagerImpl.getInstance().updateConfiguration(conf);
							
				config.setBooleanEntry(NodeEditController.CONFIG_STARTPAGE, false);
				config.setConfigurationVersion(1);
				config.setBooleanEntry("ServerConnectionSet", true);
				config.setBooleanEntry("TaskTypeValid", true);
				config.setBooleanEntry("GradingTimeSet", false);
			} catch (Exception e) {
				// import failed, roll the node back to factory settings
				updateModuleConfigDefaults(true);
				TaskConfigurationManagerImpl.getInstance().deleteTaskConfiguration(conf.getTaskConfiguration());
				ConfigurationManagerImpl.getInstance().deleteConfiguration(conf);
				// optional: create a new, empty configuration (not needed, missing configuration = empty configuration
				e.printStackTrace();				
			}
		} else {
			// nothing there to import, leave the node as it is
		}
	}

	@Override
	public void postCopy(CourseEnvironmentMapper envMapper, Processing processType, ICourse course, ICourse sourceCrourse) {
		super.postCopy(envMapper, processType, course, sourceCrourse);

		ConfigurationManager cm = ConfigurationManagerImpl.getInstance();

		// Only import the configuration once:
		//  * course copy / published task: called twice (first Processing.runstructure, then Processing.editor)
		//  * course copy / unpublished task: called once (Processing.editor)
		//  * node copy: not called at all (BUG?)
		Configuration newConfig = cm.findConfigurationByCourseID(course.getResourceableId(), Long.valueOf(getIdent()));
		if(newConfig == null) {
			newConfig = cm.createAndPersistConfiguration(course.getResourceableId(), Long.valueOf(getIdent()));

			Configuration oldConfig = cm.getConfigurationByCourseID(sourceCrourse.getResourceableId(), Long.valueOf(getIdent()));

			try {
				// Copy old config to new one by using the exporter/importer
				AutOlatNodeExporter exporter = new AutOlatNodeExporter(oldConfig);
				AutOlatNodeImporter importer = new AutOlatNodeImporter(newConfig);

				ByteArrayOutputStream ostream = new ByteArrayOutputStream();
				exporter.exportNode(ostream);
				ByteArrayInputStream istream = new ByteArrayInputStream(ostream.toByteArray());
				importer.importFromStream(istream);

				// grading time is not a part of the export/import
				newConfig.setBeginDate(oldConfig.getBeginDate());
				newConfig.setEndDate(oldConfig.getEndDate());

				TaskTypeManagerImpl.getInstance().saveOrUpdateTaskType(newConfig.getTaskConfiguration().getTaskType());
				TaskConfigurationManagerImpl.getInstance().saveOrUpdateTaskConfiguration(newConfig.getTaskConfiguration());
				cm.updateConfiguration(newConfig);
			} catch(Exception e) {
				// reset to defaults
				updateModuleConfigDefaults(true);
				// delete config (will be recreated at first get)
				TaskConfigurationManagerImpl.getInstance().deleteTaskConfiguration(newConfig.getTaskConfiguration());
				cm.deleteConfiguration(newConfig);
			}
		}
	}

	@Override
	public void cleanupOnDelete(ICourse course) {
		// Do not delete the Configuration as there may be a TaskInstance referring to it
		// Do not delete the TaskConfiguration as it may be shared by multiple Configurations
	}

	@Override
	public Controller getDetailsEditController(UserRequest ureq,
			WindowControl wControl, BreadcrumbPanel stackPanel,
			UserCourseEnvironment coachCourseEnv, UserCourseEnvironment assessedUserCourseEnvironment) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isAssessedBusinessGroups() {
		return false;
	}

	@Override
	public boolean hasIndividualAsssessmentDocuments() {
		return false;
	}

	@Override
	public List<File> getIndividualAssessmentDocuments(UserCourseEnvironment userCourseEnvironment) {
		return Collections.emptyList();
	}

	@Override
	public void addIndividualAssessmentDocument(File document, String filename, UserCourseEnvironment userCourseEnvironment, Identity coachingIdentity) {
		// individual assessment documents not supported
	}

	@Override
	public void removeIndividualAssessmentDocument(File document, UserCourseEnvironment userCourseEnvironment, Identity coachingIdentity) {
		// individual assessment documents not supported
	}

	@Override
	public void updateLastModifications(UserCourseEnvironment userCourseEnvironment, Identity identity, Role doneBy) {
		// not supported (cannot be manually assessed)
	}

	@Override
	public boolean hasCompletion() {
		return false;
	}

	@Override
	public Double getUserCurrentRunCompletion(UserCourseEnvironment userCourseEnvironment) {
		throw new OLATRuntimeException(BBautOLATCourseNode.class, "No completion available for autolat nodes", null);
	}

	@Override
	public AssessmentCourseNodeController getIdentityListController(UserRequest ureq, WindowControl wControl,
			TooledStackedPanel stackPanel, RepositoryEntry courseEntry, BusinessGroup group,
			UserCourseEnvironment coachCourseEnv, AssessmentToolContainer toolContainer,
			AssessmentToolSecurityCallback assessmentCallback) {
		return new IdentityListCourseNodeController(ureq, wControl, stackPanel,
				courseEntry, group, this, coachCourseEnv, toolContainer, assessmentCallback);
	}

	@Override
	public void updateCurrentCompletion(UserCourseEnvironment userCourseEnvironment, Identity identity,
			Double currentCompletion, AssessmentRunStatus status, Role doneBy) {
		throw new OLATRuntimeException(BBautOLATCourseNode.class, "Completion variable can't be updated in autolat nodes", null);
	}
}