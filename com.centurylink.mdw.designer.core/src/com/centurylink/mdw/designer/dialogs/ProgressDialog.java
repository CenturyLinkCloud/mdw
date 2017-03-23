/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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