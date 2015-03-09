/**
* OLAT - Online Learning and Training<br>
* http://www.olat.org
* <p>
* Licensed under the Apache License, Version 2.0 (the "License"); <br>
* you may not use this file except in compliance with the License.<br>
* You may obtain a copy of the License at
* <p>
* http://www.apache.org/licenses/LICENSE-2.0
* <p>
* Unless required by applicable law or agreed to in writing,<br>
* software distributed under the License is distributed on an "AS IS" BASIS, <br>
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
* See the License for the specific language governing permissions and <br>
* limitations under the License.
* <p>
* Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.  
* <p>
*/ 
package org.olat.core.gui.control.winmgr;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.olat.core.gui.GlobalSettings;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.WindowManager;
import org.olat.core.gui.WindowSettings;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.ComponentRenderer;
import org.olat.core.gui.components.Window;
import org.olat.core.gui.control.ChiefController;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.JSAndCSSAdderImpl;
import org.olat.core.gui.control.WindowBackOffice;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.guistack.GuiStack;
import org.olat.core.gui.control.guistack.GuiStackNiceImpl;
import org.olat.core.gui.control.pushpoll.WindowCommand;
import org.olat.core.gui.control.util.ZIndexWrapper;
import org.olat.core.gui.dev.controller.DevelopmentController;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.gui.media.ServletUtil;
import org.olat.core.gui.render.intercept.InterceptHandler;
import org.olat.core.gui.render.intercept.InterceptHandlerInstance;
import org.olat.core.gui.render.intercept.debug.GuiDebugDispatcherController;
import org.olat.core.helpers.Settings;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.event.GenericEventListener;
import org.olat.core.util.i18n.I18nManager;
import org.olat.core.util.i18n.ui.I18nUIFactory;
import org.olat.core.util.i18n.ui.InlineTranslationInterceptHandlerController;

/**
 * Description:<br>
 * impl of windowbackoffice - responsible for several activities around a (browser)window
 * 
 * <P>
 * Initial Date: 10.02.2007 <br>
 * @author Felix Jost, http://www.goodsolutions.ch
 */
public class WindowBackOfficeImpl implements WindowBackOffice {
	
	private static final OLog log = Tracing.createLoggerFor(WindowBackOfficeImpl.class);

	private final WindowManagerImpl winmgrImpl;
	private Window window;
	private WindowSettings settings;
	private ChiefController windowOwner;
	
	private InterceptHandler linkedInterceptHandler;
	// not private to avoid synthetic accessor
	InterceptHandler debug_interceptHandler = null;
	InterceptHandler inlineTranslation_interceptHandler = null;
	private AjaxController ajaxC;
	private GuiDebugDispatcherController guidebugC;
	private InlineTranslationInterceptHandlerController inlineTranslationC;
		
	private String iframeName;
	
	private List<ZIndexWrapper> guiMessages = new ArrayList<ZIndexWrapper>(); // request-transient render-related data
	
	private transient List<GenericEventListener> cycleListeners = new CopyOnWriteArrayList<GenericEventListener>();
	
	/**
	 * 
	 */
	WindowBackOfficeImpl(final WindowManagerImpl winmgrImpl, String windowName, ChiefController windowOwner, int wboId, WindowSettings settings) {
		this.winmgrImpl = winmgrImpl;
		this.windowOwner = windowOwner;
		this.iframeName = "oaa"+wboId;
		window = new Window(windowName, this);
		this.settings = settings;

		// TODO make simpler, we do only need to support one intercept handler at a time!
		linkedInterceptHandler = new InterceptHandler() {
			public InterceptHandlerInstance createInterceptHandlerInstance() {
				InterceptHandler debugH = debug_interceptHandler;
				InterceptHandler inlineTranslationH = inlineTranslation_interceptHandler;
				final InterceptHandlerInstance debugI = debugH == null? null: debugH.createInterceptHandlerInstance(); 
				final InterceptHandlerInstance inlineTranslationI = (inlineTranslationH == null ? null : inlineTranslationH.createInterceptHandlerInstance());
				return new InterceptHandlerInstance() {
					public ComponentRenderer createInterceptComponentRenderer(ComponentRenderer originalRenderer) {
						ComponentRenderer toUse = originalRenderer;

						if (winmgrImpl.isShowDebugInfo() && debugI != null) {
							toUse = debugI.createInterceptComponentRenderer(toUse);
						}
						if (I18nManager.getInstance().isCurrentThreadMarkLocalizedStringsEnabled() && inlineTranslationI != null) {
							toUse = inlineTranslationI.createInterceptComponentRenderer(toUse);
						}
						return toUse;
					}};
			}};
	}

	/* (non-Javadoc)
	 * @see org.olat.core.gui.control.WindowBackOffice#getWindow()
	 */
	public Window getWindow() {
		return window;
	}

	/* (non-Javadoc)
	 * @see org.olat.core.gui.control.WindowBackOffice#createDevelopmentController(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl)
	 */
	public Controller createDevelopmentController(UserRequest ureq, WindowControl windowControl) {
		DevelopmentController dc = new DevelopmentController(ureq, windowControl,this);
		return dc;
	}

	/* (non-Javadoc)
	 * @see org.olat.core.gui.control.WindowBackOffice#getGlobalSettings()
	 */
	public GlobalSettings getGlobalSettings() {
		return winmgrImpl.getGlobalSettings();
	}

	@Override
	public WindowSettings getWindowSettings() {
		if(settings == null) {
			settings = new WindowSettings();
		}
		return settings;
	}

	@Override
	public void setWindowSettings(WindowSettings settings) {
		this.settings = settings;
	}

