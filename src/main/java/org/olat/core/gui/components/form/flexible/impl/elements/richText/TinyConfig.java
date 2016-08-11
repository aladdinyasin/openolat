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
package org.olat.core.gui.components.form.flexible.impl.elements.richText;


/**
 * 
 * Initial date: 16.10.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class TinyConfig {
	protected static final TinyConfig editorConfig;
	protected static final TinyConfig editorFullConfig;
	protected static final TinyConfig editorCompactConfig;
	protected static final TinyConfig fileEditorConfig;
	protected static final TinyConfig minimalisticConfig;

	//min profile
	static {
		String plugins =  "colorpicker,textcolor,hr,olatsmileys,paste,link,quotespliter,tabfocus,visualchars,noneditable";
		String toolbar1 = "undo redo | bold italic underline | alignjustify alignright aligncenter alignleft | forecolor backcolor | bullist numlist | link unlink olatsmileys";
		minimalisticConfig = new TinyConfig(plugins, null, toolbar1);
	}
	//standard profile
	static {
		String plugins =   "contextmenu,colorpicker,textcolor,hr,olatsmileys,paste,link,charmap,quotespliter,olatmatheditor,tabfocus,visualchars,visualblocks,noneditable";
		String[] menu = {
				"edit: {title: 'Edit', items: 'undo redo | cut copy paste pastetext | selectall searchreplace'}",
			  "insert: {title: 'Insert', items: 'olatmovieviewer media image link | olatmatheditor hr charmap insertdatetime olatsmileys'}",
			  "view: {title: 'View', items: 'visualblocks visualchars | preview fullscreen'}",
			  "format: {title: 'Format', items: 'bold italic underline strikethrough superscript subscript | removeformat'}"
		};
		String tools1 = "bold italic underline | alignjustify alignright aligncenter alignleft | formatselect | fontselect fontsizeselect | forecolor backcolor | bullist numlist indent outdent | olatqtifibtext olatqtifibnumerical olatmovieviewer image charmap olatsmileys hr link";
		editorConfig = new TinyConfig(plugins, menu, tools1);
	}
	//compact profile
	static {
		String plugins =   "contextmenu,colorpicker,textcolor,hr,charmap,image,insertdatetime,table,visualchars,visualblocks,noneditable,olatsmileys,paste,link,quotespliter,olatmatheditor,olatmovieviewer,tabfocus,visualchars,visualblocks,noneditable,media";
		String[] menu = {
				"edit: {title: 'Edit', items: 'undo redo | cut copy paste pastetext | selectall searchreplace'}",
				"insert: {title: 'Insert', items: 'olatmovieviewer media image link | olatmatheditor hr charmap insertdatetime olatsmileys'}",
				"view: {title: 'View', items: 'visualblocks visualchars visualaid | preview fullscreen'}",
				"format: {title: 'Format', items: 'bold italic underline strikethrough superscript subscript | formats | removeformat'}",
				"table: {title: 'Table', items: 'inserttable tableprops deletetable | cell row column'}"
		};
		String tools1 = "bold italic underline | alignjustify alignright aligncenter alignleft  | styleselect | forecolor backcolor | bullist numlist indent outdent | olatqtifibtext olatqtifibnumerical olatmovieviewer image charmap hr link";
		editorCompactConfig = new TinyConfig(plugins, menu, tools1);
	}
	//full profile
	static {
		String plugins =   "contextmenu,colorpicker,textcolor,hr,olatsmileys,paste,link,charmap,quotespliter,olatmatheditor,tabfocus,visualchars,visualblocks,noneditable,table";
		String[] menu = {
				"edit: {title: 'Edit', items: 'undo redo | cut copy paste pastetext | selectall searchreplace'}",
			  "insert: {title: 'Insert', items: 'olatmovieviewer media image link | olatmatheditor hr charmap insertdatetime olatsmileys'}",
			  "view: {title: 'View', items: 'visualblocks visualchars visualaid | preview fullscreen'}",
			  "format: {title: 'Format', items: 'bold italic underline strikethrough superscript subscript | removeformat'}",
			  "table: {title: 'Table', items: 'inserttable tableprops deletetable | cell row column'}"
		};
		String tools1 = "bold italic underline | alignjustify alignright aligncenter alignleft | formatselect | fontselect fontsizeselect | forecolor backcolor | bullist numlist indent outdent | olatqtifibtext olatqtifibnumerical olatmovieviewer image charmap olatsmileys hr link";
		editorFullConfig = new TinyConfig(plugins, menu, tools1);
	}
	//file profile
	static {
		String plugins =   "colorpicker,textcolor,hr,link,charmap,image,importcss,insertdatetime,code,table,tabfocus,visualchars,visualblocks,noneditable,fullscreen,contextmenu,anchor,olatmatheditor,olatmovieviewer,searchreplace,olatsmileys,paste,media";
		String[] menu = {
				"edit: {title: 'Edit', items: 'undo redo | cut copy paste pastetext | selectall searchreplace'}",
			  "insert: {title: 'Insert', items: 'olatmovieviewer media image link | olatmatheditor hr charmap anchor insertdatetime olatsmileys'}",
			  "view: {title: 'View', items: 'visualblocks visualchars visualaid | preview fullscreen'}",
			  "format: {title: 'Format', items: 'bold italic underline strikethrough superscript subscript | formats | removeformat'}",
			  "table: {title: 'Table', items: 'inserttable tableprops deletetable | cell row column'}"
		};
		String tools1 = "bold italic underline | styleselect | fontselect fontsizeselect | forecolor backcolor | bullist numlist indent outdent | olatqtifibtext olatqtifibnumerical olatmovieviewer image charmap olatsmileys hr link | code";
		fileEditorConfig = new TinyConfig(plugins, menu, tools1);
	}

	private final String plugins;
	
	private final String[] menu;
	private final String tool1;
	
	public TinyConfig(String plugins, String[] menu, String tool1) {
		this.plugins = plugins;
		this.menu = menu;
		this.tool1 = tool1;
	}
	
	public String getPlugins() {
		return plugins;
	}
	
	public boolean hasMenu() {
		return menu != null && menu.length > 0;
	}
	
	public String[] getMenu() {
		return menu == null ? new String[0] : menu;
	}
	
	public String getTool1() {
		return tool1;
	}
	
	public boolean isMathEnabled() {
		return plugins != null && plugins.indexOf("olatmatheditor") >= 0;
	}
	
	public TinyConfig enableCode() {
		return enableFeature("code");
	}
	
	public TinyConfig enableImageAndMedia() {
		return enableFeature("image")
				.enableFeature("media")
				.enableFeature("olatmovieviewer");
	}
	
	public TinyConfig enableQTITools(boolean textEntry, boolean numericalInput) {
		TinyConfig config = enableFeature("olatqti");
		if(!textEntry) {
			config = config.disableButtons("olatqtifibtext");
		}
		if(!numericalInput) {
			config = config.disableButtons("olatqtifibnumerical");
		}
		return config;
	}
	
	/**
	 * Disable image, media and movie plugins.
	 * @return
	 */
	public TinyConfig disableImageAndMedia() {
		return disableFeature("image")
				.disableFeature("media")
				.disableFeature("olatmovieviewer");
	}
	
	public TinyConfig disableMenuAndMenuBar() {
		return new TinyConfig(plugins, new String[0], null);
	}
	
	/**
	 * Remove media + olatmovie
	 * @return
	 */
	public TinyConfig disableMedia() {
		return disableFeature("media").disableFeature("olatmovieviewer");
	}
	
	public TinyConfig disableMathEditor() {
		return disableFeature("olatmatheditor");
	}
	
	public TinyConfig disableButtons(String button) {
		TinyConfig config = this;
		if(tool1.contains(button)) {
			String clonedTools =  tool1.replace(button, "");
			config = new TinyConfig(plugins, menu, clonedTools);
		}
		return config;
	}
	
	public TinyConfig enableFeature(String feature) {
		if(plugins.contains(feature)) {
			return this;
		} else {
			String clonedPlugins =  plugins + "," + feature;
			return new TinyConfig(clonedPlugins, menu, tool1);
		}
	}
	
	private TinyConfig disableFeature(String feature) {
		if(plugins.contains(feature)) {
			String clonedPlugins =  plugins.replace("," + feature, "");
			return new TinyConfig(clonedPlugins, menu, tool1);
		} else {
			return this;
		}
	}
	
	@Override
	public TinyConfig clone() {
		return new TinyConfig(plugins, menu, tool1);
	}
}
