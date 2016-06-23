/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.dialogs;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import com.centurylink.mdw.designer.MainFrame;
import com.centurylink.mdw.designer.pages.DesignerPage;
import com.centurylink.mdw.designer.utils.Constants;
import com.centurylink.mdw.model.value.process.ProcessVO;

public class DeleteProcessDialog extends JDialog implements ActionListener{

    private static final long serialVersionUID = 1L;

    private JTable table;
    private DesignerPage page;
    private TableModel table_model;
    private JButton button_delete, button_cancel;
    private int processIndex;
    private JCheckBox checkbox_deleteInstances;
    private ArrayList<ProcessVO> procdefs;
    private Boolean[] included;
    private Boolean[] deleted;
    private ProcessVO newest;

    public DeleteProcessDialog(DesignerPage page, int processIndex){
        super(page.frame);
        this.page = page;
        this.processIndex = processIndex;
        setTitle("Delete Process");
        setModal(true);
        setSize(420,360);
        setLocationRelativeTo(page.frame);
		JPanel panel = new JPanel(null);
//        panel.setLayout(null);
        panel.setSize(400,330);
        panel.setOpaque(false);
		JLabel label = new JLabel("Please select the versions to be deleted");
		label.setBounds(20, 20, 400, 20);
		panel.add(label);

        table_model = new AbstractTableModel() {
            private static final long serialVersionUID = 1L;
            public int getColumnCount() { return 4; }
            public String getColumnName(int col)
                { return col==0?"To delete":col==1?"Version":col==2?"Process ID":"Deleted"; }
            public int getRowCount() { return procdefs.size(); }
            public Object getValueAt(int row, int col) {
                if (col==0) return included[row];
                else if (col==1)
                    return procdefs.get(row).getVersionString();
                else if (col==2) return procdefs.get(row).getProcessId().toString();
                else return deleted[row];
            }
            public boolean isCellEditable(int i, int j) { return j==0 && !deleted[i]; }
            public void setValueAt(Object value, int row, int col) {
                if (col==0) included[row] = (Boolean)value;
            }
            public Class<?> getColumnClass(int col) {
                if (col==0 || col==3) return Boolean.class;
                else return super.getColumnClass(col);
            }
        };

        table = new JTable(table_model);
        setData();
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBounds(20,40,360,160);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        panel.add(scrollPane);

        checkbox_deleteInstances = new JCheckBox();
        checkbox_deleteInstances.setText("Delete instances if exist");
        checkbox_deleteInstances.setBounds(20, 220, 360, 20);
        panel.add(checkbox_deleteInstances);

        button_delete= new JButton("Delete");
        button_delete.setBounds(80, 280, 120, 25);
        button_delete.setActionCommand(Constants.ACTION_DELETE);
        button_delete.addActionListener(this);
        panel.add(button_delete);

        button_cancel = new JButton("Cancel");
        button_cancel.setBounds(240, 280, 120, 25);
        button_cancel.setActionCommand(Constants.ACTION_EXIT);
        button_cancel.addActionListener(this);
        panel.add(button_cancel);
        getContentPane().add(panel);
    }

    private void setData() {
        ProcessVO earliest;
        newest = page.getDataModel().getProcesses().get(processIndex);
        earliest = newest;
        while (earliest.getPrevVersion()!=null) earliest = earliest.getPrevVersion();
        ProcessVO one = earliest;
        procdefs = new ArrayList<ProcessVO>();
        int i=0;
        while (one!=null) {
            procdefs.add(one);
            i++;
            one = one.getNextVersion();
        }
        included = new Boolean[procdefs.size()];
        deleted = new Boolean[included.length];
        for (i=0; i<included.length; i++) {
            included[i] = false;
            deleted[i] = false;
        }
    }

    public void actionPerformed(ActionEvent event){
        Object source = event.getSource();
	    String cmd ="";
		if (source instanceof AbstractButton)
			cmd = ((AbstractButton)source).getActionCommand();
        if (cmd.equals(Constants.ACTION_DELETE)) {
            ProgressDialog progressBar = new ProgressDialog("Deleting",
                    "Deleting Process " , page.frame, true);
            RemoveProcessThread removeProcessThread = new RemoveProcessThread(progressBar, page.frame);
            removeProcessThread.start();
            progressBar.setVisible(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            if (!removeProcessThread.has_error) {
                updateProcessList();
                this.setVisible(false);
            }
        } else if (cmd.equals(Constants.ACTION_EXIT)) {
            updateProcessList();
            this.setVisible(false);
        }
    }

    private void updateProcessList() {
        int n = deleted.length;
        int newLatest = n-1;
        while (newLatest>=0 && deleted[newLatest]) newLatest--;
        if (newLatest<0) {
            page.getDataModel().getProcesses().remove(processIndex);
            newest = null;
        } else {
            if (newLatest!=n-1)
                page.getDataModel().getProcesses().set(processIndex, procdefs.get(newLatest));
            newest = procdefs.get(newLatest);
            newest.setNextVersion(null);
            newest.setPrevVersion(null);
            ProcessVO next=newest, prev;
            for (int i=newLatest-1; i>=0; i--) {
                if (deleted[i]) continue;
                prev = procdefs.get(i);
                prev.setNextVersion(next);
                prev.setPrevVersion(null);
                next.setPrevVersion(prev);
                next = prev;
            }
        }
    }

    public ProcessVO getNewestVersion() {
        return newest;
    }

    class RemoveProcessThread extends Thread {

        private MainFrame frame;
        private ProgressDialog progressBar;
        boolean has_error;
        RemoveProcessThread(ProgressDialog progressBar,MainFrame frame){
            this.frame = frame;
            this.progressBar = progressBar;

        }

        public void run() {
            boolean deleteInstances = checkbox_deleteInstances.isSelected();
            has_error = false;
            for (int i=0; i<included.length; i++) {
                if (!included[i] || deleted[i]) continue;
                String version = procdefs.get(i).getVersionString();
                try {
                    frame.dao.removeProcess(procdefs.get(i),deleteInstances);
                    deleted[i] = true;
                } catch (Exception e) {
                    has_error = true;
                    page.showError(DeleteProcessDialog.this, "Failed to remove version " + version, e);
                    frame.setNewServer();
                    e.printStackTrace();
                    break;
                }
            }
            progressBar.setVisible(false);
        }

    }

}
