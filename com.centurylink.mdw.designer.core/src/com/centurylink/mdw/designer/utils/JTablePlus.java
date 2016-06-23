/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.utils;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import com.centurylink.mdw.common.constant.FormConstants;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.model.FormDataDocument;
import com.qwest.mbeng.MbengException;
import com.qwest.mbeng.MbengNode;

public class JTablePlus extends JPanel implements ActionListener, MouseListener {

	private static final long serialVersionUID = 1L;

	private static final String ACTION_FIRST = "TablePlusFirst";
	private static final String ACTION_PREV = "TablePlusPrev";
	private static final String ACTION_NEXT = "TablePlusNext";
	private static final String ACTION_LAST = "TablePlusLast";
	private static final String ACTION_GOTO = "TablePlusGoto";
	public static final String ACTION_ENGINE_CALL = "TABLE_ENGINE_CALL";

	private MyTableModel model;
	private TableSorter sorter;
    private JTable table;
    private JScrollPane scrollpane;
    private boolean atDesignTime;
    private JButton button_first, button_prev, button_next, button_last;
    private JButton button_goto;
    private JTextField goto_row;
    private JLabel rowrange;
    private ActionListener actionListener;
    private String paginator;
    private FormDataDocument datadoc;
    private String datapath;
    private String engine_call_action;
    private boolean withGotoButton = true;

	public JTablePlus(boolean isPaginated, boolean atRuntime) {
		super(new BorderLayout());
		model = new MyTableModel();
		this.atDesignTime = !atRuntime;
        if (!atDesignTime) {
        	sorter = new TableSorter(model);
        	table = new MyJTable(sorter);
        	sorter.setTableHeaderX(table.getTableHeader());
    		sorter.setPaginator(this);
        } else {
        	table = new JTable(model);
            sorter = null;
        }
        scrollpane = new JScrollPane(table);
        this.add(BorderLayout.CENTER, scrollpane);
        if (!atDesignTime) table.addMouseListener(this);
        if (isPaginated) {
        	JPanel button_panel = new JPanel();
        	this.add(BorderLayout.SOUTH, button_panel);

        	button_first = new JButton("First");
        	button_first.setActionCommand(ACTION_FIRST);
        	button_first.addActionListener(this);
        	button_panel.add(button_first);

        	button_prev = new JButton("Prev");
        	button_prev.setActionCommand(ACTION_PREV);
        	button_prev.addActionListener(this);
        	button_panel.add(button_prev);

        	rowrange = new JLabel("No rows");
        	button_panel.add(rowrange);

        	button_next = new JButton("Next");
        	button_next.setActionCommand(ACTION_NEXT);
        	button_next.addActionListener(this);
        	button_panel.add(button_next);

        	button_last = new JButton("Last");
        	button_last.setActionCommand(ACTION_LAST);
        	button_last.addActionListener(this);
        	button_panel.add(button_last);

        	if (withGotoButton) {
        		button_panel.add(Box.createHorizontalStrut(60));
        		button_goto = new JButton("Go to row: ");
        		button_goto.setActionCommand(ACTION_GOTO);
        		button_goto.addActionListener(this);
            	button_panel.add(button_goto);
            	goto_row = new JTextField();
            	goto_row.setPreferredSize(new Dimension(50,20));
            	button_panel.add(goto_row);
        	}

        } else rowrange = null;
	}

	@Override
	public void addMouseListener(MouseListener v) {
		if (atDesignTime) {
			table.addMouseListener(v);
			table.getTableHeader().addMouseListener(v);
			scrollpane.addMouseListener(v);
			super.addMouseListener(v);
		}
	}

	@Override
	public void addMouseMotionListener(MouseMotionListener l) {
		// for design time only
		super.addMouseMotionListener(l);
		scrollpane.addMouseMotionListener(l);
	}

	public void setPaginator(ActionListener v, String paginator) {
		this.actionListener = v;
		this.paginator = paginator;
	}

