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
package org.olat.modules.lecture.ui;

import java.util.List;

import org.olat.core.commons.persistence.SortKey;
import org.olat.core.gui.components.form.flexible.impl.elements.table.DefaultFlexiTableDataModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiSortableColumnDef;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.SortableFlexiTableDataModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.SortableFlexiTableModelDelegate;
import org.olat.modules.lecture.model.LectureBlockIdentityStatistics;

/**
 * 
 * Initial date: 16 juin 2017<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class LecturesListDataModel extends DefaultFlexiTableDataModel<LectureBlockIdentityStatistics>
implements SortableFlexiTableDataModel<LectureBlockIdentityStatistics>{
	
	public LecturesListDataModel(FlexiTableColumnModel columnModel) {
		super(columnModel);
	}

	@Override
	public void sort(SortKey orderBy) {
		SortableFlexiTableModelDelegate<LectureBlockIdentityStatistics> sorter
			= new SortableFlexiTableModelDelegate<>(orderBy, this, null);
		List<LectureBlockIdentityStatistics> views = sorter.sort();
		super.setObjects(views);
	}

	@Override
	public Object getValueAt(int row, int col) {
		LectureBlockIdentityStatistics stats = getObject(row);
		return getValueAt(stats, col);
	}

	@Override
	public Object getValueAt(LectureBlockIdentityStatistics row, int col) {
		if(col >= 0 && col < StatsCols.values().length) {
			switch(StatsCols.values()[col]) {
				case id: return row.getIdentityKey();
				case entry: return row.getDisplayName();
				case plannedLectures: return positive(row.getTotalPersonalPlannedLectures());
				case attendedLectures: return positive(row.getTotalAttendedLectures());
				case absentLectures: return positive(row.getTotalAbsentLectures());
				case authorizedAbsenceLectures: return positive(row.getTotalAuthorizedAbsentLectures());
			}
		}
		
		int propPos = col - LecturesListController.USER_PROPS_OFFSET;
		return row.getIdentityProp(propPos);
	}

	private static final long positive(long pos) {
		return pos < 0 ? 0 : pos;
	}
	
	@Override
	public DefaultFlexiTableDataModel<LectureBlockIdentityStatistics> createCopyWithEmptyList() {
		return new LecturesListDataModel(getTableColumnModel());
	}
	
	public enum StatsCols implements FlexiSortableColumnDef {
		id("table.header.id"),
		entry("table.header.entry"),
		plannedLectures("table.header.planned.lectures"),
		attendedLectures("table.header.attended.lectures"),
		absentLectures("table.header.absent.lectures"),
		authorizedAbsenceLectures("table.header.authorized.absence")
		;
		
		private final String i18nKey;
		
		private StatsCols(String i18nKey) {
			this.i18nKey = i18nKey;
		}
		
		@Override
		public String i18nHeaderKey() {
			return i18nKey;
		}

		@Override
		public boolean sortable() {
			return true;
		}

		@Override
		public String sortKey() {
			return name();
		}
	}
}
