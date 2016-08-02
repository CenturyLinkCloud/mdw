/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.dialogs; 

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class ProgressDialog extends JDialog {
    
    private static final long serialVersionUID = 1L;
    
    JProgressBar progressBar;
    
    public ProgressDialog(String title,String progressBarString,
        JFrame frame,boolean modal) {
        super(frame,title,modal);
        setTitle(title);
        setModal(modal);
        setSize(300, 75);
        setLocationRelativeTo(frame);
        progressBar = new JProgressBar(0,20);
        progressBar.setBounds(2, 2, 286, 36);
        progressBar.setBorderPainted(true);
        progressBar.setIndeterminate(true);
        progressBar.setStringPainted(true);
        progressBar.setString("  "+  progressBarString + "  " );
        JPanel progressPanel = new JPanel(null);
        progressPanel.setSize(290, 40);
        progressPanel.add(progressBar);
        getContentPane().add(progressPanel);
//        pack();
    }
 
    public void setString(String message) {
        progressBar.setString("  "+  message + "  " );
    }
}