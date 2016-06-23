/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.listener.formaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

import com.centurylink.mdw.common.constant.FormConstants;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.services.UserException;
import com.qwest.mbeng.MbengException;
import com.qwest.mbeng.MbengNode;

/**
 * This class provides the base for table paginator.
 *
 * The class is also used for supporting sorting of scrolled tables.
 *
 * Typically you will need to override the following methods:
 * <ul>
 *   <li>getPageSize: get the number of rows on a page</li>
 *   <li>getNumberOfRows: get the total number of rows in the table</li>
 *   <li>fetchTablePage: fetch rows from database or other sources</li>
 *   <li>insertTableRow: insert a new row to database or other sources</li>
 *   <li>updateTableRow: update an existing row in the database or other sources</li>
 *   <li>deleteTableRow: delete an existing row in the database or other sources</li>
 * </ul>
 *
 *
 */
public class TablePaginator extends FormActionBase {

	/**
	 * You should not typically override this method.
	 * If you do override it, such as the case you would like to
	 * have your paginator to respond to other types of user actions
	 * beside pagination related, please call <code>super.handleAction()</code>
	 * for actions <code>paging</code>, <code>insertrow</code>, <code>updaterow</code>
	 * and <code>deleterow</code>.
	 *
	 * The action sent by client has the paginator as command class name, and following parameters:
	 *   - action: paging, insertrow, deleterow, updaterow
	 * For paging:
	 *   - table: table data path
	 *   - meta: table meta data path
	 *   - topage:	S (sorting only on the same page in paginated style),
	 *   			R (sorting for non-paginated styles such as scrolled or simple table)
	 *   			A (get all rows - same as R but reload data)
	 *   			F (go to first page)
	 *   			P (go to previous page)
	 *   			N (go to next page)
	 *   			L (go to last page)
	 *   - pagesize: number rows on a page (used for paginated table only)
	 *   - sorton: name of the column to be sorted on;
	 *   		 null implies take the current sort option; if still null, no sorting
	 * For insertrow/deleterow/updaterow
	 *   - row: row index
	 *   - meta: table meta data path (for getting current page info)
	 *   - topage: S or R, for determine if it is paginated or not
	 *
	 * @param datadoc the data document received from the browser.
	 * @return form data document to be sent back to the browser
	 */
	public FormDataDocument handleAction(FormDataDocument datadoc, Map<String, String> params) {
		try {
			String action = params.get(FormConstants.URLARG_ACTION);
			if (action==null || action.equals("paging"))
				return this.paging_and_sorting(datadoc, params);
			else return this.update_table(action, datadoc, params);
		} catch (UserException e) {
			datadoc.addError(e.getMessage());
		} catch (Exception e) {
			logger.severeException("Failed to execute form aciton", e);
			datadoc.addError(e.getMessage());
		}
		return datadoc;
	}

	private FormDataDocument paging_and_sorting(FormDataDocument datadoc,
			Map<String, String> params)
			throws Exception {
		String tablepath = params.get(FormConstants.URLARG_TABLE);
		String metaname = params.get(FormConstants.URLARG_META);
		String sortOn = params.get(FormConstants.URLARG_SORTON);
		String topage = params.get(FormConstants.URLARG_TOPAGE);
		String pageSize = params.get(FormConstants.URLARG_PAGESIZE);
		MbengNode metanode = metaname==null?null:datadoc.setSubform(null, metaname);
		String v = metanode==null?null:datadoc.getValue(metanode, "start_row");
		boolean wholeTable = "A".equalsIgnoreCase(topage)
				|| "R".equalsIgnoreCase(topage);
		if (wholeTable) {
			if (sortOn==null && metanode!=null) sortOn = datadoc.getValue(metanode,"sort_on");
			if ("A".equalsIgnoreCase(topage)) {
				// get all data - for testing simple/scrolled table only
				populate_pagedata(datadoc, tablepath, 1, -1, -1, sortOn);
			} else {		// "R"
				// sorting for scrolled/simple table
				MbengNode tablenode = datadoc.setTable(null, tablepath, false);
				sortTable(datadoc, tablenode, sortOn);
				datadoc.setValue(metanode, "sort_on", sortOn);
			}
		} else {
			int startrow = v==null?1:Integer.parseInt(v);
			int pagesize = pageSize==null?this.getPageSize(datadoc):Integer.parseInt(pageSize);
			int totalrows = this.getNumberOfRows(datadoc);
			if (sortOn==null && metanode!=null) sortOn = datadoc.getValue(metanode,"sort_on");
			if ("F".equalsIgnoreCase(topage)) {
				startrow = 1;
			} else if ("N".equalsIgnoreCase(topage)) {
				startrow += pagesize;
			} else if ("P".equalsIgnoreCase(topage)) {
				startrow -= pagesize;
				if (startrow<1) startrow = 1;
			} else if ("L".equalsIgnoreCase(topage)) {
				startrow = totalrows - pagesize + 1;
			} // else "S" - sorting only for paginated style
			int nrows = pagesize;
			if (startrow+nrows-1>totalrows) nrows=totalrows-startrow+1;
			populate_pagedata(datadoc, tablepath, startrow, nrows, totalrows, sortOn);
			setMetaInfo(datadoc, metanode, startrow, pagesize, totalrows, sortOn);
		}
		return datadoc;
	}

