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
package com.centurylink.mdw.designer.pages;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.centurylink.mdw.common.constant.FormConstants;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.common.utilities.form.CallURL;
import com.centurylink.mdw.common.utilities.form.FormActionParser;
import com.centurylink.mdw.designer.DesignerDataAccess;
import com.centurylink.mdw.designer.utils.Constants;
import com.centurylink.mdw.designer.utils.JTablePlus;
import com.centurylink.mdw.designer.utils.SwingFormGenerator;
import com.centurylink.mdw.designer.utils.SwingFormGenerator.MenuButton;
import com.centurylink.mdw.designer.utils.SwingFormGenerator.SelectOption;
import com.centurylink.mdw.model.FormDataDocument;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.qwest.mbeng.DomDocument;
import com.qwest.mbeng.FormatDom;
import com.qwest.mbeng.MbengException;
import com.qwest.mbeng.MbengNode;

public class FormPanel extends JPanel implements ActionListener,FocusListener {

	private Window frame;
	private FormDesignCanvas canvas;
	private DesignerDataAccess dao;

	private DomDocument formdoc;
	private FormDataDocument datadoc;
	private boolean exit_copy;		// true when exiting a dialog with OK
	private String privileges;

	public FormPanel(Window frame, DesignerDataAccess dao, String privileges) {
		this.frame = frame;
		this.dao = dao;
		this.exit_copy = true;
		this.privileges = privileges;
	}

