/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.pages;

import java.awt.Dimension;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JToolBar;

class ToolPane extends JToolBar {
	
	private static final long serialVersionUID = 1L;
	private ActionListener listener;
	
	ToolPane(ActionListener listener) {
		this.listener = listener;
	}
	
	public JCheckBox createCheckBox(String label, boolean selected, String action) {
        JCheckBox button;
        button = new JCheckBox(label, selected);
        button.setActionCommand(action);
        button.addActionListener(listener);
        this.add(button);
        return button;
    }
	
	public JComboBox createDropdown(String[] choices, String[] images, String tip,
			String action, int selected, int width) {
		JComboBox jcb = new JComboBox();
		ImageIcon icon;
		for (int i=0; i<images.length; i++) {
			icon = new ImageIcon(this.getClass().
					getClassLoader().getResource("images/" + images[i]));
			jcb.addItem(icon);
			icon.setDescription(choices[i]);
		}
		jcb.setSelectedIndex(selected);
		jcb.setActionCommand(action);
		jcb.addActionListener(listener);
		if (width>0) {
		    Dimension d = new Dimension(width, 30);
//		    jcb.setSize(d);           // this does not help
//		    jcb.setPreferredSize(d);  // this does not help either
            jcb.setMaximumSize(d);
		}
		if (tip!=null) jcb.setToolTipText(tip);
        this.addSeparator();
		this.add(jcb);
		return jcb;
	}
	
	public JComboBox createDropdown(String[] choices,
			String action, int selected, int width) {
		JComboBox jcb = new JComboBox(choices);
		jcb.setSelectedIndex(selected);
		jcb.setActionCommand(action);
		jcb.addActionListener(listener);
        if (width>0) {
            Dimension d = new Dimension(width, 30);
//          jcb.setSize(d);           // this does not help
//          jcb.setPreferredSize(d);  // this does not help either
            jcb.setMaximumSize(d);
        }
        this.addSeparator();
		this.add(jcb);
		return jcb;
	}
	
	public JButton createButton(String name, String iconname,
			String tip, String action) {
		JButton button;
		if (iconname!=null) {
		    button = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("images/" + iconname)));
		    //button.setMinimumSize(new Dimension(36,36));
		    //button.setSize(new Dimension(36,36));
		    //button.setPreferredSize(new Dimension(36,36));
		} else button = new JButton(name);
		if (tip!=null) button.setToolTipText(tip);
		button.setActionCommand(action);
		button.addActionListener(listener);
        if(name != null) {
            if(name.equalsIgnoreCase("Delete")) {
                button.setEnabled(true);
            }
        }
		this.add(button);
		return button;
	}

}