	/**
	 * @return
	 */
	public JSAndCSSAdderImpl createJSAndCSSAdder() {
		JSAndCSSAdderImpl jcImpl = new JSAndCSSAdderImpl(this);
		return jcImpl;
	}

	/**
	 * @see org.olat.core.gui.control.WindowBackOffice#sendCommandTo(org.olat.core.gui.control.winmgr.Command)
	 */
	public void sendCommandTo(Command wco) {
		if (ajaxC != null) ajaxC.sendCommandTo(new WindowCommand(this,wco));
	}
	
	public void pushCommands(HttpServletRequest request, HttpServletResponse response) {
		Writer w = null;
		try {
			boolean acceptJson = false;
			for(Enumeration<String> headers=request.getHeaders("Accept"); headers.hasMoreElements(); ) {
				String accept = headers.nextElement();
				if(accept.contains("application/json")) {
					acceptJson = true;
				}
			}
			
			//first set the headers with the content-type
			//and after get the writer with the encoding
			//fixed by the content-type
			if(acceptJson) {
				ServletUtil.setJSONResourceHeaders(response);
				w = response.getWriter();
				ajaxC.pushJSONAndClear(w);
			} else {
				ServletUtil.setStringResourceHeaders(response);
				w = response.getWriter();
				ajaxC.pushResource(w, true);
			}
		} catch (IOException e) {
			log.error("Error pushing commans to the AJAX canal.", e);
		} finally {
			IOUtils.closeQuietly(w);
		}
	}

	/**
	 * @param wrapHTML
	 * @return
	 */
	public MediaResource extractCommands(boolean wrapHTML) {
		return ajaxC.extractMediaResource(wrapHTML);
	}

	/**
	 * @return
	 */
	public InterceptHandler getInterceptHandler() {
		return linkedInterceptHandler;
	}

	/**
	 * @param ureq
	 * @param windowControl
	 * @return the debug controller (not visible on screen, only in debug mode it wraps around each component for dispatching of gui debug info)
	 */
	public Controller createDebugDispatcherController(UserRequest ureq, WindowControl windowControl) {
		guidebugC = new GuiDebugDispatcherController(ureq, windowControl);
		this.debug_interceptHandler  = guidebugC;
		return guidebugC;
	}

	/**
	 * Factory method to create the inline translation tool dispatcher controller.
	 * This implicitly sets the translation controller on the window back office
	 * 
	 * @param ureq
	 * @param windowControl
	 * @return
	 */
	public Controller createInlineTranslationDispatcherController(UserRequest ureq, WindowControl windowControl) {
		if (inlineTranslationC != null) throw new AssertException("Can't set the inline translation dispatcher twice!", null);
		inlineTranslationC = I18nUIFactory.createInlineTranslationIntercepHandlerController(ureq, windowControl);
		this.inlineTranslation_interceptHandler  = inlineTranslationC;
		return inlineTranslationC;
	}

	public Controller createAJAXController(UserRequest ureq) {
		boolean ajaxEnabled = winmgrImpl.isAjaxEnabled();
		ajaxC = new AjaxController(ureq, this, ajaxEnabled, iframeName);
		return ajaxC;
	}

	/* (non-Javadoc)
	 * @see org.olat.core.gui.control.WindowBackOffice#isDebuging()
	 */
	public boolean isDebuging() {
		return Settings.isDebuging();
	}

	public WindowManagerImpl getWinmgrImpl() {
		return winmgrImpl;
	}

	/**
	 * 
	 */
	public void dispose() {
		windowOwner.dispose();
	}

	/**
	 * @param enabled
	 */
	public void setAjaxEnabled(boolean enabled) {
		if (ajaxC != null) ajaxC.setAjaxEnabled(enabled);
	}

	/**
	 * @param enabled
	 */
	public void setHighLightingEnabled(boolean enabled) {
		if (ajaxC != null) ajaxC.setHighLightingEnabled(enabled);
	}

	/**
	 * @param enabled
	 */
	public void setShowJSON(boolean enabled) {
		if (ajaxC != null) ajaxC.setShowJSON(enabled);
	}

	/**
	 * @param refreshInterval
	 */
	public void setRequiredRefreshInterval(int refreshInterval) {
		if (ajaxC != null) ajaxC.setPollPeriod(refreshInterval);
	}

	/**
	 * @param showDebugInfo
	 */
	public void setShowDebugInfo(boolean showDebugInfo) {
		if (guidebugC != null) {
			guidebugC.setShowDebugInfo(showDebugInfo);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.olat.core.gui.control.WindowBackOffice#getWindowManager()
	 */
	public WindowManager getWindowManager() {
		return winmgrImpl;
	}

	
	/**
	 * @return
	 */
	public String getIframeTargetName() {
		return iframeName;
	}

	/* (non-Javadoc)
	 * @see org.olat.core.gui.control.WindowBackOffice#createGuiStack(org.olat.core.gui.components.Component)
	 */
	@Override
	public GuiStack createGuiStack(Component initialComponent) {
		return new GuiStackNiceImpl(this, initialComponent);
	}

	public void fireCycleEvent(Event cycleEvent) {
		for (GenericEventListener gel : cycleListeners) {
			gel.event(cycleEvent);
		}
		if (cycleEvent == Window.AFTER_VALIDATING) {
			// clear the added data for this cycle
			guiMessages.clear();
		}
		
	}

	@Override
	public void addCycleListener(GenericEventListener gel) {
		cycleListeners.add(gel);
	}

	@Override
	public void removeCycleListener(GenericEventListener gel) {
		// Since we use a CopyOnWriteArrayList it is save to remove an event
		// listener even when we are in the fireCycleEvent() method at the same time
		cycleListeners.remove(gel);
	}

	@Override
	public List<ZIndexWrapper> getGuiMessages() {
		return guiMessages;
	}
}
