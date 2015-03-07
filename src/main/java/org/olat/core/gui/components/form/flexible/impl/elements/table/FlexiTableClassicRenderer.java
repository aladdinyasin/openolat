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
package org.olat.core.gui.components.form.flexible.impl.elements.table;


import org.olat.core.commons.persistence.SortKey;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.ComponentRenderer;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.impl.Form;
import org.olat.core.gui.components.form.flexible.impl.FormJSHelper;
import org.olat.core.gui.components.form.flexible.impl.NameValuePair;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.render.RenderResult;
import org.olat.core.gui.render.Renderer;
import org.olat.core.gui.render.StringOutput;
import org.olat.core.gui.render.URLBuilder;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.StringHelper;

/**
 * Render the table as a long HTML table
 * @author Christian Guretzki
 */
class FlexiTableClassicRenderer extends AbstractFlexiTableRenderer implements ComponentRenderer {

	@Override
	public void render(Renderer renderer, StringOutput target, Component source, URLBuilder ubu,
			Translator translator, RenderResult renderResult, String[] args) {
		super.render(renderer, target, source, ubu, translator, renderResult, args);
	}
	
	@Override
	protected void renderHeaders(StringOutput target, FlexiTableComponent ftC, Translator translator) {
		FlexiTableElementImpl ftE = ftC.getFlexiTableElement();
		FlexiTableDataModel<?> dataModel = ftE.getTableDataModel();
		FlexiTableColumnModel columnModel = dataModel.getTableColumnModel();
		      
		target.append("<thead><tr>");

		if(ftE.isMultiSelect()) {
			// render as checkbox icon to minimize used space for header
			String choice = translator.translate("table.header.choice");
			target.append("<th><i class='o_icon o_icon_checkbox_checked o_icon-lg' title=\"").append(choice).append("\"> </i></th>");
		}
		
		int cols = columnModel.getColumnCount();
		for(int i=0; i<cols; i++) {
			FlexiColumnModel fcm = columnModel.getColumnModel(i);
			if(ftE.isColumnModelVisible(fcm)) {
				renderHeader(target, ftC, fcm, translator);
			}
		}
		
		target.append("</tr></thead>");
	}
	
	private void renderHeader(StringOutput sb, FlexiTableComponent ftC, FlexiColumnModel fcm, Translator translator) {
		String header = getHeader(fcm, translator);
		sb.append("<th>");
		// sort is not defined
		if (!fcm.isSortable() || fcm.getSortKey() == null) {
			sb.append(header);	
		} else {
			FlexiTableElementImpl ftE = ftC.getFlexiTableElement();
			
			Boolean asc = null;
			String sortKey = fcm.getSortKey();
			SortKey[] orderBy = ftE.getOrderBy();
			if(orderBy != null && orderBy.length > 0) {
				for(int i=orderBy.length; i-->0; ) {
					if(sortKey.equals(orderBy[i].getKey())) {
						asc = new Boolean(orderBy[i].isAsc());
					}
				}
			}

			Form theForm = ftE.getRootForm();
			if(asc == null) {
				sb.append("<a class='o_orderby' href=\"javascript:")
				  .append(FormJSHelper.getXHRFnCallFor(theForm, ftC.getFormDispatchId(), 1, new NameValuePair("sort", sortKey), new NameValuePair("asc", "asc")))
				  .append("\" onclick=\"return o2cl();\">");
			} else if(asc.booleanValue()) {
				sb.append("<a class='o_orderby o_orderby_asc' href=\"javascript:")
				  .append(FormJSHelper.getXHRFnCallFor(theForm, ftC.getFormDispatchId(), 1, new NameValuePair("sort", sortKey), new NameValuePair("asc", "desc")))
				  .append("\" onclick=\"return o2cl();\">");
			} else {
				sb.append("<a class='o_orderby o_orderby_desc' href=\"javascript:")
				  .append(FormJSHelper.getXHRFnCallFor(theForm, ftC.getFormDispatchId(), 1, new NameValuePair("sort", sortKey), new NameValuePair("asc", "asc")))
				  .append("\" onclick=\"return o2cl();\">");
			}
			sb.append(header).append("</a>");
		}
		sb.append("</th>");
	}
	
	private String getHeader(FlexiColumnModel fcm, Translator translator) {
		String header;
		if(StringHelper.containsNonWhitespace(fcm.getHeaderLabel())) {
			header = fcm.getHeaderLabel();
		} else {
			header = translator.translate(fcm.getHeaderKey());
		}
		return header;
	}
	