	public void setData(FormDataDocument dataxml, String datapath) {
		this.datadoc = dataxml;
		this.datapath = datapath;
		MbengNode datanode = dataxml.getNode(datapath);
		MbengNode metanode = dataxml.getNode(datapath+"_META");
		model.setData(datanode);
		if (metanode!=null) {
			String sorton = datadoc.getValue(metanode, "sort_on");
			if (sorton!=null) {
				boolean descending = sorton.startsWith("-");
				if (descending) sorton = sorton.substring(1);
				setSortingStatus(sorton, descending);
			}
			String selected_str = datadoc.getValue(metanode, "selected");
			if (!StringHelper.isEmpty(selected_str)) {
				String[] rows_str = selected_str.split(",");
				int firstRow = Integer.parseInt(rows_str[0]);
				try {
					if (model.getRowCount()>firstRow) {
						table.setRowSelectionInterval(firstRow, firstRow);
						datadoc.setValue(metanode, "selected", rows_str[0]);
					} else datadoc.setValue(metanode, "selected", null);
				} catch (MbengException e) {
				}
			}
			if (rowrange!=null) {
				int nrows = model.getRowCount();
				if (nrows==0) {
					rowrange.setText("No rows");
					button_first.setEnabled(false);
					button_prev.setEnabled(false);
					button_next.setEnabled(false);
					button_last.setEnabled(false);
					button_goto.setEnabled(false);
				} else {
					String v = datadoc.getValue(metanode, "start_row");
					int start_row = v==null?1:Integer.parseInt(v);
					v = datadoc.getValue(metanode, "total_rows");
					int total_rows = v==null?nrows:Integer.parseInt(v);
					rowrange.setText("" + start_row + " - " + (start_row+nrows-1) + " of " + total_rows);
					button_first.setEnabled(start_row>1);
					button_prev.setEnabled(start_row>1);
					button_next.setEnabled(start_row+nrows-1<total_rows);
					button_last.setEnabled(start_row+nrows-1<total_rows);
					button_goto.setEnabled(nrows<total_rows);
				}
			}
		}
		// for some reason w/o the following, layout was not done and screen shows blank until resized
        super.doLayout();
        for (int k=this.getComponentCount(); k>0; k--) {
        	Component comp = this.getComponent(k-1);
        	if (comp instanceof Container) ((Container)comp).doLayout();
    	}
	}

	public void setSortingStatus(String sorton, boolean descending) {
		for (int k=0; k<model.getColumnCount(); k++) {
			String colname = this.getColumnName(k);
			if (colname.equals(sorton)) {
				sorter.setSortingStatus(k, descending?-1:1);
				break;
			}
		}
	}

	public void addColumn(MbengNode node, String label) {
        TableColumnModel columnModel = table.getColumnModel();
        model.addColumn(node, label, columnModel);
        table.updateUI();
	}

	public int getSelectedRow() {
		return table.getSelectedRow();
	}

	public String getColumnName(int j) {
		return model.column_nodes.get(j).getAttribute(FormConstants.FORMATTR_DATA);
	}

	public String getColumnLabel(int j) {
		return model.column_labels.get(j);
	}

	public void setColumnLabel(int j, String label) {
		model.column_labels.set(j, label);
		table.getColumnModel().getColumn(j).setHeaderValue(label);
	}

	public int getColumnCount() {
		return model.getColumnCount();
	}

	public int getRowCount() {
		return model.getRowCount();
	}