	private void populate_pagedata(FormDataDocument datadoc, String tablepath,
			int startrow, int nrows, int totalrows, String sortOn) throws Exception {
		MbengNode tablenode = datadoc.setTable(null, tablepath, true);
		datadoc.removeChildren(tablenode);
		boolean descending;
		String sortOnField;
		if (sortOn!=null && sortOn.startsWith("-")) {
			descending = true;
			sortOnField = sortOn.substring(1);
		} else {
			descending = false;
			sortOnField = sortOn;
		}
		fetchTablePage(datadoc, tablenode, startrow, nrows, sortOnField, descending);
	}

	/**
	 * This method sets meta information such as start row number
	 * and total number of rows. Typically you should not need to override or call
	 * this method.
	 * @param datadoc the data document to be sent back to browser.
	 * @param metanode
	 * @param startrow
	 * @param nrows
	 * @param totalrows
	 * @param sortOn
	 * @throws MbengException
	 */
	protected void setMetaInfo(FormDataDocument datadoc, MbengNode metanode,
			int startrow, int pagesize, int totalrows, String sortOn) throws MbengException {
		datadoc.setValue(metanode, "start_row", Integer.toString(startrow));
		datadoc.setValue(metanode, "total_rows", Integer.toString(totalrows));
		datadoc.setValue(metanode, "page_size", Integer.toString(pagesize));
		datadoc.setValue(metanode, "sort_on", sortOn);
	}

	private class SortEntry {
		String key;
		MbengNode row;
	}

	/**
	 * Sort the table in memory. This is not used for paginated tables,
	 * rather for scrolled tables where all rows are loaded in memory.
	 *
	 * @param datadoc the data document to be sent back to browser.
	 * @param tablenode the node in the data document representing the table.
	 *  	when the method is called, its rows are cleared already (i.e.
	 *  	it does not contain any row)
	 * @param sortOn
	 * @throws MbengException
	 */
	protected void sortTable(FormDataDocument datadoc, MbengNode tablenode, String sortOn)
			throws MbengException {
		ArrayList<SortEntry> list = new ArrayList<SortEntry>();
		final boolean descending;
		String sortOnField;
		if (sortOn!=null && sortOn.startsWith("-")) {
			descending = true;
			sortOnField = sortOn.substring(1);
		} else {
			descending = false;
			sortOnField = sortOn;
		}
		for (MbengNode row=tablenode.getFirstChild(); row!=null; row=row.getNextSibling()) {
			SortEntry entry = new SortEntry();
			entry.row = row;
			entry.key = datadoc.getValue(row, sortOnField);
			if (entry.key==null) entry.key = "";
			list.add(entry);
		}
		SortEntry[] array = list.toArray(new SortEntry[list.size()]);
		Arrays.sort(array, new Comparator<SortEntry>() {
			public int compare(SortEntry o1, SortEntry o2) {
				if (descending) return o2.key.compareTo(o1.key);
				else return o1.key.compareTo(o2.key);
			}
		});
		datadoc.removeChildren(tablenode);
		for (SortEntry entry : array) {
			tablenode.appendChild(entry.row);
		}
	}


	/**
	 * this is just test data
	 */
	private static String[][] testdata= {
		{"red", "green", "blue"},
		{"rot", "gruen", "blau"},
		{"hong", "lue", "lan"},
		{"rouge", "vert", "bleu"},
		{"gorri", "berde", "urdin"},
		{"rood", "groen", "blauw"},
		{"merah", "hijau", "biru"},
		{"dearg", "glas", "gorm"},
		{"rosso", "verde", "azzurro"},
		{"czerwony", "zielony", "niebieski"},
		{"encarnado", "verde", "azul"},
		{"rosu", "verde", "albastru"},
		{"colorado", "verde", "azul"},
		{"coch", "gwyrdd", "glas"},
		{"bomvu", "luhlaza", "luhlaza"},
		{"kirmizi", "yesil", "mavi"},
		{"sivappu", "pakkai", "nilam"},
		{"Lal", "Hara", "Nila"},
		{"ulaula", "oomaomao", "uliuli"},
		{"kokkino", "prasino", "ble"},
		{"ruga", "verda", "blua"},
		{"punane", "roheline", "sinine"},
		{"sun", "meadow", "sky"},
		{"flower", "leaf", "trunk"},
		{"fire", "wood", "water"}
	};