	@Override
	protected void renderRow(Renderer renderer, StringOutput target, FlexiTableComponent ftC, String rowIdPrefix,
			int row, URLBuilder ubu, Translator translator, RenderResult renderResult) {

		FlexiTableElementImpl ftE = ftC.getFlexiTableElement();
		FlexiTableColumnModel columnsModel = ftE.getTableDataModel().getTableColumnModel();
		int numOfCols = columnsModel.getColumnCount();
		
		// use alternating css class
		int numOfColumns = 0;
		target.append("<tr id='").append(rowIdPrefix).append(row)
				  .append("'>");
				
		if(ftE.isMultiSelect()) {
			target.append("<td>")
			      .append("<input type='checkbox' name='tb_ms' value='").append(rowIdPrefix).append(row).append("'");
			if(ftE.isMultiSelectedIndex(row)) {
				target.append(" checked='checked'");
			}   
			target.append("/></td>");
		}
				
		for (int j = 0; j<numOfCols; j++) {
			FlexiColumnModel fcm = columnsModel.getColumnModel(j);
			if(ftE.isColumnModelVisible(fcm)) {
				renderCell(renderer, target, ftC, fcm, row, ubu, translator, renderResult);
				numOfColumns++;
			}
		}
		target.append("</tr>");
		if(ftE.isDetailsExpended(row)) {
			target.append("<tr id='").append(rowIdPrefix).append(row)
			  .append("_details' class='o_table_row_details'>");
			
			VelocityContainer container = ftE.getDetailsRenderer();
			Object rowObject = ftE.getTableDataModel().getObject(row);
			container.contextPut("row", rowObject);

			FlexiTableComponentDelegate cmpDelegate = ftE.getComponentDelegate();
			if(cmpDelegate != null) {
				Iterable<Component> cmps = cmpDelegate.getComponents(row, rowObject);
				if(cmps != null) {
					for(Component cmp:cmps) {
						container.put(cmp.getComponentName(), cmp);
					}
				}
			}
			
			if(ftE.isMultiSelect()) {
				target.append("<td></td>");
			}
			target.append("<td colspan='").append(numOfColumns).append("'>");

			container.getHTMLRendererSingleton().render(renderer, target, container, ubu, translator, renderResult, null);
			container.contextRemove("row");
			
			target.append("</td></tr>");
		}
	}

	private void renderCell(Renderer renderer, StringOutput target, FlexiTableComponent ftC, FlexiColumnModel fcm,
			int row, URLBuilder ubu, Translator translator, RenderResult renderResult) {

		FlexiTableElementImpl ftE = ftC.getFlexiTableElement();
		FlexiTableDataModel<?> dataModel = ftE.getTableDataModel();

		int alignment = fcm.getAlignment();
		String cssClass = (alignment == FlexiColumnModel.ALIGNMENT_LEFT ? "text-left" : (alignment == FlexiColumnModel.ALIGNMENT_RIGHT ? "text-right" : "text-center"));

		target.append("<td class=\"").append(cssClass).append(" ")
		  .append("o_dnd_label", ftE.getColumnIndexForDragAndDropLabel() == fcm.getColumnIndex())
		  .append("\">");
		
		int columnIndex = fcm.getColumnIndex();
		Object cellValue = columnIndex >= 0 ? 
				dataModel.getValueAt(row, columnIndex) : null;
		if (cellValue instanceof FormItem) {
			FormItem formItem = (FormItem)cellValue;
			formItem.setTranslator(translator);
			if(ftE.getRootForm() != formItem.getRootForm()) {
				formItem.setRootForm(ftE.getRootForm());
			}
			ftE.addFormItem(formItem);
			if(formItem.isVisible()) {
				formItem.getComponent().getHTMLRendererSingleton().render(renderer, target, formItem.getComponent(),
					ubu, translator, renderResult, null);
			}
		} else if(cellValue instanceof Component) {
			Component cmp = (Component)cellValue;
			cmp.setTranslator(translator);
			if(cmp.isVisible()) {
				cmp.getHTMLRendererSingleton().render(renderer, target, cmp,
					ubu, translator, renderResult, null);
			}
		} else {
			fcm.getCellRenderer().render(renderer, target, cellValue, row, ftC, ubu, translator);
		}
		target.append("</td>");
	}
}