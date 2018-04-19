package org.olat.core.commons.services.csp.ui;

import java.util.Collections;
import java.util.List;

import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.persistence.DefaultResultInfos;
import org.olat.core.commons.persistence.ResultInfos;
import org.olat.core.commons.persistence.SortKey;
import org.olat.core.commons.services.csp.CSPLog;
import org.olat.core.commons.services.csp.CSPManager;
import org.olat.core.gui.components.form.flexible.elements.FlexiTableFilter;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableDataSourceDelegate;

/**
 * 
 * Initial date: 19 avr. 2018<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class CSPLogDataSource implements FlexiTableDataSourceDelegate<CSPLog> {

	private Integer count;
	private CSPManager cspManager;
	
	public CSPLogDataSource() {
		cspManager = CoreSpringFactory.getImpl(CSPManager.class);
	}
	
	public void reset() {
		count = null;
	}
	
	@Override
	public int getRowCount() {
		if(count == null) {
			count = cspManager.countLog();
		}
		return count.intValue();
	}

	@Override
	public List<CSPLog> reload(List<CSPLog> rows) {
		return Collections.emptyList();
	}

	@Override
	public ResultInfos<CSPLog> getRows(String query, List<FlexiTableFilter> filters,
			List<String> condQueries, int firstResult, int maxResults, SortKey... orderBy) {
		
		List<CSPLog> rows = cspManager.getLog(firstResult, maxResults);
		ResultInfos<CSPLog> results = new DefaultResultInfos<>(firstResult + rows.size(), -1, rows);
		if(firstResult == 0 && rows.size() < maxResults) {
			count = Integer.valueOf(rows.size());
		}
		return results;
	}
}
