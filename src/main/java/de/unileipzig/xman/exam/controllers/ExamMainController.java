package de.unileipzig.xman.exam.controllers;

import java.util.Calendar;
import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.stack.PopEvent;
import org.olat.core.gui.components.stack.TooledStackedPanel;
import org.olat.core.gui.components.stack.TooledStackedPanel.Align;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.MainLayoutBasicController;
import org.olat.core.gui.control.generic.dtabs.Activateable2;
import org.olat.core.gui.control.generic.dtabs.DTab;
import org.olat.core.gui.control.generic.dtabs.DTabs;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.modal.DialogBoxUIFactory;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.id.context.StateEntry;
import org.olat.core.util.Util;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.ui.list.RepositoryEntryDetailsController;
import org.olat.resource.OLATResourceManager;

import de.unileipzig.xman.appointment.AppointmentManager;
import de.unileipzig.xman.exam.AlreadyLockedException;
import de.unileipzig.xman.exam.Exam;
import de.unileipzig.xman.exam.ExamDBManager;
import de.unileipzig.xman.exam.components.SelectDropdown;
import de.unileipzig.xman.protocol.ProtocolManager;

public class ExamMainController extends MainLayoutBasicController implements Activateable2 {
	
	static public enum View {
		STUDENT,	// normal oo user
		LECTURER, 	// that is able to edit: owner, examoffice or admin
		OTHER 		// guests
	}
	
	private Exam exam;
	private View view;
	private TooledStackedPanel toolbarStack;
	private Link editorLink;
	private Link detailsLink;
	private SelectDropdown examType;
	private DialogBoxController changeToOralDialog;
	private DialogBoxController changeToWrittenDialog;
	private boolean inEditor;

	/**
	 * The Controller that manages the display and the edit of an exam
	 * 
	 * Pass one of View.STUDENT, View.LECTURER, View.OTHER to specify the view
	 * 
	 * @see View
	 * @param ureq
	 * @param wControl
	 * @param exam The exam to edit or display
	 * @param view The view that should be presented to the user
	 * @throws AlreadyLockedException 
	 */
	public ExamMainController(UserRequest ureq, WindowControl wControl, Exam exam, View view) {
		super(ureq, wControl);
		
		setTranslator(Util.createPackageTranslator(Exam.class, getLocale()));
		this.exam = exam;
		this.view = view;
		
		toolbarStack = new TooledStackedPanel("examStackPanel", getTranslator(), this);
		init(ureq);
		putInitialPanel(toolbarStack);
	}
	
	public ExamMainController(UserRequest ureq, WindowControl wControl, Exam exam, View view, boolean launchEditor) throws AlreadyLockedException {
		super(ureq, wControl);
		
		setTranslator(Util.createPackageTranslator(Exam.class, getLocale()));
		this.exam = exam;
		this.view = view;
		
		toolbarStack = new TooledStackedPanel("examStackPanel", getTranslator(), this);
		init(ureq);
		putInitialPanel(toolbarStack);
		
		if(launchEditor) {
			pushEditor(ureq); // can throw AlreadyLockedException
		}
	}
	
	/**
	 * Init method so we can throw an exception from only one constructor
	 */
	private void init(UserRequest ureq) {
		toolbarStack.setShowCloseLink(true, true);
		
		// initialize exam registration dates
		if(exam.getRegStartDate() == null) {
			Calendar date = Calendar.getInstance();
			date.add(Calendar.DAY_OF_MONTH, 1);
			date.set(Calendar.HOUR_OF_DAY, 0);
			date.set(Calendar.MINUTE, 0);
			date.set(Calendar.SECOND, 0);
			exam.setRegStartDate(date.getTime());

			date.add(Calendar.MONTH, 1);
			date.set(Calendar.HOUR_OF_DAY, 23);
			date.set(Calendar.MINUTE, 59);
			date.set(Calendar.SECOND, 59);
			exam.setRegEndDate(date.getTime());
			exam.setSignOffDate(date.getTime());
		}
		
		String name = exam.getName() + " (" + (exam.getIsOral() ? translate("oral") : translate("written")) + ")";
		if(view == View.STUDENT) {
			Controller examController = new ExamStudentController(ureq, getWindowControl(), exam);
			toolbarStack.rootController(name, examController);
		} else if(view == View.LECTURER) {
			Controller examController;
			if(exam.getIsOral()) {
				examController = new ExamLecturerOralController(ureq, getWindowControl(), exam);
			} else {
				examController = new ExamLecturerWrittenController(ureq, getWindowControl(), exam);
			}
			toolbarStack.setInvisibleCrumb(0); // Show the toolbar also on the top level
			toolbarStack.addListener(examController); // notify controllers of PopEvent so that they can refresh the exam
			toolbarStack.rootController(name, examController);
			buildToolbar();
		} else if(view == View.OTHER) {
			getWindowControl().setError("Don't have access!!");
			return;
		}
	}
	
