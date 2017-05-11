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
package org.olat.core.gui.components.dropdown;

import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.DefaultComponentRenderer;
import org.olat.core.gui.components.dropdown.Dropdown.Spacer;
import org.olat.core.gui.render.RenderResult;
import org.olat.core.gui.render.Renderer;
import org.olat.core.gui.render.StringOutput;
import org.olat.core.gui.render.URLBuilder;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.StringHelper;

/**
 * 
 * Initial date: 25.04.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class DropdownRenderer extends DefaultComponentRenderer {

	@Override
	public void render(Renderer renderer, StringOutput sb, Component source,
			URLBuilder ubu, Translator translator, RenderResult renderResult,
			String[] args) {

		Dropdown dropdown = (Dropdown)source;
		sb.append("<div class='btn-group'>", dropdown.isEmbbeded());
		
		Iterable<Component> components = dropdown.getComponents();
		if(dropdown.isButton()) {
			sb.append("<button type='button' class='btn btn-default dropdown-toggle");
		} else {
			sb.append("<a href='#' class='dropdown-toggle");
		}
		if(StringHelper.containsNonWhitespace(dropdown.getElementCssClass())) {
			sb.append(" ").append(dropdown.getElementCssClass());
		}
		sb.append("' data-toggle='dropdown'>");
		if(StringHelper.containsNonWhitespace(dropdown.getIconCSS())) {
			sb.append("<i class='").append(dropdown.getIconCSS()).append("'>&nbsp;</i>");
		}
		String i18nKey = dropdown.getI18nKey();
		if(StringHelper.containsNonWhitespace(i18nKey)) {
			String label;
			if(dropdown.isTranslated()) {
				label = i18nKey;
			} else {
				label = dropdown.getTranslator().translate(dropdown.getI18nKey());
			}
			sb.append("<span>").append(label).append("</span>");
		}
		sb.append(" <i class='o_icon o_icon_caret'> </i>");
		
		if(dropdown.isButton()) {
			sb.append("</button>");
		} else {
			sb.append("</a>");
		}
		sb.append("<ul class='dropdown-menu");
		if(StringHelper.containsNonWhitespace(dropdown.getElementCssClass())) {
			sb.append(" ").append(dropdown.getElementCssClass());
		}
		if(dropdown.getOrientation() == DropdownOrientation.right) {
			sb.append(" dropdown-menu-right");
		}
		
		sb.append("' role='menu'>");
		
		boolean wantSpacer = false;
		for(Component component:components) {
			if(component instanceof Spacer) {
				wantSpacer = true;
			} else if(component.isVisible()) {
				if(wantSpacer) {
					sb.append("<li class='divider'></li>");
					wantSpacer = false;
				}
				
				if(component.isEnabled()) {
					sb.append("<li>");
				} else {
					sb.append("<li class='disabled'>");
				}
				renderer.render(component, sb, args);
				sb.append("</li>");
			}
		}
		sb.append("</ul>").append("</div>", dropdown.isEmbbeded());
	}
}