	private void performJavaScriptAction(ActionEvent event, Component src,
			String cmd, MbengNode node) {
		String action = node.getAttribute(FormConstants.FORMATTR_ACTION);
		if (cmd.equals(SwingFormGenerator.ACTION_DROPDOWN)) {
			Object selected = ((JComboBox)src).getSelectedItem();
			if (selected!=null && selected instanceof SelectOption) {
				this.setDataValue(node, ((SelectOption)selected).getValue());
			}
		} else if (cmd.equals(SwingFormGenerator.ACTION_RADIOBUTTON)) {
			this.setDataValue(node, ((JRadioButton)src).getName());
		} else if (cmd.equals(SwingFormGenerator.ACTION_CHECKBOX)) {
			String av = ((JCheckBox)src).getName();
			String[] choices = av.split(",");
			this.setDataValue(node, ((JCheckBox)src).isSelected()?choices[1]:choices[0]);
		}
		try {
			FormActionParser command = new FormActionParser(action);
			String functionName = command.getFunctionName();
			if (functionName.equals("dialog_open")) {
				try {
                    String formname = command.getArgument(0);
                    show_dialog(formname, true);
                } catch (Exception e) {
                    e.printStackTrace();
                	show_error("Failed to load script " + e.getMessage());
                }
			} else if (functionName.equals("dialog_ok")) {
                frame.setVisible(false);
			} else if (functionName.equals("dialog_cancel")) {
            	exit_copy = false;
                frame.setVisible(false);
			} else if (functionName.equals("hyperlink")) {
				this.launchBrowser(command.getArgument(0));
			} else if (functionName.equals("validate")) {
			} else if (functionName.equals("repaint")) {
			} else if (functionName.equals("task_action")) {
			} else if (functionName.equals("perform_action")) {
                performGenericAction(command.getArgument(0));
			} else if (functionName.equals("ajax_action")) {
                performGenericAction(command.getArgument(0));
			} else if (functionName.equals("ajax_action_async")) {
                performGenericAction(command.getArgument(0));
			} else if (functionName.equals("show_page")) {
				boolean inNewWindow = "true".equalsIgnoreCase(command.getArgument(1));
                String formname = command.getArgument(0);
				if (inNewWindow) {
					try {
						show_dialog(formname, false);
					} catch (Exception e) {
						e.printStackTrace();
						show_error("Failed to load script " + e.getMessage());
					}
				} else {
					try {
	                    this.resetData(formname, null);
	                } catch (Exception e) {
	                    e.printStackTrace();
	                	show_error("Failed to load script " + e.getMessage());
	                }
				}
			} else if (functionName.equals("start_process")) {
				action = FormConstants.ACTION_START_PROCESS + "?" + FormDataDocument.META_PROCESS_NAME
					+ "=" + command.getArgument(0);
				if (command.getArgumentCount()>=2 && command.getArgument(1).length()>0)
					action += "&" + FormDataDocument.META_MASTER_REQUEST_ID + "=" + command.getArgument(1);
				performGenericAction(action);
			} else if (functionName.equals("table_row_view")) {
				String tableId = command.getArgument(0);
//				String dialogId = command.getArgument(1); TODO implement custom dialog
				MbengNode tableNode = canvas.getGenerator().getNodeById(tableId);
				if (tableNode==null) throw new Exception("Table node does not exist - id=" + tableId);
				JTablePlus table = (JTablePlus)canvas.getGenerator().getWidget(tableNode);
                int row = table.getSelectedRow();
                if (row<0) throw new Exception("You need to select a row");
                TableRowDialog dialog = new TableRowDialog(tableNode, table, row);
                dialog.setVisible(true);
			} else if (functionName.equals("table_row_new")) {
				String tableId = command.getArgument(0);
//				String dialogId = command.getArgument(1); TODO implement custom dialog
				MbengNode tableNode = canvas.getGenerator().getNodeById(tableId);
				if (tableNode==null) throw new Exception("Table node does not exist - id=" + tableId);
				JTablePlus table = (JTablePlus)canvas.getGenerator().getWidget(tableNode);
                TableRowDialog dialog = new TableRowDialog(tableNode, table, -1);
                dialog.setVisible(true);
			} else if (functionName.equals("table_row_delete")) {
				String tableId = command.getArgument(0);
//				String dialogId = command.getArgument(1); TODO implement custom dialog
				MbengNode tableNode = canvas.getGenerator().getNodeById(tableId);
				if (tableNode==null) throw new Exception("Table node does not exist - id=" + tableId);
				JTablePlus table = (JTablePlus)canvas.getGenerator().getWidget(tableNode);
                String datapath = tableNode.getAttribute(FormConstants.FORMATTR_DATA);
                String paginator = tableNode.getAttribute(FormConstants.FORMATTR_ACTION);
                String av = tableNode.getAttribute(FormConstants.FORMATTR_TABLE_STYLE);
            	boolean isPaginated = FormConstants.FORMATTRVALUE_TABLESTYLE_PAGINATED.equals(av);
                boolean useMetaData = true;
				if (useMetaData) {
	                action = paginator + "?action=deleterow&table=" + datapath
	            			+ "&meta=" + datapath  + "_META&topage="
	            			+ (isPaginated?"S":"R");
				} else {
	                int row = table.getSelectedRow();
	                if (row<0) throw new Exception("You need to select a row");
	                action = paginator + "?action=deleterow&table=" + datapath
	            			+ "&meta=" + datapath  + "_META&row=" + row + "&topage="
	            			+ (isPaginated?"S":"R");
				}
				performGenericAction(action);
			} else if (functionName.equals("switch_server")) {
				String server_name = datadoc.getValue(command.getArgument(0));
				server_name = server_name.replace(" port ", ":");
				String serverUrl = dao.getCurrentServer().getServerUrl();
				int k1 = serverUrl.indexOf("://");
				int k2 = serverUrl.indexOf("/", k1+4);
				String newServerUrl = k2>0 ?
							(serverUrl.substring(0,k1+3)+server_name+serverUrl.substring(k2)) :
								(serverUrl.substring(0,k1+3)+server_name);
				dao.getCurrentServer().setServerUrl(newServerUrl);
				performGenericAction(command.getArgument(1));
			} else throw new Exception("Unknown action function " + functionName);
		} catch (Exception e) {
			if (! (action==null||action.length()==0) ||
				! (cmd.equals(SwingFormGenerator.ACTION_DROPDOWN) || cmd.equals(SwingFormGenerator.ACTION_RADIOBUTTON)))
				show_error(e.getMessage());
		}

	}