    private class MyTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private ArrayList<MbengNode> column_nodes;
        private ArrayList<String> column_labels;
        private List<String[]> rows;
        MyTableModel() {
            column_nodes = new ArrayList<MbengNode>();
            column_labels = new ArrayList<String>();
            rows = null;
        }
        void setData(MbengNode datanode) {
            rows = new ArrayList<String[]>();
            String[] column_names = new String[column_nodes.size()];
            for (int j=0; j<column_names.length; j++) {
            	column_names[j] = column_nodes.get(j).getAttribute(FormConstants.FORMATTR_DATA);
            }
            if (datanode==null) return;
            for (MbengNode rnode = datanode.getFirstChild();
                    rnode!=null;
                    rnode = rnode.getNextSibling()) {
                String[] row = new String[column_nodes.size()];
                for (int j=0; j<row.length; j++) {
                	String colname = column_names[j];
                	if (colname==null || colname.length()==0) continue;
                    MbengNode cnode = rnode.findChild(colname);
                    row[j] = cnode==null?"":cnode.getValue();
                }
                rows.add(row);
            }
        }
        public int getColumnCount() {
            return column_nodes.size()==0?1:column_nodes.size();
        }
        public String getColumnName(int col) { return column_labels.size()==0?"Column":column_labels.get(col); }
        public int getRowCount() { return rows==null?5:rows.size(); }
        public Object getValueAt(int row, int col) {
            if (rows==null)
                return "Cell (" + (row+1) + "," + (col+1) + ")";
            else return rows.get(row)[col];
        }
        public boolean isCellEditable(int i, int j) { return false; }
        public void setValueAt(Object value, int row, int col) { }
        public boolean isColumnSortable(int j) {
        	return "true".equalsIgnoreCase(column_nodes.get(j).getAttribute(FormConstants.FORMATTR_SORTABLE));
        }
        void addColumn(MbengNode node, String label, TableColumnModel columnModel) {

        	TableColumn col;
            int n = column_nodes.size();
            if (column_nodes.size()>0) {
                col = new TableColumn();
                col.setModelIndex(n);
                columnModel.addColumn(col);
            } else col = table.getColumnModel().getColumn(0);
            column_nodes.add(node);
            column_labels.add(label);
            columnModel.getColumn(n).setHeaderValue(label);
            String av = node.getAttribute(FormConstants.FORMATTR_ACTION);
    		if (!StringHelper.isEmpty(av)) {
    			HyperlinkRenderer hyperlinkRenderer = new HyperlinkRenderer();
    			col.setCellRenderer(hyperlinkRenderer);
    		}
        }
    }

	@Override
	public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
		if (actionListener==null) return;
		if (cmd.equals(JTablePlus.ACTION_FIRST)) {
			String action = paginator + "?action=paging&table=" + datapath
				+ "&meta=" + datapath  + "_META&topage=F";
			call_engine(action);
        } else if (cmd.equals(JTablePlus.ACTION_PREV)) {
        	String action = paginator + "?action=paging&table=" + datapath
 				+ "&meta=" + datapath  + "_META&topage=P";
        	call_engine(action);
        } else if (cmd.equals(JTablePlus.ACTION_NEXT)) {
        	String action = paginator + "?action=paging&table=" + datapath
        	 	+ "&meta=" + datapath  + "_META&topage=N";
        	call_engine(action);
        } else if (cmd.equals(JTablePlus.ACTION_LAST)) {
        	String action = paginator + "?action=paging&table=" + datapath
 				+ "&meta=" + datapath  + "_META&topage=L";
        	call_engine(action);
        } else if (cmd.equals(JTablePlus.ACTION_GOTO)) {
        	try {
        		int row = Integer.parseInt(goto_row.getText());
        		MbengNode metanode = datadoc.getNode(datapath+"_META");
        		String v = datadoc.getValue(metanode, "total_rows");
				int total_rows = v==null?model.getRowCount():Integer.parseInt(v);
				if (row>=1 && row<=total_rows) {
					datadoc.setValue(metanode, "start_row", Integer.toString(row));
					String action = paginator + "?action=paging&table=" + datapath
        				+ "&meta=" + datapath  + "_META&topage=S";
					call_engine(action);
				}
        	} catch (Exception ex) {
        		ex.printStackTrace();
        	}
        } else if (cmd.equals(TableSorter.ACTION_TABLE_SORTING)) {
        	performSorting(e);
		}
	}

	private void call_engine(String action) {
		engine_call_action = action;
		ActionEvent e = new ActionEvent(this, 0, ACTION_ENGINE_CALL);
        actionListener.actionPerformed(e);
	}

	private void performSorting(ActionEvent e) {
		int columnIndex = e.getID();
        boolean descending = columnIndex<0;
		if (columnIndex<0) columnIndex = -columnIndex - 1;
		else columnIndex = columnIndex - 1;
		if (!model.isColumnSortable(columnIndex)) {
			e.setSource(null);
			return;
		}
        String sorton = getColumnName(columnIndex);
        if (sorton==null) return;
        if (descending) sorton = "-" + sorton;
        MbengNode metanode = datadoc.getNode(datapath+"_META");
        try {
        	if (metanode!=null) datadoc.setValue(metanode, "sort_on", sorton);
        } catch (MbengException e1) {
        }
        String action = paginator + "?action=paging&table=" + datapath
			+ "&meta=" + datapath  + "_META&topage=S&sorton=" + sorton;
        call_engine(action);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount()==2) {
			int j = table.getSelectedColumn();
			String action = model.column_nodes.get(j).getAttribute(FormConstants.FORMATTR_ACTION);
			if (!StringHelper.isEmpty(action)) {
				int i = table.getSelectedRow();
				String value = model.rows.get(i)[j];
				if (!StringHelper.isEmpty(value)) {
					String tableId = super.getName();
					if (action.indexOf('?')>0) action = action + "&";
    				else action = action + "?";
    				action += "table=" + tableId + "&row=" + value;
    				call_engine(action);
				}
			}
		}
	}

	public String getEngineCallAction() {
		return engine_call_action;
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		TableSorter sorter = (TableSorter)table.getModel();
		int[] selected = table.getSelectedRows();
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<selected.length; i++) {
			if (i>0) sb.append(",");
			sb.append(sorter.modelIndex(selected[i]));
		}
		try {
//			System.out.println("SELECTED: " + sb.toString());
			datadoc.setValue(datapath+"_META.selected", sb.toString());
		} catch (MbengException e1) {
			e1.printStackTrace();
		}
	}

