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
package org.olat.ims.qti21.ui;

import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.MultipartFileInfos;
import org.olat.core.gui.components.form.flexible.impl.elements.FormSubmit;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.fileresource.types.ImsQTI21Resource;
import org.olat.fileresource.types.ImsQTI21Resource.PathResourceLocator;
import org.olat.ims.qti21.AssessmentSessionAuditLogger;
import org.olat.ims.qti21.AssessmentTestSession;
import org.olat.ims.qti21.QTI21DeliveryOptions;
import org.olat.ims.qti21.QTI21Service;
import org.olat.ims.qti21.manager.audit.AssessmentSessionAuditDevNull;
import org.olat.ims.qti21.model.InMemoryAssessmentTestSession;
import org.olat.ims.qti21.model.audit.CandidateEvent;
import org.olat.ims.qti21.model.audit.CandidateItemEventType;
import org.olat.ims.qti21.ui.components.AssessmentItemFormItem;
import org.olat.modules.assessment.AssessmentEntry;
import org.olat.repository.RepositoryEntry;
import org.springframework.beans.factory.annotation.Autowired;

import uk.ac.ed.ph.jqtiplus.JqtiPlus;
import uk.ac.ed.ph.jqtiplus.exception.QtiCandidateStateException;
import uk.ac.ed.ph.jqtiplus.node.result.AssessmentResult;
import uk.ac.ed.ph.jqtiplus.node.test.AssessmentItemRef;
import uk.ac.ed.ph.jqtiplus.notification.NotificationLevel;
import uk.ac.ed.ph.jqtiplus.notification.NotificationRecorder;
import uk.ac.ed.ph.jqtiplus.resolution.ResolvedAssessmentItem;
import uk.ac.ed.ph.jqtiplus.running.ItemProcessingInitializer;
import uk.ac.ed.ph.jqtiplus.running.ItemSessionController;
import uk.ac.ed.ph.jqtiplus.running.ItemSessionControllerSettings;
import uk.ac.ed.ph.jqtiplus.state.ItemProcessingMap;
import uk.ac.ed.ph.jqtiplus.state.ItemSessionState;
import uk.ac.ed.ph.jqtiplus.types.FileResponseData;
import uk.ac.ed.ph.jqtiplus.types.Identifier;
import uk.ac.ed.ph.jqtiplus.types.ResponseData;
import uk.ac.ed.ph.jqtiplus.types.StringResponseData;
import uk.ac.ed.ph.jqtiplus.xmlutils.locators.ResourceLocator;

