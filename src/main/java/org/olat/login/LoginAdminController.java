package org.olat.login;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 14.01.2015<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class LoginAdminController extends FormBasicController {
	
	private MultipleSelectionElement guestLoginEl, guestLinkEl, invitationLoginEl;
	
	private static final String[] keys = new String[]{ "on" };
	private final String[] values;
	
	@Autowired
	private LoginModule loginModule;
	
	public LoginAdminController(UserRequest ureq, WindowControl wControl) {
		super(ureq, wControl);
		
		values = new String[]{ translate("enabled") };
		
		initForm(ureq);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		setFormTitle("login.admin.title");
		
		guestLoginEl = uifactory.addCheckboxesHorizontal("guest.login", "guest.login", formLayout, keys, values);
		guestLoginEl.select(keys[0], loginModule.isGuestLoginEnabled());
		guestLoginEl.addActionListener(FormEvent.ONCHANGE);
		
		guestLinkEl = uifactory.addCheckboxesHorizontal("guest.login.links", "guest.login.links", formLayout, keys, values);
		guestLinkEl.select(keys[0], loginModule.isGuestLoginLinksEnabled());
		guestLinkEl.addActionListener(FormEvent.ONCHANGE);
		
		invitationLoginEl = uifactory.addCheckboxesHorizontal("invitation.login", "invitation.login", formLayout, keys, values);
		invitationLoginEl.select(keys[0], loginModule.isInvitationEnabled());
		invitationLoginEl.addActionListener(FormEvent.ONCHANGE);
	}
	
	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(guestLoginEl == source) {
			boolean enabled = guestLoginEl.isAtLeastSelected(1);
			loginModule.setGuestLoginEnabled(enabled);
		} else if(guestLinkEl == source) {
			boolean enabled = guestLinkEl.isAtLeastSelected(1);
			loginModule.setGuestLoginLinksEnabled(enabled);
		} else if(invitationLoginEl == source) {
			boolean enabled = invitationLoginEl.isAtLeastSelected(1);
			loginModule.setInvitationEnabled(enabled);
		}
		super.formInnerEvent(ureq, source, event);
	}

	@Override
	protected void formOK(UserRequest ureq) {
		//
	}
}