//	class HyperlinkRenderer extends JLabel implements TableCellRenderer {
//	    private static final long serialVersionUID = 1L;
//		public HyperlinkRenderer() {
//			super();
//			this.setToolTipText("double click to open the link");
////			this.setOpaque(true);
//			setForeground(Color.blue);
//		}
//		public Component getTableCellRendererComponent(JTable table,
//				Object value, boolean isSelected, boolean hasFocus, int row,
//				int column) {
//			if (isSelected) {
//				setBackground(table.getSelectionBackground());
//			} else {
//				setBackground(table.getBackground());
//			}
//			setText((String)value);
//			return this;
//		}
//	}

	class HyperlinkRenderer extends DefaultTableCellRenderer {
	    private static final long serialVersionUID = 1L;
		public HyperlinkRenderer() {
			super();
			this.setToolTipText("double click to open the link");
		}
		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			Component ret = super.getTableCellRendererComponent(table,
					value, isSelected, hasFocus, row, column);
			setForeground(Color.blue);
			return ret;
		}
	}

	class MyJTable extends JTable {

        private WeakReference<TableCellRenderer> wrappedHeaderRendererRef = null;
        private TableCellRenderer wrapperHeaderRenderer = null;
        public MyJTable(TableModel dm) {
          super(dm);
        }

        private class MyTableColumn extends TableColumn {
            MyTableColumn(int modelIndex) {
                super(modelIndex);
            }
            @Override
            public TableCellRenderer getHeaderRenderer() {
                TableCellRenderer defaultHeaderRenderer =  MyJTable.this.getTableHeader().getDefaultRenderer();
                if (wrappedHeaderRendererRef == null || wrappedHeaderRendererRef.get() != defaultHeaderRenderer) {
                    wrappedHeaderRendererRef = new WeakReference<TableCellRenderer>(defaultHeaderRenderer);
                    wrapperHeaderRenderer = new MySortableHeaderRenderer(defaultHeaderRenderer);
                }
                return wrapperHeaderRenderer;
            }
        }
        @Override
        public void createDefaultColumnsFromModel() {
            TableModel m = getModel();
            if (m != null) {
                // remove any current columns
                TableColumnModel cm = getColumnModel();
                while (cm.getColumnCount() > 0) {
                    cm.removeColumn(cm.getColumn(0));
                }

                // create new columns from the data model info
                for (int i = 0; i < m.getColumnCount(); i++) {
                    TableColumn newColumn = new MyTableColumn(i);
                    addColumn(newColumn);
                }
            }
        }
	}

    private class MySortableHeaderRenderer implements TableCellRenderer {
        private TableCellRenderer tableCellRenderer;

        public MySortableHeaderRenderer(TableCellRenderer tableCellRenderer) {
            this.tableCellRenderer = tableCellRenderer;
        }

        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            Component c = tableCellRenderer.getTableCellRendererComponent(table,
                    value, isSelected, hasFocus, row, column);
            if (c instanceof JLabel) {
                JLabel l = (JLabel) c;
                l.setHorizontalTextPosition(JLabel.LEFT);
                int modelColumn = table.convertColumnIndexToModel(column);
                l.setIcon(sorter.getHeaderRendererIcon(modelColumn, l.getFont().getSize()));
            }
            return c;
        }
    }
}
