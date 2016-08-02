/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.designer.testing;

import com.centurylink.mdw.designer.MainFrame;
import com.centurylink.mdw.designer.utils.Constants;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.qwest.mbeng.MbengDocument;
import com.qwest.mbeng.MbengException;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class TestDataLoader {
    
    private static ArrayList<RuleSetVO> testDataList = null;
    
    private MainFrame frame;
    private JDialog parentDialog;
    
    public TestDataLoader(MainFrame frame, JDialog parentDialog) {
    	this.frame = frame;
    	this.parentDialog = parentDialog;
    }

	public void loadMessageList(JComboBox msglistCombo) {
		if (testDataList==null) testDataList = load_test_data();
		for (RuleSetVO msg : testDataList) {
			msglistCombo.addItem(msg);
		}
	}
	
	public String loadMessage(JComboBox msglistCombo) {
		RuleSetVO sel = (RuleSetVO)msglistCombo.getSelectedItem();
		return sel.getRuleSet();
	}
	
	public String filterMessage(String msg, String requestMsg) {
		PrintStream log = System.out;
		TestDataFilter filter = new TestDataFilter(msg, log, false);
		List<String> holders = filter.getUniquePlaceHolders();
		MbengDocument refdoc;
		try {
			refdoc = requestMsg==null?null:TestDataFilter.parseRequest(requestMsg);
		} catch (MbengException e) {
			log.println("Failed to parse request message as XML or JSON: " + e.getMessage());
			refdoc = null;
		}
		Map<String,String> parameters;
		if (holders.size()>0) {
			PlaceHolderDialog dialog;
			if (parentDialog==null) dialog = new PlaceHolderDialog(frame, holders);
			else dialog = new PlaceHolderDialog(parentDialog, holders);
			dialog.setVisible(true);
			parameters = dialog.getParameters();
		} else parameters = null;
		return filter.applyFilters(parameters, refdoc);
    }
	
	private ArrayList<RuleSetVO> load_test_data() {
		ArrayList<RuleSetVO> testdatas = new ArrayList<RuleSetVO>();
		List<RuleSetVO> allRulesets = frame.getDataModel().getRuleSets(RuleSetVO.TESTDATA);
		for (RuleSetVO one : allRulesets) {
			testdatas.add(one);
		}

        return testdatas;
	}
	
	static class PlaceHolderDialog extends JDialog implements ActionListener {
		private static final long serialVersionUID = 1L;
		static HashMap<String,String> map = null;
		private HashMap<String,JTextField> textfields;
		private List<String> holders;
		PlaceHolderDialog(JDialog parent, List<String> holders) {
			super(parent);
			init(parent, holders);
		}
		PlaceHolderDialog(MainFrame parent,  List<String> holders) {
			super(parent);
			init(parent, holders);
		}
		void init(Container parent, List<String> holders) {
			this.setModal(true);
	        setLocationRelativeTo(parent);
			this.holders = holders;
			JPanel panel = new JPanel(new BorderLayout());
			if (map==null) map = new HashMap<String,String>();
			JPanel varpanel = new JPanel(null);
			textfields = new HashMap<String,JTextField>();
			this.setTitle("Place Holder Substitutions");
			JLabel label;
			JTextField textfield;
//			Dimension textfield_size = new Dimension(360,20);
			int i = 0;
			for (String var : holders) {
				String val = map.get(var);
				label = new JLabel(var);
				label.setHorizontalAlignment(SwingConstants.RIGHT);
				label.setBounds(5, i*24+5, 120, 20);
				varpanel.add(label);
				textfield = new JTextField();
				textfield.setBounds(130, i*24+5, 360, 22);
				if (val!=null) textfield.setText(val);
				varpanel.add(textfield);
				textfields.put(var, textfield);
				i++;
			}
			JPanel buttonpanel = new JPanel();
			JButton done = new JButton("OK");
			done.setActionCommand(Constants.ACTION_EXIT);
			done.addActionListener(this);
			buttonpanel.add(done);
			panel.add(varpanel, BorderLayout.CENTER);
			panel.add(buttonpanel, BorderLayout.SOUTH);
			this.getContentPane().add(panel);
			this.setSize(500, holders.size()*24 + 70);
		}
		public void actionPerformed(ActionEvent e) {
			for (String var : holders) {
				JTextField textfield = textfields.get(var);
				String v = textfield.getText();
				if (v==null) v = "";
				map.put(var, v);
			}
			this.setVisible(false);
		}
		public HashMap<String,String> getParameters() {
			return map;
		}
		
	}
	
}
