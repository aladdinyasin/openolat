package de.unileipzig.shibboleth;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.WindowSettings;
import org.olat.core.gui.Windows;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.Window;
import org.olat.core.gui.components.htmlheader.jscss.CustomCSS;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.DefaultChiefController;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.navigation.SiteInstance;
import org.olat.core.util.WebappHelper;

/**
 * This controller can show an error message in a stand alone html page
 * @author Klemens Schölhorn
 */
public class SimpleShibbolethErrorController extends DefaultChiefController {
	/**
	 * @see SimpleShibbolethErrorController
	 * @param ureq
	 * @param message The message to display
	 * @param detail More details (can be null)
	 */
	public SimpleShibbolethErrorController(UserRequest ureq, String message, String detail) {
		Window window = Windows.getWindows(ureq).getWindowManager().createWindowBackOffice("shibbolethError", this, new WindowSettings()).getWindow();

		VelocityContainer mainVC = new VelocityContainer("shibbolethError", SimpleShibbolethErrorController.class, "errorWindow", null, this);
		mainVC.contextPut("cssURL", window.getGuiTheme().getBaseURI() + "theme.css");
		String backlink = WebappHelper.getServletContextPath();
		if(backlink.isEmpty()) {
			backlink = "/";
		}
		mainVC.contextPut("backLink", backlink);
		mainVC.contextPut("message", message);
		if(detail != null) {
			mainVC.contextPut("detail", detail);
		}
		try {
			// UrlEncoder encodes spaces as +, so we have to replace it back (URIBuilder does not support opaque URI with query)
			String body = URLEncoder.encode("\n\n" + message + "\n" + (detail == null ? "" : detail), "UTF-8").replace("+", "%20");
			mainVC.contextPut("mailto", WebappHelper.getMailConfig("mailError") + "?subject=Shibboleth%20error&body=" + body);
		} catch (UnsupportedEncodingException e) {}

		window.setContentPane(mainVC);
		setWindow(window);
	}

	@Override
	public void event(UserRequest ureq, Component source, Event event) {
		// nothing to do
	}

	@Override
	protected void doDispose() {
		// nothing to dispose
	}

	@Override
	public void addBodyCssClass(String cssClass) {
		// not supported
	}

	@Override
	public void removeBodyCssClass(String cssClass) {
		// not supported
	}

	@Override
	public void addCurrentCustomCSSToView(CustomCSS customCSS) {
		// not supported
	}

	@Override
	public void removeCurrentCustomCSSFromView() {
		// not supported
	}

	@Override
	public boolean hasStaticSite(Class<? extends SiteInstance> type) {
		return false;
	}
}