	private class MyCompare implements Comparator<String[]> {
		int sortOn;
		boolean descending;
		MyCompare(int sortOn, boolean descending) {
			this.sortOn = sortOn;
			this.descending = descending;
		}
		public int compare(String[] c1, String[] c2) {
			if (descending) return c2[sortOn].compareTo(c1[sortOn]);
			else return c1[sortOn].compareTo(c2[sortOn]);
		}
	}

	/**
	 * This is a convenient method to be used to load the first
	 * page of a table.
	 * It invokes fetchTablePage() for the first page and set up meta information.
	 *
	 * @param datadoc the data document to be sent back to browser.
	 * @param tablenode the node in the data document representing the table.
	 *  	when the method is called, its rows are cleared already (i.e.
	 *  	it does not contain any row)
	 * @param metanode
	 * @param sortOn
	 * @param descending
	 * @throws Exception
	 */
	protected void loadFirstPage(FormDataDocument datadoc,
			MbengNode tablenode, MbengNode metanode, String sortOn, boolean descending)
			throws Exception {
		datadoc.removeChildren(tablenode);
		int totalrows = this.getNumberOfRows(datadoc);
		int pagesize = this.getPageSize(datadoc);
		int nrows = pagesize<totalrows?pagesize:totalrows;
		this.fetchTablePage(datadoc, tablenode, 1, nrows, sortOn, descending);
		if (metanode!=null) {
			datadoc.removeChildren(metanode);
			setMetaInfo(datadoc, metanode, 1, pagesize, totalrows,
					sortOn==null?null:descending?("-"+sortOn):sortOn);
		}
	}

	/**
	 *  This method is invoked to generate data for the requested
	 *  page of the table. The default implementation is only
	 *  for testing data and you must override
	 *  this method in your paginator (a subclass of this),
	 *  and the code here can serve as a sample.
	 *
	 *  Basically, you will need to call the addRow method of the FormDataDocument for each row in the table,
	 *  then call FormDataDocument.setCell for each cell to set the cells in the row.
	 *
	 *  If the paginator is used for sorting of scrolled tables, you can
	 *  simply ignore the argument <code>start_row</code> and <code>nrows</code>
	 *
	 *  @param datadoc the data document to be sent back to browser.
	 *  @param tablenode the node in the data document representing the table.
	 *  	when the method is called, its rows are cleared already (i.e.
	 *  	it does not contain any row)
	 *  @param start_row the starting row index (counting from 1) of
	 *  	the page to be loaded.
	 *  @param nrows number of rows to fetch. When it is -1, fetch all rows in the table
	 *  	(this is for non-paginated table styles). When it is paginated,
	 *  	the caller already calculated
	 *      the exact number of rows needed with the consideration of
	 *      partial pages. You only need to ensure {@link #getNumberOfRows}
	 *      returns the correct number of rows.
	 *  @param sortOn the column name on which the table rows are sorted.
	 *  	It is null if no specific order is requested.
	 *  @param descending when sortOn is not null, this indicates whether
	 *  	the order is descending (true) or ascending (false).
	 *
	 */
	protected void fetchTablePage(FormDataDocument datadoc, MbengNode tablenode,
			int start_row, int nrows, String sortOn, boolean descending) throws Exception {
		if ("RED".equals(sortOn)) {
			Arrays.sort(testdata, new MyCompare(0,descending));
		} else if ("GREEN".equals(sortOn)) {
			Arrays.sort(testdata, new MyCompare(1,descending));
		} else if ("BLUE".equals(sortOn)) {
			Arrays.sort(testdata, new MyCompare(2,descending));
		}
		if (nrows<0) {
			for (int i=0; i<testdata.length; i++) {
				MbengNode row = datadoc.addRow(tablenode);
				datadoc.setCell(row, "RED", testdata[start_row-1+i][0]);
				datadoc.setCell(row, "GREEN", testdata[start_row-1+i][1]);
				datadoc.setCell(row, "BLUE", testdata[start_row-1+i][2]);
			}
		} else {
			for (int i=0; i<nrows; i++) {
				MbengNode row = datadoc.addRow(tablenode);
				datadoc.setCell(row, "RED", testdata[start_row-1+i][0]);
				datadoc.setCell(row, "GREEN", testdata[start_row-1+i][1]);
				datadoc.setCell(row, "BLUE", testdata[start_row-1+i][2]);
			}
		}
	}