	private void buildToolbar() {
		editorLink = LinkFactory.createToolLink("editor", translate("ExamMainController.tool.editExam"), this, "o_icon_courseeditor");
		toolbarStack.addTool(editorLink);
		
		detailsLink = LinkFactory.createToolLink("details", translate("ExamMainController.tool.info"), this, "o_icon_details");
		toolbarStack.addTool(detailsLink);

		examType = new SelectDropdown("examType",
				new String[] {"written", "oral"},
				new String[] {"ExamMainController.tool.examType.written", "ExamMainController.tool.examType.oral"},
				new String[] {"o_icon_exam_written", "o_icon_exam_oral"}, getTranslator());
		if(exam.getIsOral()) {
			examType.select("oral");
		}
		examType.addListener(this);
		toolbarStack.addTool(examType, Align.left);
	}

	private void pushEditor(UserRequest ureq) throws AlreadyLockedException {
		if(ExamDBManager.getInstance().isClosed(exam)) {
			showInfo("ExamMainController.info.closed");
			return;
		}
		if(inEditor) {
			return;
		}
		
		OLATResourceable res = OLATResourceManager.getInstance().findResourceable(exam.getResourceableId(), exam.getResourceableTypeName());
		toolbarStack.pushController(translate("examEditor_html.header"), new ExamEditorController(ureq, getWindowControl(), res));
		inEditor = true;
	}

	private void pushDetails(UserRequest ureq) {
		RepositoryEntry re = ExamDBManager.getInstance().findRepositoryEntryOfExam(exam);
		toolbarStack.pushController(translate("ExamMainController.stack.infopage"), new RepositoryEntryDetailsController(ureq, getWindowControl(), re));
	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		 if(source == toolbarStack) {
			if(event instanceof PopEvent) {
				PopEvent popEvent = (PopEvent) event;
				if(popEvent.getController() instanceof ExamEditorController) {
					inEditor = false;
				}
			} else if(event == Event.CLOSE_EVENT) {
				// close the tab we are in
				DTabs tabs = getWindowControl().getWindowBackOffice().getWindow().getDTabs();
				if (tabs != null) {
					DTab tab = tabs.getDTab(ExamDBManager.getInstance().findRepositoryEntryOfExam(exam).getOlatResource());
					if (tab != null) {
						tabs.removeDTab(ureq, tab);
					}
				}
			}
		} else if (source == editorLink) {
			try {
				pushEditor(ureq);
			} catch(AlreadyLockedException e) {
				getWindowControl().setInfo(translate("ExamEditorController.alreadyLocked", new String[] { e.getName() }));
			}
		} else if(source == detailsLink) {
			pushDetails(ureq);
		} else if(source == examType) {
			if(ExamDBManager.getInstance().isClosed(exam)) {
				showInfo("ExamMainController.info.closed");
				return;
			}
			if(ProtocolManager.getInstance().findAllProtocolsByExam(exam).size() > 0) {
				showError("ExamMainController.error.studentsSubscribed");
				return;
			}

			String newType = event.getCommand();
			if(newType.equals("oral")) {
				if(exam.getIsOral()) {
					return;
				}
				changeToOralDialog = activateOkCancelDialog(ureq, translate("ExamMainController.dialog.examType.title"), translate("ExamMainController.dialog.examType.oral"), changeToOralDialog);
			} else if(newType.equals("written")) {
				if(!exam.getIsOral()) {
					return;
				}
				changeToWrittenDialog = activateOkCancelDialog(ureq, translate("ExamMainController.dialog.examType.title"), translate("ExamMainController.dialog.examType.written"), changeToWrittenDialog);
			}
		}
	}
	
	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if(source == changeToOralDialog) {
			if(DialogBoxUIFactory.isOkEvent(event)) {
				changeExamType(ureq, true);
			}
		} else if(source == changeToWrittenDialog) {
			if(DialogBoxUIFactory.isOkEvent(event)) {
				changeExamType(ureq, false);
			}
		}
	}

	@Override
	public void activate(UserRequest ureq, List<ContextEntry> entries, StateEntry state) {
		if(entries != null && entries.size() > 0) {
			String action = entries.get(0).getOLATResourceable().getResourceableTypeName();

			if("Editor".equals(action)) {
				try {
					pushEditor(ureq);
				} catch(AlreadyLockedException e) {
					getWindowControl().setInfo(translate("ExamEditorController.alreadyLocked", new String[] { e.getName() }));
				}
			} else if("Infos".equals(action)) {
				pushDetails(ureq);
			}
		}
	}

	private void changeExamType(UserRequest ureq, boolean oral) {
		if(ProtocolManager.getInstance().findAllProtocolsByExam(exam).size() > 0) {
			showError("ExamMainController.error.studentsSubscribed");
			return;
		}

		AppointmentManager.getInstance().deleteAllAppointmentsByExam(exam);
		exam = ExamDBManager.getInstance().findExamByID(exam.getKey());
		exam.setIsOral(oral);

		// rebuild the site (stack is cleared automatically by rootController)
		init(ureq);
	}

	@Override
	protected void doDispose() {
		removeAsListenerAndDispose(changeToOralDialog);
		removeAsListenerAndDispose(changeToWrittenDialog);
		if(inEditor) {
			toolbarStack.popContent(); // disposes the editor controller and thus releases the lock
		}
	}
}
