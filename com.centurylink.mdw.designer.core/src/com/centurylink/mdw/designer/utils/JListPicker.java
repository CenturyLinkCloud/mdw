/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.utils;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.event.ListDataListener;

public class JListPicker extends JPanel implements ActionListener {

	private static final long serialVersionUID = 1L;

	private JList selectedList;
	private JList candidateList;
	private JScrollPane scrollpaneSelected, scrollpaneCandidate;
	private JButton add, delete;
	private MyListModel selectedModel;
	private MyListModel candidateModel;

	public JListPicker() {
		super(null);

		selectedModel = new MyListModel();
		candidateModel = new MyListModel();

		selectedList = new JList(selectedModel);
        scrollpaneSelected = new JScrollPane(selectedList);
        scrollpaneSelected.setBorder(BorderFactory.createTitledBorder("selected"));
        add(scrollpaneSelected);

		candidateList = new JList(candidateModel);
        scrollpaneCandidate = new JScrollPane(candidateList);
        scrollpaneCandidate.setBorder(BorderFactory.createTitledBorder("candidates"));
        add(scrollpaneCandidate);

        add = new JButton("<<");
        add.setActionCommand("add");
        add.addActionListener(this);
        add(add);
        delete = new JButton(">>");
        add.setActionCommand("delete");
        delete.addActionListener(this);
        add(delete);
	}

	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if (cmd.equals("add")) {

		} else if (cmd.equals("delete")) {

		}
	}

	@Override
	public void setBounds(Rectangle r) {
		setBounds(r.x, r.y, r.width, r.height);
	}

	@Override
	public void setBounds(int x, int y, int w, int h) {
		super.setBounds(x, y, w, h);
		scrollpaneSelected.setBounds(5, 5, w/2-30, h-10);
		scrollpaneCandidate.setBounds(w/2+25, 5, w/2-30, h-10);
		add.setBounds(w/2-24, h/2-28, 48, 25);
		delete.setBounds(w/2-24, h/2+3, 48, 25);
	}

	private class ListItem {
	}

	private class MyListModel extends ArrayList<ListItem> implements ListModel {

		private static final long serialVersionUID = 1L;

		public void addListDataListener(ListDataListener l) {
			// TODO Auto-generated method stub
		}

		public ListItem getElementAt(int index) {
			return get(index);
		}

		public int getSize() {
			return size();
		}

		public void removeListDataListener(ListDataListener l) {
			// TODO Auto-generated method stub

		}

	}

}