	/**
	 * You must need to override this method to return the page size
	 * (number of rows in a page) for paginated style tables.
	 * It is not used for scrolled style tables.
	 *
	 * The default method returns 6 and is for testing purpose only.
	 *
	 * @param datadoc passed in from the event handler, may be used
	 * 		to obtain information such as database table name
	 * @return number of rows in a page.
	 */
	protected int getPageSize(FormDataDocument datadoc) {
		return 6;
	}

	/**
	 * You must override the method to return total number of rows
	 * in the data table for paginated table style.
	 * It is not used for scrolled style tables.
	 * @param datadoc passed in from the event handler, may be used
	 * 		to obtain information such as database table name
	 * @return Total number of rows in the table.
	 */
	protected int getNumberOfRows(FormDataDocument datadoc) {
		return testdata.length;
	}

	/**
	 * You must override this method in order to support insertion
	 * of a new row, and perform real insertion in the persistent storage.
	 * The default method does nothing.
	 * The following shows some sample code for implementing the method.
	 * <pre>
	 * protected void insertTableRow(FormDataDocument datadoc, String tablepath, MbengNode row)
	 * throws Exception {
	 *     DatabaseAccess db = new DatabaseAccess(database_url);
	 *     try {
	 *         db.openConnection();
	 *         String query = "insert into mytable (mykey,mycol) values (?, ?)";
	 *         Object[] args = new Object[2];
	 *         args[0] = datadoc.getValue(row, "mykey");
	 *         args[1] = datadoc.getValue(row, "mycol");
	 *         db.runUpdate(query, args);
	 *         db.commit();
	 *     } catch (SQLException e) {
	 *         throw new DataAccessException(0, "Failed to insert row: " + e.getMessage(), e);
	 *     } finally {
	 *         db.closeConnection();
	 *     }
	 * }
	 * </pre>
	 *
	 * @param datadoc the data document received from the browser.
	 * @param tablepath the path of the table node in the form data document
	 * @param row the row to be updated in the persistent store
	 * @throws Exception
	 */
	protected void insertTableRow(FormDataDocument datadoc,
			String tablepath, MbengNode row)
		throws Exception {
		// do nothing
	}

	/**
	 * You must override this if deletion is supported in the GUI.
	 * The default method does nothing.
	 * The following is some sample code for the implementation
	 * <pre>
	 * protected void deleteTableRow(FormDataDocument datadoc, String tablepath, MbengNode row)
	 * throws Exception {
	 *     DatabaseAccess db = new DatabaseAccess(database_url);
	 *     try {
	 *         db.openConnection();
	 *         String query = "delete from mytable where mykey=?";
	 *         db.runUpdate(query, datadoc.getValue(row, "mykey"));
	 *         db.commit();
	 *     } catch (SQLException e) {
	 *         throw new DataAccessException(0, "Failed to delete row: " + e.getMessage(), e);
	 *     } finally {
	 *         db.closeConnection();
	 *     }
	 * }
	 * </pre>
	 *
	 * @param datadoc the data document received from the browser.
	 * @param tablepath the path of the table node in the form data document
	 * @param row the row to be deleted in the persistent store
	 * @throws Exception
	 */
	protected void deleteTableRow(FormDataDocument datadoc,
			String tablepath, MbengNode row)
		throws Exception {
		// do nothing
	}

	/**
	 * You must override this method in order to support updating a row,
	 * and perform real update to the data corresponding to the
	 * row in the persistent storage.
	 * The default method does nothing.
	 * The following shows some sample code for implementing the method.
	 * <pre>
	 * protected void updateTableRow(FormDataDocument datadoc, String tablepath, MbengNode row)
	 * throws Exception {
	 *     DatabaseAccess db = new DatabaseAccess(database_url);
	 *     try {
	 *         db.openConnection();
	 *         String query = "update mytable set mycol=? where mykey=?";
	 *         Object[] args = new Object[2];
	 *         args[0] = datadoc.getValue(row, "mykey");
	 *         args[1] = datadoc.getValue(row, "mycol");
	 *         db.runUpdate(query, args);
	 *         db.commit();
	 *     } catch (SQLException e) {
	 *         throw new DataAccessException(0, "Failed to update row: " + e.getMessage(), e);
	 *     } finally {
	 *         db.closeConnection();
	 *     }
	 * }
	 * </pre>
	 *
	 * @param datadoc the data document received from the browser.
	 * @param tablepath the path of the table node in the form data document
	 * @param row the row to be updated in the persistent store
	 * @throws Exception
	 */
	protected void updateTableRow(FormDataDocument datadoc,
			String tablepath, MbengNode row)
		throws Exception {
		// do nothing
	}