/**
 * 
 * Initial date: 22.05.2015<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class AssessmentItemDisplayController extends BasicController implements CandidateSessionContext {
	
	private final VelocityContainer mainVC;
	private QtiWorksController qtiWorksCtrl;
	
	private ItemSessionController itemSessionController;
	
	private final String mapperUri;
	private final File fUnzippedDirRoot;
	private final File itemFileRef;
	private final QTI21DeliveryOptions deliveryOptions;
	private final ResolvedAssessmentItem resolvedAssessmentItem;
	
	private CandidateEvent lastEvent;
	private Date currentRequestTimestamp;
	private RepositoryEntry entry;
	private AssessmentTestSession candidateSession;
	
	private AssessmentSessionAuditLogger candidateAuditLogger = new AssessmentSessionAuditDevNull();

	@Autowired
	private QTI21Service qtiService;
	
	/**
	 * OPen in memory session
	 * @param ureq
	 * @param wControl
	 * @param authorMode
	 * @param resolvedAssessmentItem
	 * @param fUnzippedDirRoot
	 * @param itemFileRef
	 */
	public AssessmentItemDisplayController(UserRequest ureq, WindowControl wControl, ResolvedAssessmentItem resolvedAssessmentItem,
			File fUnzippedDirRoot, File itemFileRef) {
		super(ureq, wControl);
		
		this.itemFileRef = itemFileRef;
		this.fUnzippedDirRoot = fUnzippedDirRoot;
		this.resolvedAssessmentItem = resolvedAssessmentItem;
		deliveryOptions = QTI21DeliveryOptions.defaultSettings();
		currentRequestTimestamp = ureq.getRequestTimestamp();
		candidateSession = new InMemoryAssessmentTestSession();
		mapperUri = registerCacheableMapper(null, UUID.randomUUID().toString(), new ResourcesMapper(itemFileRef.toURI()));
		
		itemSessionController = enterSession(ureq);
		
		if (itemSessionController.getItemSessionState().isEnded()) {
			mainVC = createVelocityContainer("end");
		} else {
			mainVC = createVelocityContainer("run");
        	initQtiWorks(ureq);
		}
		putInitialPanel(mainVC);
	}
	
	public AssessmentItemDisplayController(UserRequest ureq, WindowControl wControl,
			ResolvedAssessmentItem resolvedAssessmentItem, AssessmentItemRef itemRef, File fUnzippedDirRoot) {
		super(ureq, wControl);
		
		this.itemFileRef = new File(fUnzippedDirRoot, itemRef.getHref().toString());
		this.fUnzippedDirRoot = fUnzippedDirRoot;
		this.resolvedAssessmentItem = resolvedAssessmentItem;
		deliveryOptions = QTI21DeliveryOptions.defaultSettings();
		currentRequestTimestamp = ureq.getRequestTimestamp();
		candidateSession = new InMemoryAssessmentTestSession();
		mapperUri = registerCacheableMapper(null, UUID.randomUUID().toString(), new ResourcesMapper(itemFileRef.toURI()));
		
		itemSessionController = enterSession(ureq);
		
		if (itemSessionController.getItemSessionState().isEnded()) {
			mainVC = createVelocityContainer("end");
		} else {
			mainVC = createVelocityContainer("run");
        	initQtiWorks(ureq);
		}
		putInitialPanel(mainVC);
	}
	
	public AssessmentItemDisplayController(UserRequest ureq, WindowControl wControl,
			RepositoryEntry testEntry, AssessmentEntry assessmentEntry, boolean authorMode,
			ResolvedAssessmentItem resolvedAssessmentItem, AssessmentItemRef itemRef, File fUnzippedDirRoot) {
		super(ureq, wControl);
		
		this.itemFileRef = new File(fUnzippedDirRoot, itemRef.getHref().toString());
		this.fUnzippedDirRoot = fUnzippedDirRoot;
		this.resolvedAssessmentItem = resolvedAssessmentItem;
		deliveryOptions = QTI21DeliveryOptions.defaultSettings();
		currentRequestTimestamp = ureq.getRequestTimestamp();
		candidateSession = qtiService.createAssessmentTestSession(getIdentity(), assessmentEntry, testEntry, itemRef.getIdentifier().toString(), testEntry, authorMode);
		mapperUri = registerCacheableMapper(null, UUID.randomUUID().toString(), new ResourcesMapper(itemFileRef.toURI()));
		
		itemSessionController = enterSession(ureq);
		
		if (itemSessionController.getItemSessionState().isEnded()) {
			mainVC = createVelocityContainer("end");
		} else {
			mainVC = createVelocityContainer("run");
        	initQtiWorks(ureq);
		}
		putInitialPanel(mainVC);
	}
	
	private void initQtiWorks(UserRequest ureq) {
		String filename = itemFileRef.getName();
		qtiWorksCtrl = new QtiWorksController(ureq, getWindowControl(), filename);
    	listenTo(qtiWorksCtrl);
    	mainVC.put("qtirun", qtiWorksCtrl.getInitialComponent());
	}

	@Override
	protected void doDispose() {
		//
	}

	@Override
	public boolean isTerminated() {
		return false;
	}

	@Override
	public AssessmentTestSession getCandidateSession() {
		return candidateSession;
	}

	@Override
	public CandidateEvent getLastEvent() {
		return lastEvent;
	}

	@Override
	public Date getCurrentRequestTimestamp() {
		return currentRequestTimestamp;
	}

	@Override
	public boolean isMarked(String itemKey) {
		return false;
	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		currentRequestTimestamp = ureq.getRequestTimestamp();
		//
	}
	
	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if(qtiWorksCtrl == source) {
			if(event instanceof QTIWorksAssessmentItemEvent) {
				processQTIEvent(ureq, (QTIWorksAssessmentItemEvent)event);
			}
		}
		super.event(ureq, source, event);
	}
	
	private void processQTIEvent(UserRequest ureq, QTIWorksAssessmentItemEvent qe) {
		currentRequestTimestamp = ureq.getRequestTimestamp();
		
		switch(qe.getEvent()) {
			case solution:
				requestSolution(ureq);
				break;
			case response:
				handleResponses(ureq, qe.getStringResponseMap(), qe.getFileResponseMap(), qe.getComment());
				break;
			case close:
				endSession(ureq);
				break;
			case exit:
				exitSession(ureq);
				break;
			case resetsoft:
				break;
			case resethard:
				break;
			case source:
				logError("QtiWorks event source not implemented", null);
				break;
			case state:
				logError("QtiWorks event state not implemented", null);
				break;
			case validation:
				logError("QtiWorks event validation not implemented", null);
				break;
			case authorview:
				logError("QtiWorks event authorview not implemented", null);
				break;
			case result:
				logError("QtiWorks event result not implemented", null);
				break;
		}
	}
	
	private ItemSessionController enterSession(UserRequest ureq /*, final UserTestSession candidateSession */) {
        /* Set up listener to record any notifications */
        final NotificationRecorder notificationRecorder = new NotificationRecorder(NotificationLevel.INFO);

        /* Create fresh JQTI+ state Object and try to create controller */
        itemSessionController = createNewItemSessionStateAndController(notificationRecorder);
        if (itemSessionController==null) {
        	logError("", null);
            return null;//handleExplosion(null, candidateSession);
        }

        /* Try to Initialise JQTI+ state */
        final ItemSessionState itemSessionState = itemSessionController.getItemSessionState();
        try {
            final Date timestamp = ureq.getRequestTimestamp();
            itemSessionController.initialize(timestamp);
            itemSessionController.performTemplateProcessing(timestamp);
            itemSessionController.enterItem(timestamp);
        }
        catch (final RuntimeException e) {
        	logError("", e);
            return null;//handleExplosion(null, candidateSession);
        }

        /* Record and log entry event */
        final CandidateEvent candidateEvent = qtiService.recordCandidateItemEvent(candidateSession, null, entry,
        		CandidateItemEventType.ENTER, itemSessionState, notificationRecorder);
        //candidateAuditLogger.logCandidateEvent(candidateEvent);
        lastEvent = candidateEvent;

        /* Record current result state */
        final AssessmentResult assessmentResult = computeAndRecordItemAssessmentResult(ureq);

        /* Handle immediate end of session */
        if (itemSessionState.isEnded()) {
            qtiService.finishItemSession(candidateSession, assessmentResult, ureq.getRequestTimestamp());
        }

        return itemSessionController;
    }
	
    public ItemSessionController createNewItemSessionStateAndController(NotificationRecorder notificationRecorder) {
        /* Resolve the underlying JQTI+ object */
        final ItemProcessingMap itemProcessingMap = getItemProcessingMap();
        if (itemProcessingMap == null) {
            return null;
        }

        /* Create fresh state for session */
        final ItemSessionState itemSessionState = new ItemSessionState();

        /* Create config for ItemSessionController */
        final ItemSessionControllerSettings itemSessionControllerSettings = new ItemSessionControllerSettings();
        itemSessionControllerSettings.setTemplateProcessingLimit(computeTemplateProcessingLimit());
        itemSessionControllerSettings.setMaxAttempts(10 /*itemDeliverySettings.getMaxAttempts() */);

        /* Create controller and wire up notification recorder */
        final ItemSessionController result = new ItemSessionController(qtiService.jqtiExtensionManager(),
                itemSessionControllerSettings, itemProcessingMap, itemSessionState);
        if (notificationRecorder != null) {
            result.addNotificationListener(notificationRecorder);
        }
        return result;
    }
    
    public ItemProcessingMap getItemProcessingMap() {
        ItemProcessingMap result = new ItemProcessingInitializer(resolvedAssessmentItem, true).initialize();
        return result;
    }
    
	public int computeTemplateProcessingLimit() {
		final Integer requestedLimit = deliveryOptions.getTemplateProcessingLimit();
		if (requestedLimit == null) {
			/* Not specified, so use default */
			return JqtiPlus.DEFAULT_TEMPLATE_PROCESSING_LIMIT;
		}
		final int requestedLimitIntValue = requestedLimit.intValue();
		return requestedLimitIntValue > 0 ? requestedLimitIntValue : JqtiPlus.DEFAULT_TEMPLATE_PROCESSING_LIMIT;
	}
	
	public void handleResponses(UserRequest ureq, Map<Identifier, StringResponseData> stringResponseMap,
			Map<Identifier,MultipartFileInfos> fileResponseMap, String candidateComment) {
		
		//Assert.notNull(candidateSessionContext, "candidateSessionContext");
		// assertSessionType(candidateSessionContext, AssessmentObjectType.ASSESSMENT_ITEM);
		// final CandidateSession candidateSession = candidateSessionContext.getCandidateSession();
		// assertSessionNotTerminated(candidateSession);

		/* Retrieve current JQTI state and set up JQTI controller */
		NotificationRecorder notificationRecorder = new NotificationRecorder(NotificationLevel.INFO);
		//final ItemSessionController itemSessionController = this.candidateDataService.createItemSessionController(mostRecentEvent, notificationRecorder);
		ItemSessionState itemSessionState = itemSessionController.getItemSessionState();

		/* Make sure an attempt is allowed */
		if (itemSessionState.isEnded()) {
			//candidateAuditLogger.logAndThrowCandidateException(candidateSession, CandidateExceptionReason.RESPONSES_NOT_EXPECTED);
			logError("RESPONSES_NOT_EXPECTED", null);
			return;
		}

		/* Make sure candidate may comment (if set) */
		/*
		if (candidateComment != null && !itemDeliverySettings.isAllowCandidateComment()) {
			candidateAuditLogger.logAndThrowCandidateException(candidateSession, CandidateExceptionReason.CANDIDATE_COMMENT_FORBIDDEN);
			return null;
		}
		 */

		/* Build response map in required format for JQTI+.
		 * NB: The following doesn't test for duplicate keys in the two maps. I'm not sure
		 * it's worth the effort.
		 */
		final Map<Identifier, ResponseData> responseDataMap = new HashMap<Identifier, ResponseData>();
		if (stringResponseMap!=null) {
			for (final Entry<Identifier, StringResponseData> stringResponseEntry : stringResponseMap.entrySet()) {
				final Identifier identifier = stringResponseEntry.getKey();
				final StringResponseData stringResponseData = stringResponseEntry.getValue();
				responseDataMap.put(identifier, stringResponseData);
			}
		}
		
	       // final Map<Identifier, CandidateFileSubmission> fileSubmissionMap = new HashMap<Identifier, CandidateFileSubmission>();
        if (fileResponseMap!=null) {
            for (final Entry<Identifier, MultipartFileInfos> fileResponseEntry : fileResponseMap.entrySet()) {
                final Identifier identifier = fileResponseEntry.getKey();
                final MultipartFileInfos multipartFile = fileResponseEntry.getValue();
                if (!multipartFile.isEmpty()) {
                    //final CandidateFileSubmission fileSubmission = candidateUploadService.importFileSubmission(candidateSession, multipartFile);
                	File storedFile = qtiService.importFileSubmission(candidateSession, multipartFile);
                	final FileResponseData fileResponseData = new FileResponseData(storedFile, multipartFile.getContentType(), multipartFile.getFileName());
                    responseDataMap.put(identifier, fileResponseData);
                    //fileSubmissionMap.put(identifier, fileSubmission);
                }
            }
        }

		/* Submit comment (if provided)
		 * NB: Do this first in case next actions end the item session.
		 */
		final Date timestamp = ureq.getRequestTimestamp();
		if (candidateComment != null) {
			try {
				itemSessionController.setCandidateComment(timestamp, candidateComment);
			} catch (final QtiCandidateStateException e) {
				//candidateAuditLogger.logAndThrowCandidateException(candidateSession, CandidateExceptionReason.CANDIDATE_COMMENT_FORBIDDEN);
				logError("CANDIDATE_COMMENT_FORBIDDEN", null);
				return;
			} catch (final RuntimeException e) {
				logError("", e);
				return; //handleExplosion(e, candidateSession);
			}
		}

		/* Attempt to bind responses */
		boolean allResponsesValid = false, allResponsesBound = false;
		try {
			itemSessionController.bindResponses(timestamp, responseDataMap);

			/* Note any responses that failed to bind */
			final Set<Identifier> badResponseIdentifiers = itemSessionState.getUnboundResponseIdentifiers();
			allResponsesBound = badResponseIdentifiers.isEmpty();


			/* Now validate the responses according to any constraints specified by the interactions */
			if (allResponsesBound) {
				final Set<Identifier> invalidResponseIdentifiers = itemSessionState.getInvalidResponseIdentifiers();
				allResponsesValid = invalidResponseIdentifiers.isEmpty();
			}

			/* (We commit responses immediately here) */
			itemSessionController.commitResponses(timestamp);

			/* Invoke response processing (only if responses are valid) */
			if (allResponsesValid) {
				itemSessionController.performResponseProcessing(timestamp);
			}
		} catch (final QtiCandidateStateException e) {
	            //candidateAuditLogger.logAndThrowCandidateException(candidateSession, CandidateExceptionReason.RESPONSES_NOT_EXPECTED);
			logError("RESPONSES_NOT_EXPECTED", e);
			return;
		} catch (final RuntimeException e) {
			logError("", e);
			return;// handleExplosion(e, candidateSession);
		}

		/* Record resulting attempt and event */
		final CandidateItemEventType eventType = allResponsesBound ?
				(allResponsesValid ? CandidateItemEventType.ATTEMPT_VALID : CandidateItemEventType.RESPONSE_INVALID)
				: CandidateItemEventType.RESPONSE_BAD;
		final CandidateEvent candidateEvent = qtiService.recordCandidateItemEvent(candidateSession, null, entry,
	                eventType, itemSessionState, notificationRecorder);
		//candidateAuditLogger.logCandidateEvent(candidateEvent);
		lastEvent = candidateEvent;

		/* Record current result state, or finish session */
		updateSessionFinishedStatus(ureq);
	}
	
    private AssessmentTestSession updateSessionFinishedStatus(UserRequest ureq) {
        /* Record current result state and maybe close session */
        final ItemSessionState itemSessionState = itemSessionController.getItemSessionState();
        final AssessmentResult assessmentResult = computeAndRecordItemAssessmentResult(ureq);
        if (itemSessionState.isEnded()) {
            qtiService.finishItemSession(candidateSession, assessmentResult, null);
        }
        else {
            if (candidateSession != null && candidateSession.getFinishTime() != null) {
                /* (Session is being reopened) */
                candidateSession.setFinishTime(null);
                candidateSession = qtiService.updateAssessmentTestSession(candidateSession);
            }
        }
        return candidateSession;
    }
    
    public AssessmentResult computeAndRecordItemAssessmentResult(UserRequest ureq) {
        final AssessmentResult assessmentResult = computeItemAssessmentResult(ureq);
        qtiService.recordItemAssessmentResult(candidateSession, assessmentResult, candidateAuditLogger);
        return assessmentResult;
    }
    
    public AssessmentResult computeItemAssessmentResult(UserRequest ureq) {
    	String baseUrl = "http://localhost:8080/olat";
        final URI sessionIdentifierSourceId = URI.create(baseUrl);
        final String sessionIdentifier = "itemsession/" + (candidateSession == null ? "sdfj" : candidateSession.getKey());
        return itemSessionController.computeAssessmentResult(ureq.getRequestTimestamp(), sessionIdentifier, sessionIdentifierSourceId);
    }
    
    public void requestSolution(UserRequest ureq) {
        //Assert.notNull(candidateSessionContext, "candidateSessionContext");
        //assertSessionType(candidateSessionContext, AssessmentObjectType.ASSESSMENT_ITEM);
        //final CandidateSession candidateSession = candidateSessionContext.getCandidateSession();
        //assertSessionNotTerminated(candidateSession);

        NotificationRecorder notificationRecorder = new NotificationRecorder(NotificationLevel.INFO);
        ItemSessionState itemSessionState = itemSessionController.getItemSessionState();

        /* Make sure caller may do this */
        boolean allowSolutionWhenOpen = true;//itemDeliverySettings.isAllowSolutionWhenOpen()

        if (!itemSessionState.isEnded()  && !allowSolutionWhenOpen) {
            //candidateAuditLogger.logAndThrowCandidateException(candidateSession, CandidateExceptionReason.SOLUTION_WHEN_INTERACTING_FORBIDDEN);
        	logError("SOLUTION_WHEN_INTERACTING_FORBIDDEN", null);
            return;
        } else if (itemSessionState.isEnded() /* && !itemDeliverySettings.isAllowSoftResetWhenEnded() */) {
            //candidateAuditLogger.logAndThrowCandidateException(candidateSession, CandidateExceptionReason.SOLUTION_WHEN_ENDED_FORBIDDEN);
        	logError("SOLUTION_WHEN_ENDED_FORBIDDEN", null);
            return;
        }

        /* End session if still open */
        final Date timestamp = ureq.getRequestTimestamp();
        boolean isClosingSession = false;
        if (!itemSessionState.isEnded()) {
            isClosingSession = true;
            try {
                itemSessionController.endItem(timestamp);
            } catch (final QtiCandidateStateException e) {
                //candidateAuditLogger.logAndThrowCandidateException(candidateSession, CandidateExceptionReason.SOLUTION_WHEN_ENDED_FORBIDDEN);
            	logError("SOLUTION_WHEN_ENDED_FORBIDDEN", e);
                return;
            } catch (final RuntimeException e) {
            	logError("", e);
                return;// handleExplosion(e, candidateSession);
            }
        }

        /* Record current result state, and maybe close session */
        final AssessmentResult assessmentResult = computeAndRecordItemAssessmentResult(ureq);
        if (isClosingSession) {
            qtiService.finishItemSession(candidateSession, assessmentResult, timestamp);
        }

        /* Record and log event */
        final CandidateEvent candidateEvent = qtiService.recordCandidateItemEvent(candidateSession, null, entry,
        		CandidateItemEventType.SOLUTION, itemSessionState);
        //candidateAuditLogger.logCandidateEvent(candidateEvent);
        lastEvent = candidateEvent;

        //return candidateSession;
    }
    
	public void endSession(UserRequest ureq) {
		//Assert.notNull(candidateSessionContext, "candidateSessionContext");
		//assertSessionType(candidateSessionContext, AssessmentObjectType.ASSESSMENT_ITEM);
		//final CandidateSession candidateSession = candidateSessionContext.getCandidateSession();
		//assertSessionNotTerminated(candidateSession);

        NotificationRecorder notificationRecorder = new NotificationRecorder(NotificationLevel.INFO);
        //final ItemSessionController itemSessionController = candidateDataService.createItemSessionController(mostRecentEvent, notificationRecorder);
        ItemSessionState itemSessionState = itemSessionController.getItemSessionState();

        /* Check this is allowed in current state */
        
        if (itemSessionState.isEnded()) {
        	logError("END_SESSION_WHEN_ALREADY_ENDED", null);
            //candidateAuditLogger.logAndThrowCandidateException(candidateSession, CandidateExceptionReason.END_SESSION_WHEN_ALREADY_ENDED);
            return;
        } /* else if (!itemDeliverySettings.isAllowEnd()) {
            candidateAuditLogger.logAndThrowCandidateException(candidateSession, CandidateExceptionReason.END_SESSION_WHEN_INTERACTING_FORBIDDEN);
            return null;
        }*/

        /* Update state */
        final Date timestamp = ureq.getRequestTimestamp();
        try {
            itemSessionController.endItem(timestamp);
        } catch (QtiCandidateStateException e) {
        	String msg = itemSessionState.isEnded() ? "END_SESSION_WHEN_ALREADY_ENDED" : "END_SESSION_WHEN_INTERACTING_FORBIDDEN";
        	logError(msg, e);
            //candidateAuditLogger.logAndThrowCandidateException(candidateSession, itemSessionState.isEnded() ? CandidateExceptionReason.END_SESSION_WHEN_ALREADY_ENDED : CandidateExceptionReason.END_SESSION_WHEN_INTERACTING_FORBIDDEN);
            return;
        }
        catch (final RuntimeException e) {
        	logError("", e);
            return; //handleExplosion(e, candidateSession);
        }

        /* Record current result state */
        final AssessmentResult assessmentResult = computeAndRecordItemAssessmentResult(ureq);

        /* Record and log event */
        final CandidateEvent candidateEvent = qtiService.recordCandidateItemEvent(candidateSession, null, entry,
                CandidateItemEventType.END, itemSessionState, notificationRecorder);
        //candidateAuditLogger.logCandidateEvent(candidateEvent);
        lastEvent = candidateEvent;

        /* Close session */
        qtiService.finishItemSession(candidateSession, assessmentResult, timestamp);

        //return candidateSession;
    }
	
	   public void exitSession(UserRequest ureq) {
	        //Assert.notNull(candidateSessionContext, "candidateSessionContext");
	        //assertSessionType(candidateSessionContext, AssessmentObjectType.ASSESSMENT_ITEM);
	        //final CandidateSession candidateSession = candidateSessionContext.getCandidateSession();
	        //assertSessionNotTerminated(candidateSession);

	        NotificationRecorder notificationRecorder = new NotificationRecorder(NotificationLevel.INFO);
	        ItemSessionState itemSessionState = itemSessionController.getItemSessionState();

	        /* Are we terminating a session that hasn't already been ended? If so end the session and record final result. */
	        final Date currentTimestamp = ureq.getRequestTimestamp();
	        if (!itemSessionState.isEnded()) {
	            try {
	                itemSessionController.endItem(currentTimestamp);
	            } catch (final RuntimeException e) {
	            	logError("", e);
	                return;// handleExplosion(e, candidateSession);
	            }
	            final AssessmentResult assessmentResult = computeAndRecordItemAssessmentResult(ureq);
	            qtiService.finishItemSession(candidateSession, assessmentResult, currentTimestamp);
	        }

	        /* Update session entity */
	        candidateSession.setTerminationTime(currentTimestamp);
	        candidateSession = qtiService.updateAssessmentTestSession(candidateSession);

	        /* Record and log event */
	        final CandidateEvent candidateEvent = qtiService.recordCandidateItemEvent(candidateSession, null, entry,
	                CandidateItemEventType.EXIT, itemSessionState);
	        lastEvent = candidateEvent;
	        //candidateAuditLogger.logCandidateEvent(candidateEvent);

	        //return candidateSession;
	    }
	
	/**
	 * QtiWorks manage the form tag itself.
	 * 
	 * Initial date: 20.05.2015<br>
	 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
	 *
	 */
	private class QtiWorksController extends AbstractQtiWorksController {
		
		private AssessmentItemFormItem qtiEl;
		private final String filename;
		
		public QtiWorksController(UserRequest ureq, WindowControl wControl, String filename) {
			super(ureq, wControl, "ff_run");
			this.filename = filename;
			initForm(ureq);
		}

		@Override
		protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
			mainForm.setMultipartEnabled(true);

			FormSubmit submit = uifactory.addFormSubmitButton("submit", formLayout);
			qtiEl = new AssessmentItemFormItem("qtirun", submit);
			formLayout.add("qtirun", qtiEl);

			ResourceLocator fileResourceLocator = new PathResourceLocator(fUnzippedDirRoot.toPath());
			final ResourceLocator inputResourceLocator = 
	        		ImsQTI21Resource.createResolvingResourceLocator(fileResourceLocator);
			qtiEl.setResourceLocator(inputResourceLocator);
			qtiEl.setItemSessionController(itemSessionController);
			qtiEl.setResolvedAssessmentItem(resolvedAssessmentItem);

			File manifestPath = new File(fUnzippedDirRoot, filename);
			qtiEl.setAssessmentObjectUri(manifestPath.toURI());
			qtiEl.setCandidateSessionContext(AssessmentItemDisplayController.this);
			qtiEl.setMapperUri(mapperUri);
		}
		
		@Override
		protected Identifier getResponseIdentifierFromUniqueId(String uniqueId) {
			return qtiEl.getInteractionOfResponseUniqueIdentifier(uniqueId).getResponseIdentifier();
		}

		@Override
		protected void formOK(UserRequest ureq) {
			processResponse(ureq, qtiEl.getSubmitButton());
		}

		@Override
		protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
			if(source == qtiEl) {
				if(event instanceof QTIWorksAssessmentItemEvent) {
					fireEvent(ureq, event);
				}
			} else if(source instanceof FormLink) {
				FormLink formLink = (FormLink)source;
				processResponse(ureq, formLink);	
			}
			super.formInnerEvent(ureq, source, event);
		}

		@Override
		protected void fireResponse(UserRequest ureq, FormItem source,
				Map<Identifier, StringResponseData> stringResponseMap, Map<Identifier, MultipartFileInfos> fileResponseMap,
				String comment) {
			fireEvent(ureq, new QTIWorksAssessmentItemEvent(QTIWorksAssessmentItemEvent.Event.response, stringResponseMap, fileResponseMap, comment, source));
		}
	}
}