	private void performGenericAction(String action) {
		try {
			datadoc.setAttribute(FormDataDocument.ATTR_ACTION, action);
			datadoc.setAttribute(FormDataDocument.ATTR_FORM, formdoc.getId());
			datadoc.setMetaValue(FormDataDocument.META_PRIVILEGES, privileges);
			datadoc.setMetaValue(FormDataDocument.META_USER, dao.getCuid());
			// clear errors
			datadoc.clearErrors();
			String request = datadoc.format();
			String response = dao.engineCall(request);
//			System.out.println("Response: " + response);

			datadoc.load(response);

			List<String> errors = datadoc.getErrors();
			MbengNode node1;
			if (!datadoc.getRootNode().getKind().equals(FormDataDocument.KIND_FORMDATA)) {
				node1 = datadoc.getRootNode().getFirstChild();
				while (node1!=null && errors.size()==0) {
					if (node1.getKind().contains("StatusMessage")) {
						errors.add("SUCCESS".equals(node1.getValue()) ? "Unexpected server response." : node1.getValue());
					}
					node1 = node1.getNextSibling();
				}
				if (errors.size()==0) errors.add("Unknown error from engine");
				datadoc.load(request);	// change back to form data document
			}
			if (errors.size()>0) {
				show_errors(errors);
			} else {
				String additionalAction = datadoc.getAttribute(FormDataDocument.ATTR_ACTION);
				CallURL callurl = additionalAction==null?null:new CallURL(additionalAction);
				if (callurl==null) {
					String newFormName = datadoc.getAttribute(FormDataDocument.ATTR_FORM);
					resetData(newFormName, null);
				} else if (callurl.getAction().equals(FormConstants.ACTION_PROMPT)) {
					String message = datadoc.getMetaValue(FormDataDocument.META_PROMPT);
					if (message!=null) {
						show_info(message);
					}
					String newFormName = datadoc.getAttribute(FormDataDocument.ATTR_FORM);
					resetData(newFormName, null);
				} else if (callurl.getAction().equals(FormConstants.ACTION_DIALOG)) {
					String formName = callurl.getParameter(FormConstants.URLARG_FORMNAME);
					if (formName.startsWith(FormConstants.TABLE_ROW_DIALOG_PREFIX)) {
						String tableId = formName.substring(FormConstants.TABLE_ROW_DIALOG_PREFIX.length());
						MbengNode tableNode = canvas.getGenerator().getNodeById(tableId);
						JTablePlus table = (JTablePlus)canvas.getGenerator().getWidget(tableNode);
		                int row = table.getSelectedRow();
		                if (row<0) throw new Exception("You need to select a row");
		                TableRowDialog dialog = new TableRowDialog(tableNode, table, row);
		                dialog.setVisible(true);
					} else {
						show_dialog(formName, true);
					}
				} else if (callurl.getAction().equals(FormConstants.ACTION_OK)) {
					String message = datadoc.getMetaValue(FormDataDocument.META_PROMPT);
					if (message!=null) show_info(message);
					frame.setVisible(false);
				} else if (callurl.getAction().equals(FormConstants.ACTION_CANCEL)) {
					exit_copy = false;
					frame.setVisible(false);
				} else {
					String newFormName = datadoc.getAttribute(FormDataDocument.ATTR_FORM);
					resetData(newFormName, null);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			show_error(ex.getMessage());
		}
	}

	private DomDocument loadForm(String formname) throws Exception {
	    if (dao.isVcsPersist())
	        formname = formname + ".xml";
        RuleSetVO ruleset = dao.getRuleSet(formname, RuleSetVO.FORM, 0);
        String content;
        if (ruleset==null) {
            if (dao.isVcsPersist()) {
                content = dao.getServerResourceRest(formname);
            } else {
            	StringBuffer request = new StringBuffer();
        		request.append("<_mdw_get_resource>");
        		request.append("<name>").append(formname).append("</name>");
        		request.append("<language>").append(RuleSetVO.FORM).append("</language>");
        		request.append("</_mdw_get_resource>");
        		content = dao.engineCall(request.toString());
        		if (content.startsWith("ERROR:"))
        			throw new Exception("Failed to load form " + formname + " - " + content);
            }
        } else content = ruleset.getRuleSet();
        FormatDom fmter = new FormatDom();
        DomDocument formdoc = new DomDocument();
        fmter.load(formdoc, content);
        return formdoc;
	}

    /**
	 * this is invoked when coming from script list page or package tree
	 */
	public void setData(String formname, String data) throws Exception {
        try {
            formdoc = loadForm(formname);
            datadoc = new FormDataDocument();
            if (data!=null && data.length()>0) datadoc.load(data);
            canvas = new FormDesignCanvas(frame, this, dao);
            canvas.setFormXml(formdoc, datadoc);
            JScrollPane canvasScrollpane = new JScrollPane(canvas);
            MbengNode rootnode = canvas.getFormXml().getRootNode();
            String av = rootnode.getAttribute(FormConstants.FORMATTR_VW);
            int w = (av==null)?800:Integer.parseInt(av);
            av = rootnode.getAttribute(FormConstants.FORMATTR_VH);
            int h = (av==null)?600:Integer.parseInt(av);
            this.add(canvasScrollpane,BorderLayout.CENTER);
            Container content_pane;
            if (frame instanceof JDialog) {
            	content_pane = ((JDialog)frame).getContentPane();
            } else {
            	content_pane = ((JFrame)frame).getContentPane();
            }
            content_pane.add(this);
            frame.setSize(w+10, h+30);
        } catch (Exception e) {
            e.printStackTrace();
            show_error("failed to set data for canvas: " + e.getMessage());
            throw e;
        }
	}

    /**
	 * this is invoked when coming from script list page or package tree
	 */
	private void resetData(String formname, String data) {
        try {
        	if (formname!=null) formdoc = loadForm(formname);
            if (data!=null) {
            	datadoc.load(data);
            }
            canvas.setFormXml(formdoc, datadoc);
            frame.repaint();
            // TODO set title
        } catch (Exception e) {
            e.printStackTrace();
            show_error("failed to set data for canvas: " + e.getMessage());
        }
	}

    private void show_errors(List<String> msgs) {
    	StringBuffer sb = new StringBuffer();
        for (int i=0; i<msgs.size(); i++) sb.append(msgs.get(i)).append('\n');
        JOptionPane.showMessageDialog(frame, sb.toString(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void show_error(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void show_info(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    public void setDataValue(MbengNode formnode, String value) {
    	String dataname = formnode.getAttribute(FormConstants.FORMATTR_DATA);
    	if (dataname==null || dataname.length()==0) return;
    	try {
			datadoc.setValue(dataname, value, "FIELD");
		} catch (MbengException e) {
			e.printStackTrace();
		}
    }

    private void show_dialog(String formname, boolean modal) {
    	try {
    		if (modal) {
	            JDialog testdialog = new JDialog((JFrame)frame, "MDW Form Dialog");
	            FormPanel formpanel = new FormPanel(testdialog, dao, privileges);
	            formpanel.setData(formname, datadoc.format());
	            testdialog.setLocationRelativeTo(frame);
	            testdialog.setModal(true);
	            testdialog.setVisible(true);
	            if (formpanel.exit_copy) {
	            	resetData(null, formpanel.datadoc.format());
	            }
    		} else {
    			JFrame testframe = new JFrame("MDW Form Window");
    			FormPanel formpanel = new FormPanel(testframe, dao, privileges);
	            formpanel.setData(formname, datadoc.format());
	            testframe.setVisible(true);
    		}
        } catch (Exception e) {
            e.printStackTrace();
            show_error("Failed to load script " + e.getMessage());
        }
    }

	public DesignerDataAccess getDesignerDataAccess() {
		return dao;
	}

    public void launchBrowser(String url) {
        try {
            dao.launchBrowser(url);
        } catch (Exception ex) {
        	ex.printStackTrace();
        	show_error("cannot launch browser - " + ex.getMessage());
        }
    }

    public void focusGained(FocusEvent e) {
    }

    public void focusLost(FocusEvent e) {
        Component src = e.getComponent();
        if (src instanceof JTextField) {
            JTextField textfield = (JTextField)src;
            MbengNode node = canvas.getGenerator().getNode(src);
            if (node!=null) setDataValue(node, textfield.getText());
        }
    }

    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        Component src = (Component)e.getSource();
        if (cmd.equals(FormConstants.ACTION_MENU)) {
        	MenuButton button = (MenuButton)src;
        	JPopupMenu menu = button.getMenu();
        	menu.show(src, src.getWidth()-10, src.getHeight()-10);
        } else if (cmd.equals(SwingFormGenerator.ACTION_TABBING)) {
        	JTabbedPane tabbedPane = (JTabbedPane)src;
        	MbengNode node = canvas.getGenerator().getNode(tabbedPane);
            String id = node.getAttribute(FormConstants.FORMATTR_ID);
            String datapath = node.getAttribute(FormConstants.FORMATTR_DATA);
            if (datapath==null || datapath.length()==0) datapath = "__mdwtabindex__" + id;
            String paginator = node.getAttribute(FormConstants.FORMATTR_ACTION);
        	if (paginator==null) paginator = "com.centurylink.mdw.listener.formaction.TabChanger";
        	int index = e.getID();
            String action = paginator + "?action=tabbing&tabs=" + id
        			+ "&tab=" + index  + "&data=" + datapath;
            performGenericAction(action);
        } else if (cmd.equals(JTablePlus.ACTION_ENGINE_CALL)) {
        	String action = ((JTablePlus)src).getEngineCallAction();
            performGenericAction(action);
        } else {
            MbengNode node = canvas.getGenerator().getNode(src);
        	performJavaScriptAction(e, src, cmd, node);
    	}
    }

	class TableRowDialog extends JDialog implements ActionListener {
		private static final long serialVersionUID = 1L;
		private MbengNode tableDefNode;
		private JTablePlus table;
		private int row;
		private List<Component> fields;

		TableRowDialog(MbengNode tableDefNode, JTablePlus table, int row) {
			super(frame);
			this.table = table;
			this.row = row;
			this.tableDefNode = tableDefNode;
	        setTitle("Table Row View Dialog");
	        setModal(true);
			JPanel panel = new JPanel(null);
			int y = 20;

	        boolean isNew = row < 0;
	        MbengNode rownode;
	        if (!isNew) {
                String datapath = tableDefNode.getAttribute(FormConstants.FORMATTR_DATA);
	        	try {
					MbengNode tableDataNode = datadoc.setTable(null, datapath, false);
					rownode = getRow(tableDataNode, row);
				} catch (MbengException e) {
					e.printStackTrace();
					rownode = null;
				}
	        } else rownode = null;
	        JLabel label;
	        MbengNode column;
	        fields = new ArrayList<Component>();
	        for (column=tableDefNode.getFirstChild(); column!=null; column=column.getNextSibling()) {
	        	label = new JLabel(column.getAttribute(FormConstants.FORMATTR_LABEL));
				label.setBounds(20, y, 150, 20);
				panel.add(label);
	        	String style_str = column.getAttribute(FormConstants.FORMATTR_COLUMN_STYLE);
	        	Map<String,String> styles = StringHelper.parseMap(style_str);
	        	String editable_str = column.getAttribute(FormConstants.FORMATTR_EDITABLE);
	        	boolean editable;
	    		if ("true".equalsIgnoreCase(editable_str)) editable =  true;
	    		else if (isNew && "when new".equalsIgnoreCase(editable_str)) editable = true;
	    		else editable = false;
	    		String dataname = column.getAttribute(FormConstants.FORMATTR_DATA);
	    		String data = (dataname==null||rownode==null)?null:datadoc.getValue(rownode, dataname);
	        	Component widget;
	        	String style = styles.get("style");
	        	if ("textarea".equalsIgnoreCase(style)) {
	        		String v = styles.get("height");
	        		int height = v==null?60:Integer.parseInt(v);
	        		JTextArea textarea = new JTextArea();
	        		JScrollPane pane = new JScrollPane(textarea);
					if (!editable) textarea.setEditable(false);
					if (data!=null) textarea.setText(data);
					pane.setBounds(170, y, 350, height);
					widget = textarea;
					panel.add(pane);
	        		y += height + 5;
	        	} else {	// text
					JTextField textfield = new JTextField();
					if (!editable) textfield.setEditable(false);
					if (data!=null) textfield.setText(data);
					textfield.setBounds(170, y, 350, 20);
					widget = textfield;
					panel.add(widget);
					y += 25;
	        	}
	        	fields.add(widget);
	        	widget.setName(column.getAttribute(FormConstants.FORMATTR_DATA));
	        }

	        // command buttons
	        y += 10;
	        JButton button_save = new JButton("OK");
	        button_save.setBounds(140, y, 120, 25);
	        button_save.setActionCommand(Constants.ACTION_SAVE);
	        button_save.addActionListener(this);
	        panel.add(button_save);
	        JButton button_cancel = new JButton("Cancel");
	        button_cancel.setBounds(300, y, 120, 25);
	        button_cancel.setActionCommand(Constants.ACTION_EXIT);
	        button_cancel.addActionListener(this);
	        panel.add(button_cancel);

	        setSize(540, y+60);
	        panel.setSize(540, y+40);
	        setLocationRelativeTo(frame);

	        getContentPane().add(panel);
		}

		private MbengNode getRow(MbengNode tableNode, int index) {
			int i = 0;
			for (MbengNode r=tableNode.getFirstChild(); r!=null; r=r.getNextSibling()) {
				if (i==index) return r;
				i++;
			}
			return null;
		}

		public void actionPerformed(ActionEvent e) {
	        String cmd = e.getActionCommand();
	        if (cmd.equals(Constants.ACTION_SAVE)) {
                String datapath = tableDefNode.getAttribute(FormConstants.FORMATTR_DATA);
                String av = tableDefNode.getAttribute(FormConstants.FORMATTR_TABLE_STYLE);
            	boolean isPaginated = FormConstants.FORMATTRVALUE_TABLESTYLE_PAGINATED.equals(av);
                try {
					MbengNode tableDataNode = datadoc.setTable(null, datapath, false);
				    String paginator = tableDefNode.getAttribute(FormConstants.FORMATTR_ACTION);
					MbengNode rownode;
					String action;
					if (row<0) {
						row = table.getRowCount();
						rownode = datadoc.addRow(tableDataNode);
						action = paginator + "?action=insertrow&table=" + datapath
								+ "&meta=" + datapath  + "_META&row=" + row + "&topage="
								+ (isPaginated?"S":"R");
					} else {
						rownode = getRow(tableDataNode, row);
						action = paginator + "?action=updaterow&table=" + datapath
							+ "&meta=" + datapath  + "_META&row=" + row + "&topage="
							+ (isPaginated?"S":"R");
					}
					for (int j=0; j<fields.size(); j++) {
						String value;
						Component widget = fields.get(j);
						if (widget instanceof JTextArea) {
							value = ((JTextArea)widget).getText();
						} else {
							value = ((JTextField)widget).getText();
						}
						datadoc.setCell(rownode, widget.getName(), value);
					}
					performGenericAction(action);
					this.setVisible(false);
				} catch (MbengException e1) {
					e1.printStackTrace();
					show_error("failed to save table row - " + e1.getMessage());
				}
	        } else if (cmd.equals(Constants.ACTION_EXIT)) {
	        	this.setVisible(false);
	        }
		}

	}


    // TODO a. mask and validators b. ajax repaint c. red asterick d. disable button
    // f. pick list data h. selected tab

    // bugs: red and blue static text does not show

}