	/**
	 * A convenient method to refresh current page in a paginated environment
	 * @param datadoc
	 * @param tablepath
	 * @param metanode
	 * @throws Exception
	 */
	protected void refreshCurrentPage(FormDataDocument datadoc,
			String tablepath, MbengNode metanode) throws Exception {
		this.refreshPageData(datadoc, metanode, tablepath, "S", null);
	}

	private void refreshPageData(FormDataDocument datadoc, MbengNode metanode,
			String tablepath, String topage, Map<String, String> params) throws Exception {
		String sortOn = metanode==null?null:datadoc.getValue(metanode,"sort_on");
		if ("S".equals(topage)) {
			String v = metanode==null?null:datadoc.getValue(metanode, "start_row");
			int startrow = v==null?1:Integer.parseInt(v);
			int totalrows = this.getNumberOfRows(datadoc);
			String pageSize = params==null?null:params.get(FormConstants.URLARG_PAGESIZE);
			int pagesize = pageSize==null?this.getPageSize(datadoc):Integer.parseInt(pageSize);
			int nrows = pagesize;
			if (startrow+nrows-1>totalrows) nrows=totalrows-startrow+1;
			populate_pagedata(datadoc, tablepath, startrow, pagesize, totalrows, sortOn);
			setMetaInfo(datadoc, metanode, startrow, nrows, totalrows, sortOn);
		} else {
			populate_pagedata(datadoc, tablepath, 1, -1, -1, sortOn);
		}
	}

	private MbengNode findRowByCommandArgument(MbengNode tablenode, Map<String, String> params) {
		String rowstr = params.get(FormConstants.URLARG_ROW);
		if (rowstr==null) return null;
		int index = Integer.parseInt(rowstr);
		int i = 0;
		MbengNode row;
		for (row=tablenode.getFirstChild(); row!=null; row=row.getNextSibling()) {
			if (i==index) return row;
			i++;
		}
		return null;
	}

	private FormDataDocument update_table(String action, FormDataDocument datadoc,
			Map<String, String> params) throws Exception {
		String tablepath = params.get(FormConstants.URLARG_TABLE);
		String metaname = params.get(FormConstants.URLARG_META);
		String topage = params.get(FormConstants.URLARG_TOPAGE);
		MbengNode tablenode = datadoc.setTable(null, tablepath, false);
		MbengNode row;
		MbengNode metanode = metaname==null?null:datadoc.setSubform(null, metaname);
		if (action.equals("insertrow")) {
			row = this.findRowByCommandArgument(tablenode, params);
			if (row==null) throw new Exception("Cannot find the row to insert");
			try {
				insertTableRow(datadoc, tablepath, row);
			} catch (Exception e) {
				tablenode.removeChild(row);	// remove uninserted row
				throw new UserException(e.getMessage(), e);
			}
			this.refreshPageData(datadoc, metanode, tablepath, topage, params);
		} else if (action.equals("deleterow")) {
			row = this.findRowByCommandArgument(tablenode, params);
			try {
				if (row!=null) {
					deleteTableRow(datadoc, tablepath, row);
				} else if (metanode!=null) {	// find selected rows
					String selected_str = datadoc.getValue(metanode,"selected");
					if (!StringHelper.isEmpty(selected_str)) {
						String[] rows_str = selected_str.split(",");
						for (String row_str : rows_str) {
							int row_index = Integer.parseInt(row_str);
							row = datadoc.getRow(tablenode, row_index);
							if (row!=null) deleteTableRow(datadoc, tablepath, row);
						}
					}
				}
			} catch (Exception e) {
				throw new UserException(e.getMessage(), e);
			}
			this.refreshPageData(datadoc, metanode, tablepath, topage, params);
		} else {	// "updaterow"
			row = this.findRowByCommandArgument(tablenode, params);
			if (row==null) throw new Exception("Cannot find the row to update");
			try {
				updateTableRow(datadoc, tablepath, row);
			} catch (Exception e) {
				throw new UserException(e.getMessage(), e);
			}
			this.refreshPageData(datadoc, metanode, tablepath, topage, params);
		}
		return datadoc;
	}

}
