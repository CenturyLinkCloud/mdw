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

import com.centurylink.mdw.common.constant.FormConstants;
import com.centurylink.mdw.designer.utils.Constants;
import com.centurylink.mdw.designer.utils.JTablePlus;
import com.centurylink.mdw.designer.utils.SwingFormGenerator;
import com.centurylink.mdw.designer.utils.SwingFormGenerator.MenuButton;
import com.qwest.mbeng.MbengDocument;
import com.qwest.mbeng.MbengNode;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.MaskFormatter;

public class FormWidgetDialog extends JDialog 
        implements ActionListener {

    private static final long serialVersionUID = 1L;
    
    private static final String ACTION_PROMPT_CHANGE = "prompt";
    
    private Map<String,String> validatorMasks;

    private JTextField widgetId;
    private JCheckBox widgetIdAuto = null;
    private JTextField widgetData = null;
    private JTextField widgetLabel;
    private JTextField widgetTip = null;;
    private JTextField widgetRequired = null;
    private JCheckBox widgetCanTypeIn = null;
    private JCheckBox widgetIsStatic = null;
    private JTextField widgetImage = null;
    private JTextField widgetDatePattern = null;
    private JTextField displayCondition = null;
    private JTextField editableCondition = null;
    private List<JTextField> validatorEditors = null;
    private List<JCheckBox> validatorSelectors = null;
    private JTextField autoValue = null;
    private JComboBox commandAction = null;
    private JTextField paginator = null;
    private JTextField choiceListSource = null;
    private JPanel validatorTable = null;
    private JComboBox validatorTypes;
    private JTextField validatorMessage = null;
    private JCheckBox radioVertical = null;
    private JCheckBox widgetIsSortable = null;
    private JCheckBox validateData = null;
    private JCheckBox showBusy = null;
    private JComboBox prompt = null;
    private JTextField promptMessage = null;
    private JTextField promptOptions = null;
    private JComboBox tableStyles;
    private JComboBox tabbingStyles;
    private JComboBox columnEditable = null;
    private JTextField columnStyle = null;
    
    private SwingFormGenerator generator;
    private String label_old;
    
    private Font inputFont;
    private boolean readonly;
    
    private boolean use_mask = false;
    
//    private DesignerPage page;
    private MbengNode node;
    
//    private List<String> validators;
    
    private int y_gap = 5;
    private int label_x = 20;
//    private int value_x = 140;
    private int y_off;

    public FormWidgetDialog(JFrame frame, MbengNode node, SwingFormGenerator generator, boolean readonly) {
        super(frame);
        setModal(true);
        setSize(700,400);
        setLocationRelativeTo(frame);
        this.generator = generator;
//      this.page = page;
        this.node = node;
        this.readonly = readonly;
        init();
    }
    
    public FormWidgetDialog(JDialog owner, MbengNode node, SwingFormGenerator generator, boolean readonly) {
        super(owner);
        setModal(true);
        setSize(700,400);
        setLocationRelativeTo(owner);
        this.generator = generator;
//      this.page = page;
        this.node = node;
        this.readonly = readonly;
        init();
    }
    
    private void init() {
        label_old = node.getAttribute(FormConstants.FORMATTR_LABEL);
        setTitle(node.getName() + " " + node.getAttribute(FormConstants.FORMATTR_ID));

		JPanel panel = new JPanel(null);
        panel.setBounds(200,200,100,100);
        inputFont = new Font("Monospaced", Font.PLAIN, 12);
        validatorMasks = new HashMap<String,String>();
        validatorMasks.put("mask", "mask('_99LLAAXX_')");
        validatorMasks.put("length", "length(_min_,_max_)");
        validatorMasks.put("range", "range(_low_,_high_)");
        validatorMasks.put("int", "int()");
        String v;
        y_off = 20;
        
        // widget label and ID
        JLabel label;
        
        widgetLabel = init_TextField(panel, "Label",
        		FormConstants.FORMATTR_LABEL, label_x, 120, 200, false);
        widgetId = init_TextField(panel, "ID",
        		FormConstants.FORMATTR_ID, 380, 30, 100, false);
        v = node.getAttribute(FormConstants.FORMATTR_ID);
        boolean autoid = (v==null||v.length()==0||v.startsWith("_id_"));
        if (autoid) widgetId.setEditable(false);
        
        widgetIdAuto = new JCheckBox();
        widgetIdAuto.setBounds(520, y_off, 20, 20);
        widgetIdAuto.setSelected(autoid);
        widgetIdAuto.addActionListener(this);
        widgetIdAuto.setEnabled(!readonly);
        widgetIdAuto.setActionCommand(Constants.ACTION_RESET);
        panel.add(widgetIdAuto);
        label = new JLabel("Auto generated");
        label.setBounds(545, y_off, 100, 20);
        panel.add(label);

        y_off += 20 + y_gap;
        
        // widget data association
        if (node.getName().equals(FormConstants.WIDGET_TEXT) 
                || node.getName().equals(FormConstants.WIDGET_DROPDOWN)
                || node.getName().equals(FormConstants.WIDGET_RADIOBUTTONS)
                || node.getName().equals(FormConstants.WIDGET_CHECKBOX)
                || node.getName().equals(FormConstants.WIDGET_TEXTAREA)
                || node.getName().equals(FormConstants.WIDGET_DATE)
                || node.getName().equals(FormConstants.WIDGET_LIST)
                || node.getName().equals(FormConstants.WIDGET_LISTPICKER)
                || node.getName().equals(FormConstants.WIDGET_TABLE)
                || node.getName().equals(FormConstants.WIDGET_COLUMN)
                || node.getName().equals(FormConstants.WIDGET_TABBEDPANE)) {
        	widgetData = init_TextField(panel, "Data",
            		FormConstants.FORMATTR_DATA, 120, 320);
        }
        
        // action
        if (node.getName().equals(FormConstants.WIDGET_BUTTON) 
                || node.getName().equals(FormConstants.WIDGET_COLUMN)
                || node.getName().equals(FormConstants.WIDGET_MENUITEM)
                || node.getName().equals(FormConstants.WIDGET_RADIOBUTTONS)
                || node.getName().equals(FormConstants.WIDGET_CHECKBOX)
                || node.getName().equals(FormConstants.WIDGET_HYPERLINK)
                || node.getName().equals(FormConstants.WIDGET_DROPDOWN)) {
            String[] commands = {
            		"",
            		"_action_?arg1=value1&...&argn=valuen",
            		"dialog_open(_dialogFormName_)",
            		"dialog_ok(_dialogFormName_)",
            		"dialog_cancel(_dialogFormName_)",
            		"validate()",
            		"repaint(_idToRefresh_)",
            		"show_page(_formName_, _inNewWindow_)",
            		"start_process(_processName_)",
            		"task_action(_actionName_)",
            		"perform_action(_action_, _showBusy_)",
            		"ajax_action(_action_,_idToUpdate_)",
            		"ajax_action_async(_action_,_idToUpdate_,_timeoutSec_,_checkInterval_,_callback_)",
            		"hyperlink:task?name=#{this.TaskInstanceId}",
    	        	"hyperlink:form?formName=...&row=#{this.TaskInstanceId}"
            };
            String label_str;
            if (node.getName().equals(FormConstants.WIDGET_COLUMN))
        		label_str = "Hyperlink Action";
        	else if (node.getName().equals(FormConstants.WIDGET_HYPERLINK))
        		label_str = "URL";
        	else label_str = "Action";
            commandAction = init_ComboBox(panel, label_str, 
            		commands, FormConstants.FORMATTR_ACTION, true, 120, 520);
            
            // discard data changes during action
            if (node.getName().equals(FormConstants.WIDGET_BUTTON) 
                    || node.getName().equals(FormConstants.WIDGET_MENUITEM)) {
                validateData = init_CheckBox(panel, "Validate",
            			FormConstants.FORMATTR_VALIDATE, 160);
            }
            // show busy dialog, confirm/input dialog during action
            if (node.getName().equals(FormConstants.WIDGET_BUTTON) 
                    || node.getName().equals(FormConstants.WIDGET_MENUITEM)) {
            	showBusy = init_CheckBox(panel, "Show busy popup",
            			FormConstants.FORMATTR_SHOW_BUSY, 160);
            	label = new JLabel("Prompt");
                label.setBounds(label_x, y_off, 160, 20);
                panel.add(label);
                String[] choices = {
                		FormConstants.FORMATTRVALUE_PROMPT_NONE,
                		FormConstants.FORMATTRVALUE_PROMPT_CONFIRM,
                		FormConstants.FORMATTRVALUE_PROMPT_INPUT,
                		FormConstants.FORMATTRVALUE_PROMPT_SELECT
                };
                prompt = new JComboBox(choices);
                prompt.setBounds(140, y_off, 80, 20);
                v = node.getAttribute(FormConstants.FORMATTR_PROMPT);
                String[] promptParsed = v==null?new String[0]:v.split("\\|");
                String pType = promptParsed.length<1?FormConstants.FORMATTRVALUE_PROMPT_NONE:promptParsed[0];
                prompt.setSelectedItem(pType);
                prompt.setEnabled(!readonly);
                prompt.addActionListener(this);
                prompt.setActionCommand(ACTION_PROMPT_CHANGE);
                panel.add(prompt);
                label = new JLabel("Message");
                label.setBounds(230, y_off, 60, 20);
                panel.add(label);
                promptMessage = new JTextField();
                promptMessage.setBounds(290, y_off, 160, 20);
                String pMsg = promptParsed.length<2?null:promptParsed[1];
                if (pMsg!=null) promptMessage.setText(pMsg);
                if (readonly || pType.equals(FormConstants.FORMATTRVALUE_PROMPT_NONE))
                	promptMessage.setEditable(false);
                panel.add(promptMessage);
                
                label = new JLabel("Choices");
                label.setBounds(460, y_off, 60, 20);
                panel.add(label);
                promptOptions = new JTextField();
                promptOptions.setBounds(520, y_off, 160, 20);
                v = promptParsed.length<3?null:promptParsed[2];
                if (v!=null) promptOptions.setText(v);
                if (readonly || !pType.equals(FormConstants.FORMATTRVALUE_PROMPT_SELECT))
                	promptOptions.setEditable(false);
                panel.add(promptOptions);
                y_off += 20 + y_gap;
                
//                promptOptions = init_TextField(panel, "Choices",
//            			FormConstants.FORMATTRVALUE_PROMPT_SELECT, 460, 60, 160, true);
            }
        }
        
        // required condition
        if (node.getName().equals(FormConstants.WIDGET_TEXT) 
                || node.getName().equals(FormConstants.WIDGET_TEXTAREA)
                || node.getName().equals(FormConstants.WIDGET_DATE)
                || node.getName().equals(FormConstants.WIDGET_RADIOBUTTONS)
                || node.getName().equals(FormConstants.WIDGET_LIST)
                || node.getName().equals(FormConstants.WIDGET_DROPDOWN)) {        

            label = new JLabel("Required");
            label.setBounds(label_x, y_off, 120, 20);
            panel.add(label);
            v = node.getAttribute(FormConstants.FORMATTR_REQUIRED);
            int k;
            if (v==null||v.equalsIgnoreCase("false")) k = 0;
            else if (v.equalsIgnoreCase("true")) k = 1;
            else k = 2;
            String[] options = {"False", "True", "Conditional"};
            JComboBox requiredCombo = new JComboBox(options);
            requiredCombo.setBounds(140, y_off, 75, 22);
            requiredCombo.setSelectedIndex(k);
            if (readonly) requiredCombo.setEnabled(false);
            else {
            	requiredCombo.addActionListener(this);
            	requiredCombo.setActionCommand(Constants.ACTION_COMBO);
            }
            panel.add(requiredCombo);
            widgetRequired = new JTextField();
            widgetRequired.setBounds(220, y_off, 420, 20);
            if (v!=null) widgetRequired.setText(v);
            panel.add(widgetRequired);
            widgetRequired.setEditable(!readonly && k==2);
            y_off += 20 + y_gap;
            
            // auto value
            autoValue = init_TextField(panel, "Default Value",
        			FormConstants.FORMATTR_AUTOVALUE, 120, 520);
        }
        
        // display condition
        displayCondition = init_TextField(panel, "Visible when",
    			FormConstants.FORMATTR_VISIBLE, 120, 520);
            
        // modifiable condition
        if (node.getName().equals(FormConstants.WIDGET_TEXT) 
                || node.getName().equals(FormConstants.WIDGET_RADIOBUTTONS)
                || node.getName().equals(FormConstants.WIDGET_CHECKBOX)
                || node.getName().equals(FormConstants.WIDGET_TEXTAREA)
                || node.getName().equals(FormConstants.WIDGET_DATE)
                || node.getName().equals(FormConstants.WIDGET_TABBEDPANE) 
                || node.getName().equals(FormConstants.WIDGET_DROPDOWN) 
                || node.getName().equals(FormConstants.WIDGET_BUTTON)) {
            editableCondition = init_TextField(panel, 
            		node.getName().equals(FormConstants.WIDGET_BUTTON)?"Enabled when":"Modifiable when",
        			FormConstants.FORMATTR_EDITABLE, 120, 520);
        }

        // widget specific stuff
        if (node.getName().equals(FormConstants.WIDGET_TEXT)) {
            label = new JLabel("Validators");
            label.setBounds(label_x, y_off, 120, 20);
            panel.add(label);
            validatorTable = new JPanel(null);
            createValidatorTable(stringToList(node.getAttribute(FormConstants.FORMATTR_VALIDATORS),';'));
            JScrollPane scrollPane = new JScrollPane(validatorTable);
            scrollPane.setBounds(140, y_off, 350, 90);
            panel.add(scrollPane);
            JButton button_delete = new JButton("Delete Checked");
            button_delete.setBounds(510, y_off+5, 130, 25);
            button_delete.setActionCommand(Constants.ACTION_DELETE);
            button_delete.setEnabled(!readonly);
            button_delete.addActionListener(this);
            panel.add(button_delete);
            JButton button_add = new JButton("Add Following");
            button_add.setBounds(510, y_off+35, 130, 25);
            button_add.setActionCommand(Constants.ACTION_NEW);
            button_add.addActionListener(this);
            button_add.setEnabled(!readonly);
            panel.add(button_add);
            validatorTypes = new JComboBox();
            Set<String> validatorNames = validatorMasks.keySet();
            for (String one : validatorNames) {
                validatorTypes.addItem(one);
            }
            validatorTypes.setBounds(510, y_off+65, 120, 25);
            panel.add(validatorTypes);
            y_off += 90 + y_gap;
            
            validatorMessage = init_TextField(panel, "Error Message",
        			FormConstants.FORMATTR_INVALID_MSG, 120, 520);
        } else if (node.getName().equals(FormConstants.WIDGET_DROPDOWN)) {
        	widgetCanTypeIn = init_CheckBox(panel, "Can type in value",
        			FormConstants.FORMATTR_CAN_TYPE_IN, 120);
        	choiceListSource = init_TextField(panel, "Choices",
        			FormConstants.FORMATTR_CHOICES, 120, 520);
        } else if (node.getName().equals(FormConstants.WIDGET_DATE)) {
            widgetCanTypeIn = init_CheckBox(panel, "Can type in value",
        			FormConstants.FORMATTR_CAN_TYPE_IN, 120);
            widgetDatePattern = init_TextField(panel, "Date Pattern",
        			FormConstants.FORMATTR_DATE_PATTERN, 120, 160);
        } else if (node.getName().equals(FormConstants.WIDGET_TEXTAREA)) {   
            widgetIsStatic = init_CheckBox(panel, "Is Static",
        			FormConstants.FORMATTR_IS_STATIC, 120);
        } else if (node.getName().equals(FormConstants.WIDGET_BUTTON)) {
                widgetImage = init_TextField(panel, "Image",
            			FormConstants.FORMATTR_IMAGE, 120, 160);
        } else if (node.getName().equals(FormConstants.WIDGET_HYPERLINK)) {
            widgetImage = init_TextField(panel, "Image",
        			FormConstants.FORMATTR_IMAGE, 120, 160);
        } else if (node.getName().equals(FormConstants.WIDGET_RADIOBUTTONS)) {
        	choiceListSource = init_TextField(panel, "Choices",
        			FormConstants.FORMATTR_CHOICES, 120, 520);
        	radioVertical = init_CheckBox(panel, "Vertical",
        			FormConstants.FORMATTR_DIRECTION, 120);
        } else if (node.getName().equals(FormConstants.WIDGET_LIST)) {
        	choiceListSource = init_TextField(panel, "Choices",
        			FormConstants.FORMATTR_CHOICES, 120, 520);		
        } else if (node.getName().equals(FormConstants.WIDGET_LISTPICKER)) {    
            choiceListSource = init_TextField(panel, "Choices",
        			FormConstants.FORMATTR_CHOICES, 120, 520);
        } else if (node.getName().equals(FormConstants.WIDGET_MENU)) {
            MenuEditor menueditor = new MenuEditor(node, (JComponent)generator.getWidget(node));
            menueditor.setBounds(label_x, y_off, 600, 240);
            panel.add(menueditor);
            y_off += 240 + y_gap;
        } else if (node.getName().equals(FormConstants.WIDGET_TABLE)) {
        	String[] choices = {
        			FormConstants.FORMATTRVALUE_TABLESTYLE_SCROLLED,
        			FormConstants.FORMATTRVALUE_TABLESTYLE_PAGINATED,
        			FormConstants.FORMATTRVALUE_TABLESTYLE_SIMPLE };
        	tableStyles = init_ComboBox(panel, "Table Style", 
        			choices, FormConstants.FORMATTR_TABLE_STYLE, false, 120, 160);
        	paginator = init_TextField(panel, "Table Action Class",
        			FormConstants.FORMATTR_ACTION, 120, 360);
        } else if (node.getName().equals(FormConstants.WIDGET_TABBEDPANE)) {
        	String[] choices = {
        			FormConstants.FORMATTRVALUE_TABBINGSTYLE_CLIENT,
        			FormConstants.FORMATTRVALUE_TABBINGSTYLE_AJAX,
        			FormConstants.FORMATTRVALUE_TABBINGSTYLE_SERVER,
        			FormConstants.FORMATTRVALUE_TABBINGSTYLE_JQUERY };
        	tabbingStyles = init_ComboBox(panel, "Tabbing Style", 
        			choices, FormConstants.FORMATTR_TABBING_STYLE, false, 120, 160);
        	paginator = init_TextField(panel, "Tabbing class (not used for client tabbing style)",
        			FormConstants.FORMATTR_ACTION, 280, 360);
        } else if (node.getName().equals(FormConstants.WIDGET_COLUMN)) {
        	widgetIsSortable = init_CheckBox(panel, "Sortable",
        			FormConstants.FORMATTR_SORTABLE, 120);
        	String[] choices = { "true", "false", "when new" };
        	columnEditable = init_ComboBox(panel, "Editable", 
        			choices, FormConstants.FORMATTR_EDITABLE, false, 120, 160);
        	columnStyle = init_TextField(panel, "Column Detail Style",
        			FormConstants.FORMATTR_COLUMN_STYLE, 120, 400);
        }
        
        // tip
        if (node.getName().equals(FormConstants.WIDGET_TEXT) 
                || node.getName().equals(FormConstants.WIDGET_DROPDOWN)
                || node.getName().equals(FormConstants.WIDGET_RADIOBUTTONS)
                || node.getName().equals(FormConstants.WIDGET_CHECKBOX)
                || node.getName().equals(FormConstants.WIDGET_TEXTAREA)
                || node.getName().equals(FormConstants.WIDGET_DATE)
                || node.getName().equals(FormConstants.WIDGET_BUTTON)
                || node.getName().equals(FormConstants.WIDGET_LIST)) {
        	widgetTip = init_TextField(panel, "Tip", 
        			FormConstants.FORMATTR_TIP, 120, 320);
        }
        
        // buttons
        JButton button_ok = new JButton("OK");
        button_ok.setBounds(120, 320, 120, 25);
        button_ok.setActionCommand(Constants.ACTION_SAVE);
        button_ok.addActionListener(this);
        button_ok.setEnabled(!readonly);
        panel.add(button_ok);
        JButton button_cancel = new JButton("Cancel");
        button_cancel.setBounds(260, 320, 120, 25);
        button_cancel.setActionCommand(Constants.ACTION_EXIT);
        button_cancel.addActionListener(this);
        panel.add(button_cancel);
        JButton button_delete = new JButton("Delete");
        button_delete.setBounds(400, 320, 120, 25);
        button_delete.setActionCommand(Constants.ACTION_DELETE);
        button_delete.addActionListener(this);
        button_delete.setEnabled(false);
        panel.add(button_delete);
        
        getContentPane().add(panel);
    }
    
    private JTextField init_TextField(JPanel panel,
    		String label, String attr, int lx, int lw, int vw, boolean newline) {
    	JLabel jlabel = new JLabel(label);
        jlabel.setBounds(lx, y_off, lw, 20);
        panel.add(jlabel);
        JTextField widget = new JTextField();
        widget.setBounds(lx+lw, y_off, vw, 20);
        widget.setText(node.getAttribute(attr));
        widget.setEditable(!readonly);
        panel.add(widget);
        if (newline) y_off += 20 + y_gap;
        return widget;
    }
    
    private JTextField init_TextField(JPanel panel,
    		String label, String attr, int lw, int vw) {
    	return init_TextField(panel, label, attr, label_x, lw, vw, true);
    }
    
    private JComboBox init_ComboBox(JPanel panel,
    		String label, String[] choices, String attr, boolean editable, int lw, int vw) {
    	JLabel jlabel = new JLabel(label);
        jlabel.setBounds(label_x, y_off, lw, 20);
        panel.add(jlabel);
        JComboBox widget = new JComboBox(choices);
        if (editable) widget.setEditable(true);
        widget.setBounds(label_x+lw, y_off, vw, 25);
        widget.setEnabled(!readonly);
        String v = node.getAttribute(attr);
        if (v!=null) widget.setSelectedItem(v);
        panel.add(widget);
        y_off += 25 + y_gap;
        return widget;
    }
    
    private JCheckBox init_CheckBox(JPanel panel, 
    		String label, String attr, int lw) {
    	JLabel jlabel = new JLabel(label);
        jlabel.setBounds(label_x, y_off, lw, 20);
        panel.add(jlabel);
        JCheckBox widget = new JCheckBox();
        widget.setBounds(label_x+lw, y_off, 20, 20);
        String v = node.getAttribute(attr);
        widget.setSelected(v!=null && v.equalsIgnoreCase("true"));
        widget.setEnabled(!readonly);
        panel.add(widget);
        y_off += 20 + y_gap;
        return widget;
    }
    
    private int createOneValidator(String value, int y_off) {
    	JTextField ftt;
    	if (use_mask) {
	        String mask = value;
	        int m = mask.length();
	        for (int j=0; j<m; j++) {
	            if (!Character.isLetter(mask.charAt(j))) {
	                mask = mask.substring(0,j);
	                break;
	            }
	        }
	        try {
	            MaskFormatter fmter = new MaskFormatter(validatorMasks.get(mask));
	            fmter.setPlaceholderCharacter('_');
	            ftt = new JFormattedTextField(fmter);
	        } catch (Exception e) {
	            ftt = new JFormattedTextField();
	        }
    	} else {
    		ftt = new JTextField();
    	}
        ftt.setText(value);
        ftt.setFont(inputFont);
        ftt.setBounds(2, y_off, 290, 20);
        JCheckBox sel = new JCheckBox();
        sel.setBounds(300, y_off, 20, 20);
        validatorTable.add(sel);
        y_off += 22;
        validatorTable.add(ftt);
        validatorEditors.add(ftt);
        validatorSelectors.add(sel);
        return y_off;
    }
    
    private void createValidatorTable(List<String> validators) {
        int y_off = 2;
        validatorEditors = new ArrayList<JTextField>();
        validatorSelectors = new ArrayList<JCheckBox>();
        for (int i=0; i<validators.size(); i++) {
            y_off = createOneValidator(validators.get(i), y_off);
        }
        Dimension size = new Dimension(330, y_off+2);
        validatorTable.setPreferredSize(size);
        validatorTable.setSize(size);
    }
    
    private void refreshValidatorTable() {
        int y_off = 2;
        for (int i=0; i<validatorEditors.size(); i++) {
            JTextField ftt = validatorEditors.get(i);
            JCheckBox sel = validatorSelectors.get(i);
            ftt.setBounds(2, y_off, 290, 20);
            sel.setBounds(300, y_off, 20, 20);
            y_off += 22;
        }
        Dimension size = new Dimension(330, y_off+2);
        validatorTable.setPreferredSize(size);
        validatorTable.setSize(size);
        validatorTable.repaint();
    }
    
    private void updateWidgetLabel(MbengNode node) {
        String label_new = node.getAttribute(FormConstants.FORMATTR_LABEL);
        if (label_new!=null && !label_new.equals(label_old)) {
            if (node.getName().equals(FormConstants.WIDGET_PANEL)) {
                JPanel panel = (JPanel)generator.getWidget(node);
                ((TitledBorder)panel.getBorder()).setTitle(label_new);
            } else if (node.getName().equals(FormConstants.WIDGET_BUTTON)) {
                JButton button = (JButton)generator.getWidget(node);
                button.setText(label_new);
            } else if (node.getName().equals(FormConstants.WIDGET_MENU)) {
                Component menu = generator.getWidget(node);
                if (menu instanceof JPopupMenu) ((JPopupMenu)menu).setName(label_new);
                else if (menu instanceof MenuButton) ((MenuButton)menu).setText(label_new);
                else ((JMenu)menu).setText(label_new);
            } else if (node.getName().equals(FormConstants.WIDGET_MENUITEM)) {
                JMenuItem menuitem = (JMenuItem)generator.getWidget(node);
                menuitem.setText(label_new);
            } else if (node.getName().equals(FormConstants.WIDGET_TAB)) {
                JPanel panel = (JPanel)generator.getWidget(node);;
                JTabbedPane tabbedPane = (JTabbedPane)panel.getParent();
                for (int i=0; i<tabbedPane.getTabCount(); i++) {
                    Component comp = tabbedPane.getComponent(i);
                    if (comp==panel) {
                        tabbedPane.setTitleAt(i, label_new);
                        break;
                    }
                }
            } else if (node.getName().equals(FormConstants.WIDGET_COLUMN)) {
            	JTablePlus tablePlus = (JTablePlus)generator.getWidget(node.getParent());
                int c = Integer.parseInt(node.getAttribute("INDEX"));
                tablePlus.setColumnLabel(c, label_new);
            } else {
                JLabel label = generator.getLabel(node);	
                if (label!=null) {
                	label.setText(generator.formatText(label_new));
                }
            }
        }
    }
    
    private void switchTextAreaWidget(MbengNode node) {
        String is_static = node.getAttribute(FormConstants.FORMATTR_IS_STATIC);
        Component widget = generator.getWidget(node);
        JLabel label = generator.getLabel(node);
        String label_text = node.getAttribute(FormConstants.FORMATTR_LABEL);
        String data = node.getAttribute(FormConstants.FORMATTR_DATA);
        String defval = node.getAttribute(FormConstants.FORMATTR_AUTOVALUE);
        if (is_static!=null && is_static.equalsIgnoreCase("true")) {
        	if (widget.isVisible()) {	// text area -> label
        		widget.setVisible(false);
        		String v = node.getAttribute(FormConstants.FORMATTR_VX);
                int vx = (v!=null)?Integer.parseInt(v):10;
                v = node.getAttribute(FormConstants.FORMATTR_VY);
                int vy = (v!=null)?Integer.parseInt(v):10;
                v = node.getAttribute(FormConstants.FORMATTR_VW);
                int vw = (v!=null)?Integer.parseInt(v):160;
                v = node.getAttribute(FormConstants.FORMATTR_VH);
                int vh = (v!=null)?Integer.parseInt(v):60;
                label.setBounds(vx, vy, vw, vh);
                if (data!=null && data.length()>0) label.setText("$$." + data);
                else label.setText(generator.formatText(defval));
        	}
        } else {
        	if (!widget.isVisible()) {			// label -> text area
        		String v = node.getAttribute(FormConstants.FORMATTR_LX);
                int lx = v!=null?Integer.parseInt(v):10;
                v = node.getAttribute(FormConstants.FORMATTR_LY);
                int ly = v!=null?Integer.parseInt(v):10;
                v = node.getAttribute(FormConstants.FORMATTR_LW);
                int lw = (v!=null)?Integer.parseInt(v):60;
                v = node.getAttribute(FormConstants.FORMATTR_LH);
                int lh = (v!=null)?Integer.parseInt(v):20;
                label.setBounds(lx, ly, lw, lh);
                label.setText(generator.formatText(label_text));
                widget.setVisible(true);
        	}
        }
    }
    
    public void actionPerformed(ActionEvent event)
    {
	    String cmd = event.getActionCommand();
		if (cmd.equals(Constants.ACTION_SAVE)) {
			boolean auto = widgetIdAuto.isSelected();
			node.setAttribute(FormConstants.FORMATTR_ID, auto?"":widgetId.getText());
			node.setAttribute(FormConstants.FORMATTR_LABEL, widgetLabel.getText());
            if (widgetData!=null)
                node.setAttribute(FormConstants.FORMATTR_DATA, widgetData.getText());
            if (widgetRequired!=null)
                node.setAttribute(FormConstants.FORMATTR_REQUIRED, widgetRequired.getText());
            if (widgetCanTypeIn!=null)
            	node.setAttribute(FormConstants.FORMATTR_CAN_TYPE_IN, widgetCanTypeIn.isSelected()?"TRUE":"FALSE");
            if (autoValue!=null)
            	node.setAttribute(FormConstants.FORMATTR_AUTOVALUE, autoValue.getText());
            if (widgetIsStatic!=null) {
            	node.setAttribute(FormConstants.FORMATTR_IS_STATIC, widgetIsStatic.isSelected()?"TRUE":"FALSE");
            	switchTextAreaWidget(node);
            }
            if (widgetImage!=null) {
            	String v = widgetImage.getText();
            	node.setAttribute(FormConstants.FORMATTR_IMAGE, v);
            	Component widget = generator.getWidget(node);
            	if (widget!=null && widget instanceof JButton) {
            		JButton button = (JButton)widget;
            		if (v==null || v.length()==0) {
            			button.setIcon(null);
            			button.setText(widgetLabel.getText());
            		} else {
                    	try {
                    		button.setText(null);
							button.setIcon(generator.loadIcon(v));
						} catch (Exception e) {
							e.printStackTrace();
						}
            		}
            	}
            }
            if (displayCondition!=null)
                node.setAttribute(FormConstants.FORMATTR_VISIBLE, displayCondition.getText());
            if (editableCondition!=null)
                node.setAttribute(FormConstants.FORMATTR_EDITABLE, editableCondition.getText());
            else if (columnEditable!=null)
            	node.setAttribute(FormConstants.FORMATTR_EDITABLE, (String)columnEditable.getSelectedItem());
            if (validatorTable!=null) {
                List<String> validators = new ArrayList<String>();
                for (JTextField ftt : validatorEditors) {
                    validators.add(ftt.getText());
                }
                node.setAttribute(FormConstants.FORMATTR_VALIDATORS, 
                		listToString(validators, ';'));
            }
            if (commandAction!=null)
                node.setAttribute(FormConstants.FORMATTR_ACTION, (String)commandAction.getSelectedItem());
            if (validateData!=null)
            	node.setAttribute(FormConstants.FORMATTR_VALIDATE, validateData.isSelected()?"true":"false");
            if (showBusy!=null)
            	node.setAttribute(FormConstants.FORMATTR_SHOW_BUSY, showBusy.isSelected()?"true":"false");
            if (prompt!=null) {
            	String pType = (String)prompt.getSelectedItem();
            	String pMsg = promptMessage.getText();
            	String pOptions = promptOptions.getText();
            	String av;
            	if (pType.equals(FormConstants.FORMATTRVALUE_PROMPT_NONE))
            		av = pType;
            	else if (pType.equals(FormConstants.FORMATTRVALUE_PROMPT_CONFIRM))
            		av = pType + "|" + pMsg;
            	else if (pType.equals(FormConstants.FORMATTRVALUE_PROMPT_INPUT))
            		av = pType + "|" + pMsg;
            	else av = pType + "|" + pMsg + "|" + pOptions;
            	node.setAttribute(FormConstants.FORMATTR_PROMPT, av);
            }
            if (choiceListSource!=null)
                node.setAttribute(FormConstants.FORMATTR_CHOICES, choiceListSource.getText());
            if (radioVertical!=null) {
            	node.setAttribute(FormConstants.FORMATTR_DIRECTION, radioVertical.isSelected()?"V":"H");
            }
            if (widgetIsSortable!=null) {
            	node.setAttribute(FormConstants.FORMATTR_SORTABLE, widgetIsSortable.isSelected()?"true":"false");
            }
            if (tableStyles!=null) {
            	node.setAttribute(FormConstants.FORMATTR_TABLE_STYLE, (String)tableStyles.getSelectedItem());
            }
            if (columnStyle!=null) {
            	node.setAttribute(FormConstants.FORMATTR_COLUMN_STYLE, columnStyle.getText());
            }
            if (tabbingStyles!=null) {
            	node.setAttribute(FormConstants.FORMATTR_TABBING_STYLE, (String)tabbingStyles.getSelectedItem());
            }
            if (paginator!=null)
                node.setAttribute(FormConstants.FORMATTR_ACTION, paginator.getText());
            if (validatorMessage!=null)
            	node.setAttribute(FormConstants.FORMATTR_INVALID_MSG, validatorMessage.getText());
			if (widgetTip!=null)
				node.setAttribute(FormConstants.FORMATTR_TIP, widgetTip.getText());
			if (widgetDatePattern!=null)
            	node.setAttribute(FormConstants.FORMATTR_DATE_PATTERN, widgetDatePattern.getText());
			this.updateWidgetLabel(node);
            this.setVisible(false);
        } else if (cmd.equals(Constants.ACTION_EXIT)) {
        	 this.setVisible(false);
        } else if (cmd.equals(Constants.ACTION_DELETE)) {
            for (int i=validatorSelectors.size()-1; i>=0; i--) {
                if (validatorSelectors.get(i).isSelected()) {
                    validatorTable.remove(validatorEditors.remove(i));
                    validatorTable.remove(validatorSelectors.remove(i));
                    break;
                }
            }
            this.refreshValidatorTable();
        } else if (cmd.equals(Constants.ACTION_NEW)) {
            String validatorName = (String)validatorTypes.getSelectedItem();
            if (validatorName!=null) {
            	this.createOneValidator(validatorMasks.get(validatorName), 0);
            	this.refreshValidatorTable();
            }
        } else if (cmd.equals(Constants.ACTION_RESET)) {	// change auto ID
        	boolean auto = widgetIdAuto.isSelected();
        	widgetId.setEditable(!auto);
        	widgetId.setText("");
        } else if (cmd.equals(Constants.ACTION_COMBO)) {
        	JComboBox combo = (JComboBox)event.getSource();
        	int k = combo.getSelectedIndex();
        	if (k==0) widgetRequired.setText(null);
        	else if (k==1) widgetRequired.setText("TRUE");
        	else widgetRequired.setText("please enter condition here");
        	widgetRequired.setEditable(k==2);
        } else if (cmd.equals(ACTION_PROMPT_CHANGE)) {
        	String selected = prompt.getSelectedItem().toString();
        	if (selected.equals(FormConstants.FORMATTRVALUE_PROMPT_SELECT)) {
        		promptMessage.setEditable(true);
        		promptOptions.setEditable(true);
        	} else if (selected.equals(FormConstants.FORMATTRVALUE_PROMPT_NONE)) {
        		promptMessage.setEditable(false);
        		promptOptions.setEditable(false);
        	} else {
        		promptMessage.setEditable(true);
        		promptOptions.setEditable(false);
        	}
        }
    }

    private List<String> stringToList(String inputStr, char delimiter) {
        List<String> list = new ArrayList<String>();
        if (inputStr==null || inputStr.length()==0) return list;
        StringBuffer sb = new StringBuffer();
        int i=0, n = inputStr.length();
        boolean escaped = false;
        char ch;
        while (i<n) {
            ch = inputStr.charAt(i);
            if (escaped) {
                sb.append(ch);
                escaped = false;
            } else if (ch=='\\') {
                escaped = true;
            } else if (ch==delimiter) {
                list.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(ch);
            }
            i++;
        }
        list.add(sb.toString());
        return list;
    }
    
    private String listToString(List<String> list, char delimiter) {
        if (list==null) return null;
        StringBuffer sb = new StringBuffer();
        boolean first = true;
        for (String one : list) {
            if (!first) sb.append(delimiter);
            else first = false;
            int n = one.length();
            for (int i=0; i<n; i++) {
                char ch = one.charAt(i);
                if (ch=='\\' || ch==delimiter) sb.append('\\');
                sb.append(ch);
            }
        }
        return sb.toString();
    }
    
    private class MenuEditor extends JPanel implements ActionListener, ListSelectionListener {
        
        private static final long serialVersionUID = 1L;
        private static final String ACTION_ADD_MENUITEM = "___ADD_MENUITEM___";
        private static final String ACTION_ADD_SUBMENU = "___ADD_SUBMENU___";
//        private static final String ACTION_ADD_DIVIDER = "___ADD_DIVIDER___";
        private static final String ACTION_DELETE_ITEM = "___DELETE_ITEM___";
        private static final String ACTION_MOVE_UP = "___MOVE_UP___";
        private static final String ACTION_MOVE_DOWN = "___MOVE_DOWN___";
        private static final String ACTION_SHOW_SUBMENU = "___SHOW_SUBMENU___";
        private static final String ACTION_ITEM_LABEL = "___ITEM_LABEL___";
        private static final String ACTION_ITEM_ACTION = "___ITEM_ACTION___";
        
        private MbengNode menuNode;
        private JComponent menu;    // JMenu or JPopuMenu
        private JList itemlist;
        private ArrayList<MbengNode> items;
        private JTextField item_label;
        private JTextField item_action;
        private JButton button_up, button_down, button_submenu, button_delete;
        
        MenuEditor(MbengNode menuNode, JComponent menu) {
            super(new BorderLayout());
            this.menuNode = menuNode;
            if (menu instanceof MenuButton) this.menu = ((MenuButton)menu).getMenu();
            else this.menu = menu;
//            this.setResizable(true);
//          layeredPane.setLocation(200, 200);
            this.setBorder(BorderFactory.createTitledBorder("Menu Items"));
//            setSize(new Dimension(300, 310));
//            setBackground(Color.lightGray);
            
            // item attribute panel
            JPanel itemAttrArea = new JPanel(new FlowLayout(FlowLayout.LEFT));
            itemAttrArea.setPreferredSize(new Dimension(340,300));
            itemAttrArea.add(new JLabel("Label"));
            item_label = new JTextField();
            item_label.setPreferredSize(new Dimension(160,20));
            item_label.addActionListener(this);
            item_label.setActionCommand(ACTION_ITEM_LABEL);
            item_label.setEditable(!readonly);
            itemAttrArea.add(item_label);
            itemAttrArea.add(new JLabel("Action"));
            item_action = new JTextField();
            item_action.setPreferredSize(new Dimension(240,20));
            item_action.addActionListener(this);
            item_action.setActionCommand(ACTION_ITEM_ACTION);
            item_action.setEditable(!readonly);
            itemAttrArea.add(item_action);
            this.add(itemAttrArea, BorderLayout.EAST);
            
            JPanel buttonArea = itemAttrArea;
            JButton button;
            button = new JButton("Add Menu Item");
            button.addActionListener(this);
            button.setActionCommand(ACTION_ADD_MENUITEM);
            button.setEnabled(!readonly);
            buttonArea.add(button);
            button = new JButton("Add Sub Menu");
            button.addActionListener(this);
            button.setActionCommand(ACTION_ADD_SUBMENU);
            button.setEnabled(!readonly);
            buttonArea.add(button);
//            button = new JButton("Add Divider");
//            button.addActionListener(this);
//            button.setActionCommand(ACTION_ADD_DIVIDER);
//            buttonArea.add(button);
            button_delete = new JButton("Delete Item");
            button_delete.addActionListener(this);
            button_delete.setActionCommand(ACTION_DELETE_ITEM);
            button_delete.setEnabled(!readonly);
            buttonArea.add(button_delete);
            button_up = new JButton("Move Up");
            button_up.addActionListener(this);
            button_up.setActionCommand(ACTION_MOVE_UP);
            button_up.setEnabled(!readonly);
            buttonArea.add(button_up);
            button_down = new JButton("Move Down");
            button_down.addActionListener(this);
            button_down.setActionCommand(ACTION_MOVE_DOWN);
            button_down.setEnabled(!readonly);
            buttonArea.add(button_down);
            button_submenu = new JButton("Show Sub Menu");
            button_submenu.addActionListener(this);
            button_submenu.setActionCommand(ACTION_SHOW_SUBMENU);
            buttonArea.add(button_submenu);
            
            items = new ArrayList<MbengNode>();
            ListModel mymodel = new AbstractListModel() {
				private static final long serialVersionUID = 1L;
				public int getSize() { return items.size(); }
                public Object getElementAt(int index) {
                    String label = items.get(index).getAttribute(FormConstants.FORMATTR_LABEL);
                    if (items.get(index).getName().equals(FormConstants.WIDGET_MENU))
                        return label + " [menu]";
                    else return label;
                }
            };

            itemlist = new JList(mymodel);
            itemlist.addListSelectionListener(this);
            JScrollPane scrollpane = new JScrollPane(itemlist);
            scrollpane.setPreferredSize(new Dimension(240,120));
            add(scrollpane, BorderLayout.CENTER);

            setLocation(100, 100);
            MbengNode mitemnode = menuNode.getFirstChild();
            items.clear();
            while (mitemnode!=null) {
                items.add(mitemnode);
                mitemnode = mitemnode.getNextSibling();
            }
            itemlist.updateUI();
            enableButtons(-1);
        }

        public void actionPerformed(ActionEvent e) {
            String cmd = e.getActionCommand();
            if (cmd.equals(ACTION_ADD_MENUITEM)) {
                MbengDocument desc_doc = node.getDocument();
                MbengNode newnode = desc_doc.newNode(FormConstants.WIDGET_MENUITEM,
                        null, "X", ' ');
                newnode.setAttribute(FormConstants.FORMATTR_LABEL, "New item");
                menuNode.appendChild(newnode);
                items.add(newnode);
                itemlist.updateUI();
                itemlist.setSelectedIndex(items.size()-1);
                enableButtons(items.size()-1);
                this.repaint();
//                menu.add(new JMenuItem("New item"));
                try {
                	generator.create_component(newnode, 0, 0, 0, menu);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            } else if (cmd.equals(ACTION_ADD_SUBMENU)) {
                MbengDocument desc_doc = node.getDocument();
                MbengNode newnode = desc_doc.newNode(FormConstants.WIDGET_MENU,
                        null, "X", ' ');
                newnode.setAttribute(FormConstants.FORMATTR_LABEL, "New sub menu");
                menuNode.appendChild(newnode);
                items.add(newnode);
                itemlist.updateUI();
                itemlist.setSelectedIndex(items.size()-1);
                enableButtons(items.size()-1);
                this.repaint();
//                menu.add(new JMenu("New sub menu"));
                try {
                	if (menu instanceof MenuButton)
                		 generator.create_component(newnode, 0, 0, 0, ((MenuButton)menu).getMenu());
                	else generator.create_component(newnode, 0, 0, 0, menu);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
//            } else if (cmd.equals(ACTION_ADD_DIVIDER)) {
            } else if (cmd.equals(ACTION_MOVE_UP)) {
                int k = itemlist.getSelectedIndex();
                if (k>0) {
                    MbengNode prev = items.get(k-1);
                    MbengNode mnode = items.get(k);
                    MbengNode parent = mnode.getParent();
                    parent.removeChild(mnode);
                    parent.insertChild(mnode, prev);
                    items.remove(k);
                    items.add(k-1, mnode);
                    itemlist.updateUI();
                    itemlist.setSelectedIndex(k-1);
                    enableButtons(k-1);
                    this.repaint();
                    Component comp = (menu instanceof JMenu)?((JMenu)menu).getMenuComponent(k):
                            ((JPopupMenu)menu).getComponent(k);
                    menu.remove(k);
                    menu.add(comp, k-1);
                }
            } else if (cmd.equals(ACTION_MOVE_DOWN)) {
                int k = itemlist.getSelectedIndex();
                if (k>=0 && k<items.size()-1) {
                    MbengNode next = k<items.size()-2?items.get(k+2):null;
                    MbengNode mnode = items.get(k);
                    MbengNode parent = mnode.getParent();
                    parent.removeChild(mnode);
                    if (next!=null) parent.insertChild(mnode, next);
                    else parent.appendChild(mnode);
                    items.remove(k);
                    items.add(k+1, mnode);
                    itemlist.updateUI();
                    itemlist.setSelectedIndex(k+1);
                    enableButtons(k+1);
                    this.repaint();
                    Component comp = (menu instanceof JMenu)?((JMenu)menu).getMenuComponent(k):
                        ((JPopupMenu)menu).getComponent(k);
                    menu.remove(k);
                    menu.add(comp, k+1);
                }
            } else if (cmd.equals(ACTION_DELETE_ITEM)) {
                int k = itemlist.getSelectedIndex();
                if (k>=0 && k<items.size()) {
                    MbengNode mnode = items.get(k);
                    MbengNode parent = mnode.getParent();
                    parent.removeChild(mnode);
                    items.remove(k);
                    itemlist.updateUI();
                    itemlist.setSelectedIndex(-1);
                    enableButtons(-1);
                    this.repaint();
                    generator.deleteNode(parent, mnode);
//                    Component comp = menu.getMenuComponent(k);
//                    menu.remove(k);
                }
            } else if (cmd.equals(ACTION_SHOW_SUBMENU)) {
                int k = itemlist.getSelectedIndex();
                if (k>=0 && k<items.size()) {
                    MbengNode mnode = items.get(k);
                    if (mnode.getName().equals(FormConstants.WIDGET_MENU)) {
                        FormWidgetDialog dialog = new FormWidgetDialog(FormWidgetDialog.this,
                        		mnode, generator, readonly);
                        dialog.setVisible(true);
//                        this.show(mnode, submenu);
                        // TODO implement above
                    }
                }
            } else if (cmd.equals(ACTION_ITEM_LABEL)) {
                int k = itemlist.getSelectedIndex();
                if (k>=0 && k<items.size()) {
                    String label = item_label.getText();
                    MbengNode mnode = items.get(k);
                    mnode.setAttribute(FormConstants.FORMATTR_LABEL, label);
                    itemlist.updateUI();
                    updateWidgetLabel(mnode);
                }
            } else if (cmd.equals(ACTION_ITEM_ACTION)) {
                int k = itemlist.getSelectedIndex();
                if (k>=0 && k<items.size()) {
                    String action = item_action.getText();
                    MbengNode mnode = items.get(k);
                    mnode.setAttribute(FormConstants.FORMATTR_ACTION, action);
                    itemlist.updateUI();
                }
            }
        }

        public void valueChanged(ListSelectionEvent e) {
            int k = itemlist.getSelectedIndex();
            if (!e.getValueIsAdjusting()) {
                MbengNode mnode = items.get(k);
                item_label.setText(mnode.getAttribute(FormConstants.FORMATTR_LABEL));
                item_action.setText(mnode.getAttribute(FormConstants.FORMATTR_ACTION));
                enableButtons(k);
            }
            
        }
        
        private void enableButtons(int k) {
            MbengNode mnode = k>=0?items.get(k):null;
            boolean isSubmenu = k>=0&&mnode.getName().equals(FormConstants.WIDGET_MENU);
            button_submenu.setEnabled(isSubmenu);
            button_up.setEnabled(k>0&&!readonly);
            button_down.setEnabled(k<items.size()-1&&!readonly);
            button_delete.setEnabled(k>=0&&!readonly);
            item_label.setEditable(k>=0&&!readonly);
            item_action.setEditable(k>=0 && !isSubmenu && !readonly);
        }
    }
    
}
