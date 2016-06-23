/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.runtime;

import com.centurylink.mdw.designer.MainFrame;
import com.centurylink.mdw.designer.utils.Constants;
import com.centurylink.mdw.model.value.work.WorkTransitionInstanceVO;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

public class WorkTransitionsDialog extends JDialog implements ActionListener{

     JTable table;
     MainFrame frame;
     TableModel table_model;
     List<WorkTransitionInstanceVO> workTransitionsDataList;

     private static final long serialVersionUID = 1L;

     public WorkTransitionsDialog(MainFrame frame){
        super(frame,"Work Transition Instance(s)",true);
        this.frame = frame;
        setTitle("Work Transition Instance(s)");
        setModal(true);
        setSize(720,150);
        setLocationRelativeTo(frame);
        workTransitionsDataList = new ArrayList<WorkTransitionInstanceVO>();
        table_model = new AbstractTableModel() {
            private static final long serialVersionUID = 1L;

            public int getColumnCount() { return 5; }
            public String getColumnName(int col) {
                switch(col){
                 case 0:
                    return "Instance ID";
                 case 1:
                    return "Process Instance ID ";
                 case 2:
                    return "Status";
                 case 3:
                    return "Start Date";
                 case 4:
                    return "End Date";
                default:
                    return null;

                }

              }
            public int getRowCount() {
                return workTransitionsDataList.size();
            }
            public Object getValueAt(int row, int col) {
                WorkTransitionInstanceVO inst = workTransitionsDataList.get(row);
                switch(col){
                 case 0:
                    return inst.getTransitionInstanceID();
                 case 1:
                    return inst.getProcessInstanceID();
                 case 2:
                    return this.translateStatusCode(inst.getStatusCode());
                 case 3:
                    if(null!= inst.getStartDate())
                        return inst.getStartDate().toString();
                     else
                        return "";
                 case 4:
                    if(null!= inst.getEndDate())
                        return inst.getEndDate().toString();
                     else
                        return "";
                 default:
                    return null;

                }
             }

            public boolean isCellEditable(int i, int j) {
                return false;
            }

            public void setValueAt(Object value, int row, int col) {
            }
            public Class<?> getColumnClass(int c) {
                return getValueAt(0, c).getClass();
            }
             private String translateStatusCode(int status) {
                switch(status){
                     case 1:
                        return "Initiated";
                     case 2:
                        return "In Progress";
                     case 3:
                        return "Not Required";
                     case 4:
                        return "Waiting for dependents";
                     case 5:
                        return "Skipped";
                     case 6:
                        return "Completed";
                     case 7:
                        return "Waiting for external event";
                     case 8:
                        return "Delay";
                     case 9:
                        return "Failed";
                    default:
                        return null;
                }

             }
        };
        table = new JTable(table_model);
        JScrollPane scrollpane = new JScrollPane(table);
        table.getColumnModel().getColumn(0).setMinWidth(50);
        table.getColumnModel().getColumn(0).setMaxWidth(200);
        table.getColumnModel().getColumn(1).setMinWidth(50);
        table.getColumnModel().getColumn(1).setMaxWidth(200);
        table.getColumnModel().getColumn(2).setMinWidth(50);
        table.getColumnModel().getColumn(2).setMaxWidth(200);
        table.getColumnModel().getColumn(3).setMinWidth(100);
        table.getColumnModel().getColumn(3).setMaxWidth(300);
        table.getColumnModel().getColumn(4).setMinWidth(100);
        table.getColumnModel().getColumn(4).setMaxWidth(300);
        getContentPane().add(scrollpane,BorderLayout.CENTER);
        JPanel topPanel = new JPanel();
        JButton button = new JButton("Close");
        button.setActionCommand(Constants.ACTION_EXIT);
        button.addActionListener(this);
        topPanel.add(button);
        getContentPane().add(topPanel, BorderLayout.SOUTH);
        JPanel leftPanel = new JPanel();
        getContentPane().add(leftPanel, BorderLayout.WEST);
        JPanel rightPanel = new JPanel();
        getContentPane().add(rightPanel, BorderLayout.EAST);
     }

    /**
     * Sets the Work Transitions
     * @param workTransitionList
     */
    public void setWorkTransitionList(List<WorkTransitionInstanceVO> workTransitionList){
        this.workTransitionsDataList = workTransitionList;
    }

    /**
     * Returns the Work Transitions
     * @return List
     */
    public List<WorkTransitionInstanceVO> getWorkTansitionList() {
        return workTransitionsDataList;
    }

    public void actionPerformed(ActionEvent event){
        Object source = event.getSource();
	    String cmd;
		if (source instanceof AbstractButton)
			cmd = ((AbstractButton)source).getActionCommand();
		else return;
        if (cmd.equals(Constants.ACTION_EXIT)) {
            this.setVisible(false);
        }
    }


}
