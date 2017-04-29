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
package com.centurylink.mdw.plugin.designer.properties.editor;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import com.centurylink.mdw.common.Compatibility;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.utilities.StringHelper;
import com.centurylink.mdw.model.data.task.TaskCategory;
import com.centurylink.mdw.model.value.event.BamMessageDefinition;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.DesignerProxy;
import com.centurylink.mdw.plugin.designer.PluginDataAccess;
import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.AttributeHolder;
import com.centurylink.mdw.plugin.designer.model.Transition;
import com.centurylink.mdw.plugin.designer.model.WorkflowAsset;
import com.centurylink.mdw.plugin.designer.model.WorkflowElement;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.properties.convert.ParameterizedValueConverter;
import com.centurylink.mdw.plugin.designer.properties.convert.ParameterizedValueConverter.ComboParameter;
import com.centurylink.mdw.plugin.designer.properties.convert.ValueConverter;
import com.centurylink.mdw.plugin.designer.properties.editor.ListComposer.IMutableContentProvider;
import com.centurylink.mdw.plugin.designer.properties.editor.TimeInterval.Units;
import com.centurylink.mdw.plugin.project.model.WorkflowProject;
import com.qwest.mbeng.MbengNode;

import net.sf.ecl.datepicker.DatePicker;

public class PropertyEditor {
    public static final int COLUMNS = 4;
    public static final int LABEL_WIDTH = 110;
    public static final int DEFAULT_VALUE_WIDTH = 400;

    public static final String TYPE_TEXT = "TEXT";
    public static final String TYPE_COMBO = "DROPDOWN";
    public static final String TYPE_IMAGE_COMBO = "IMAGE_DROPDOWN";
    public static final String TYPE_LINK = "HYPERLINK";
    public static final String TYPE_INFO = "NOTE";
    public static final String TYPE_SCRIPT = "RULE"; // not here; see ScriptSection
    public static final String TYPE_RADIO = "SELECT";
    public static final String TYPE_CHECKBOX = "BOOLEAN";
    public static final String TYPE_SWITCH = "SWITCH";
    public static final String TYPE_BUTTON = "BUTTON";
    public static final String TYPE_PICKLIST = "LIST";
    public static final String TYPE_TIMER = "DATETIME";
    public static final String TYPE_SPINNER = "SPINNER";
    public static final String TYPE_DIALOG = "DIALOG";
    public static final String TYPE_DATE_PICKER = "DATE";
    public static final String TYPE_BAM_MESSAGE = "BAM_MESSAGE";
    public static final String TYPE_SEPARATOR = "SEPARATOR";
    public static final String TYPE_PARAMETERIZED_COMBO = "PARAMETERIZED_COMBO";
    public static final String TYPE_WEB = "WEB";

    public static final String DATE_FORMAT = "EEE MMM dd HH:mm:ss zzz yyyy";

    // special sections specified in attribute xml
    public static final String SECTION_VARIABLES = "Variables";
    public static final String SECTION_INDEXES = "Indexes";
    public static final String SECTION_WORKGROUPS = "Workgroups";
    public static final String SECTION_NOTICES = "Notices";
    public static final String SECTION_BINDINGS = "Bindings";
    public static final String SECTION_RECIPIENTS = "Recipients";
    public static final String SECTION_CC_RECIPIENTS = "CC Recipients";
    public static final String SECTION_EVENTS = "Events";
    public static final String SECTION_PROCESSES = "Processes";
    public static final String SECTION_CUSTOM = "Custom";

    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private String label;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
        if (type.equals(TYPE_LINK) && widget != null) {
            // allows dynamic link text
            Link link = (Link) widget;
            if (getLabel().indexOf("<A>") >= 0)
                link.setText(getLabel());
            else
                link.setText(" <A>" + getLabel() + "</A>");
        }
        else if (type.equals(TYPE_BUTTON) && widget != null) {
            // allows dynamic button label
            Button button = (Button) widget;
            button.setText(getLabel());
        }
        else if (labelWidget != null && !labelWidget.isDisposed()) {
            labelWidget.setText(label);
            labelWidget.pack();
        }
    }

    private String comment;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    private String section;

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    private int style;

    public int getStyle() {
        return style;
    }

    public void setStyle(int style) {
        this.style = style;
    }

    private FontData font;

    public FontData getFont() {
        return font;
    }

    public void setFont(FontData font) {
        this.font = font;
    }

    private Color background;

    public Color getBackground() {
        return background;
    }

    public void setBackground(Color background) {
        this.background = background;
    }

    private int width = DEFAULT_VALUE_WIDTH;

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    private int height = SWT.DEFAULT;

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    private int indent;

    public int getIndent() {
        return indent;
    }

    public void setIndent(int indent) {
        this.indent = indent;
    }

    private int verticalIndent;

    public int getVerticalIndent() {
        return verticalIndent;
    }

    public void setVerticalIndent(int vi) {
        this.verticalIndent = vi;
    }

    private boolean readOnly;

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    private boolean multiLine;

    public boolean isMultiLine() {
        return multiLine;
    }

    public void setMultiLine(boolean multiLine) {
        this.multiLine = multiLine;
    }

    private int textLimit;

    public int getTextLimit() {
        return textLimit;
    }

    public void setTextLimit(int textLimit) {
        this.textLimit = textLimit;
    }

    private String source;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    private String scriptType;

    public String getScriptType() {
        return scriptType;
    }

    private String scriptLanguages; // comma-separated

    public String getScriptLanguages() {
        return scriptLanguages;
    }

    protected String value;

    public String getValue() {
        return value;
    }

    private String defaultValue;

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    private int span;

    public int getSpan() {
        return span;
    }

    public void setSpan(int span) {
        this.span = span;
    }

    private boolean fireDirtyStateChange = true;

    public boolean isFireDirtyStateChange() {
        return fireDirtyStateChange;
    }

    public void setFireDirtyStateChange(boolean fire) {
        this.fireDirtyStateChange = fire;
    }

    public void setValue(Object value) {
        if (valueConverter == null)
            throw new UnsupportedOperationException(
                    "Requires a value converter for: " + Object.class);

        if (value == null)
            setValue((String) null);
        else
            setValue(valueConverter.toPropertyValue(value));

        updateWidget(this.value);
    }

    /**
     * Set the value based on a string.
     */
    public void setValue(String value) {
        this.value = value;
        updateWidget(value);
    }

    /**
     * Set the value and units.
     */
    public void setValue(String value, Units units) {
        this.value = value;
        if (units != null)
            this.units = units;
        updateWidget(value);
    }

    /**
     * Set the value based on a long (eg: id); cannot be null.
     */
    public void setValue(Long longValue) {
        String newValue = longValue == null ? "-1" : longValue.toString();
        setValue(newValue);
    }

    /**
     * Set the value based on an int; zero means null.
     */
    public void setValue(int intValue) {
        String newValue = intValue == 0 ? null : String.valueOf(intValue);
        setValue(newValue);
    }

    /**
     * Set the value based on a boolean.
     */
    public void setValue(boolean booleanValue) {
        String newValue = String.valueOf(booleanValue);
        setValue(newValue);
    }

    /**
     * Set the value based on an activity attribute.
     */
    public void setValue(Activity activity) {
        if (mbengNode != null && !type.equals(TYPE_LINK) && !type.equals(TYPE_INFO)) {
            String newValue = activity.getAttribute(mbengNode.getAttribute("NAME"));

            if (type.equals(TYPE_TIMER)) {
                Units newUnits = Units.Minutes;
                String unitsAttrName = mbengNode.getAttribute("UNITS_ATTR");
                if (unitsAttrName != null) {
                    String unitsAttrVal = activity.getAttribute(unitsAttrName);
                    if (unitsAttrVal != null)
                        newUnits = Units.valueOf(unitsAttrVal);
                }
                else {
                    String slaUnitsVal = activity.getAttribute(WorkAttributeConstant.SLA_UNIT);
                    if (slaUnitsVal != null)
                        newUnits = Units.valueOf(slaUnitsVal);
                }

                // figure out how to set the units value for MDW 4
                if (!activity.getProject().isMdw5() && activity.isEventWait()) {
                    int legacySla = activity.getNode().nodet.getSla(); // MDW3

                    if (legacySla != 0) {
                        String slaSpecial = activity.getAttribute("$+SLA");
                        if (slaSpecial != null) {
                            double factor = legacySla / Integer.parseInt(slaSpecial);
                            if (factor == 60)
                                newUnits = Units.Minutes;
                            else if (factor == 3600)
                                newUnits = Units.Hours;
                            else if (factor == 86400)
                                newUnits = Units.Days;
                        }
                        else {
                            // last ditch -- fall back to designer classic logic
                            if (legacySla >= 86400 && legacySla % 86400 == 0) {
                                newUnits = Units.Days;
                                newValue = Integer.toString(legacySla / 86400);
                            }
                            else if (legacySla >= 3600 && legacySla % 3600 == 0) {
                                newUnits = Units.Hours;
                                newValue = Integer.toString(legacySla / 3600);
                            }
                            else if (legacySla >= 60 && legacySla % 60 == 0) {
                                newUnits = Units.Minutes;
                                newValue = Integer.toString(legacySla / 60);
                            }
                            else {
                                newUnits = Units.Seconds;
                                newValue = Integer.toString(legacySla);
                            }
                        }
                    }
                }

                setValue(newValue, newUnits);
            }
            else {
                if (newValue != null && valueConverter != null)
                    newValue = valueConverter.toPropertyValue(newValue);
                value = newValue;
                updateWidget(value);
            }
        }
    }

    /**
     * Set the value based on a workflow asset.
     */
    public void setValue(WorkflowAsset asset) {
        if (mbengNode != null && !type.equals(TYPE_LINK) && !type.equals(TYPE_INFO)) {
            String newValue = asset.getAttribute(mbengNode.getAttribute("NAME"));

            if (type.equals(TYPE_TIMER)) {
                String unitsAttrName = mbengNode.getAttribute("UNITS_ATTR");
                String unitsAttr = unitsAttrName == null ? Units.Seconds.toString()
                        : asset.getAttribute(unitsAttrName);
                Units newUnits = unitsAttr == null ? Units.Minutes : Units.valueOf(unitsAttr);
                setValue(newValue, newUnits);
            }
            else {
                if (newValue != null && valueConverter != null)
                    newValue = valueConverter.toPropertyValue(newValue);
                value = newValue;
                updateWidget(value);
            }
        }
    }

    private int minValue;

    public int getMinValue() {
        return minValue;
    }

    public void setMinValue(int minVal) {
        this.minValue = minVal;
        if (widget instanceof Spinner)
            ((Spinner) widget).setMinimum(minVal);
    }

    private int maxValue;

    public int getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(int maxVal) {
        this.maxValue = maxVal;
        if (widget instanceof Spinner)
            ((Spinner) widget).setMaximum(maxVal);
    }

    private Units units = TimeInterval.Units.Minutes;

    public Units getUnits() {
        return units;
    }

    public void setUnits(Units units) {
        this.units = units;
    }

    private Units[] acceptedUnits = { Units.Seconds, Units.Minutes, Units.Hours, Units.Days };

    public Units[] getAcceptedUnits() {
        return acceptedUnits;
    }

    public void setAcceptedUnits(Units[] accepted) {
        this.acceptedUnits = accepted;
    }

    public void setAcceptedUnits(String unitsString) {
        String[] unitsArr = unitsString.split(",");
        acceptedUnits = new Units[unitsArr.length];
        for (int i = 0; i < unitsArr.length; i++)
            acceptedUnits[i] = Units.valueOf(unitsArr[i]);
    }

    private List<String> valueOptions;

    public List<String> getValueOptions() {
        return valueOptions;
    }

    public void setValueOptions(List<String> valueOptions) {
        this.valueOptions = valueOptions;
        if (TYPE_COMBO.equals(getType()) && widget != null) {
            Combo combo = (Combo) widget;
            combo.removeAll();
            for (String option : valueOptions)
                combo.add(option);
            if (valueOptions.size() > 0)
                combo.select(0);
        }
    }

    public void addValueOption(String newValue) {
        if (valueOptions.contains(newValue))
            return;
        valueOptions.add(newValue);
        if (TYPE_COMBO.equals(getType()) && widget != null)
            ((Combo) widget).add(newValue);
    }

    public void removeValueOption(String toRemove) {
        if (!valueOptions.contains(toRemove))
            return;
        valueOptions.remove(toRemove);
        if (TYPE_COMBO.equals(getType()) && widget != null)
            ((Combo) widget).remove(toRemove);
    }

    private List<Image> imageOptions;

    public List<Image> getImageOptions() {
        return imageOptions;
    }

    public void setImageOptions(List<Image> imageOptions) {
        this.imageOptions = imageOptions;
        if (TYPE_IMAGE_COMBO.equals(getType()) && widget != null) {
            ImageCombo imageCombo = (ImageCombo) widget;
            imageCombo.removeAll();
            for (int i = 0; i < valueOptions.size(); i++)
                imageCombo.add(valueOptions.get(i), imageOptions.get(i));
            if (valueOptions.size() > 0)
                imageCombo.select(0);
        }
    }

    private WorkflowElement element;

    public WorkflowElement getElement() {
        return element;
    }

    public void setElement(WorkflowElement element) {
        this.element = element;
    }

    public WorkflowProject getProject() {
        return element.getProject();
    }

    public DesignerProxy getDesignerProxy() {
        return getProject().getDesignerProxy();
    }

    public PluginDataAccess getDataAccess() {
        return getProject().getDataAccess();
    }

    private MbengNode mbengNode;

    public MbengNode getMbengNode() {
        return mbengNode;
    }

    private Control widget;

    public Control getWidget() {
        return widget;
    }

    public void setWidget(Control widget) {
        this.widget = widget;
    }

    private Label labelWidget;

    public Label getLabelWidget() {
        return labelWidget;
    }

    private Label commentWidget;

    public Label getCommentWidget() {
        return commentWidget;
    }

    private Label commentSpacer;

    public void updateWidget(String value) {
        String newValue = value == null ? "" : value;

        if (type.equals(TYPE_TEXT))
            ((Text) widget).setText(newValue);
        else if (type.equals(TYPE_COMBO))
            ((Combo) widget).setText(newValue);
        else if (type.equals(TYPE_IMAGE_COMBO))
            ((ImageCombo) widget).setText(newValue);
        else if (type.equals(TYPE_INFO))
            ((Label) widget).setText(newValue);
        else if (type.equals(TYPE_PICKLIST))
            ((ListComposer) widget).setInput(newValue);
        else if (type.equals(TYPE_RADIO))
            updateRadioButtons(newValue);
        else if (type.equals(TYPE_TIMER))
            updateTimerInterval(newValue);
        else if (type.equals(TYPE_SPINNER))
            ((Spinner) widget).setSelection(
                    newValue == null || newValue.length() == 0 ? 0 : Integer.parseInt(newValue));
        else if (type.equals(TYPE_CHECKBOX))
            ((Button) widget).setSelection(Boolean.parseBoolean(newValue));
        else if (type.equals(TYPE_SWITCH))
            ((SwitchButton) widget).setSelection(Boolean.parseBoolean(newValue));
        else if (type.equals(TYPE_DATE_PICKER)) {
            try {
                Calendar calendar = Calendar.getInstance();
                if (newValue.length() == 0) {
                    ((DatePicker) widget).setSelection(null);
                }
                else {
                    calendar.setTime(new SimpleDateFormat(DATE_FORMAT).parse(newValue));
                    ((DatePicker) widget).setSelection(calendar);
                }
            }
            catch (ParseException ex) {
                throw new RuntimeException(ex.getMessage(), ex);
            }
        }
        else if (type.equals(TYPE_BAM_MESSAGE)) {
            BamEventComposer bamComposer = (BamEventComposer) widget;
            bamComposer.setElement(getElement());
            bamComposer.setInput(valueConverter.toModelValue(newValue));
        }
        else if (type.equals(TYPE_PARAMETERIZED_COMBO)) {
            ((ParameterizedCombo) widget).setInput(valueConverter.toModelValue(newValue));
        }
        else if (type.equals(TYPE_WEB)) {
            Composite c = (Composite) widget;
            // Browser browser =
            // (Browser)((Composite)sc.getChildren()[0]).getChildren()[0];
            Browser browser = (Browser) c.getChildren()[0];
            if (newValue.startsWith("http://") || newValue.startsWith("https://"))
                browser.setUrl(newValue);
            else
                browser.setText(newValue);
        }
    }

    private void updateRadioButtons(String newValue) {
        Group group = (Group) widget;
        for (Control control : group.getChildren()) {
            if (control instanceof Button) {
                Button button = (Button) control;
                button.setSelection(newValue.equals(button.getText()));
            }
        }
    }

    private void updateTimerInterval(String newValue) {
        TimeInterval timeInterval = (TimeInterval) widget;
        String interval = newValue == null || newValue.length() == 0 ? "0" : newValue;
        TimeInterval.TimerValue timerValue = timeInterval.new TimerValue(interval, units);
        timeInterval.setValue(timerValue);
    }

    public PropertyEditor(WorkflowElement workflowElement, MbengNode mbengNode) {
        this.element = workflowElement;
        this.mbengNode = mbengNode;
        if ("TEXT".equals(mbengNode.getName()) && mbengNode.getAttribute("UNITS_ATTR") != null)
            type = TYPE_TIMER;
        else
            type = mbengNode.getName();
        name = mbengNode.getAttribute("NAME");
        section = mbengNode.getAttribute("SECTION");
        if (type.equals(TYPE_LINK)) {
            String url = mbengNode.getAttribute("URL");
            if (url != null)
                this.value = url;
            else
                this.type = TYPE_TEXT;
        }
        label = type.equals(TYPE_LINK) ? mbengNode.getValue() : mbengNode.getAttribute("LABEL");
        if (label == null)
            label = name;
        String vw = mbengNode.getAttribute("VW");
        if (vw != null)
            width = Integer.parseInt(vw);
        String vh = mbengNode.getAttribute("VH");
        if (vh != null)
            height = Integer.parseInt(vh);
        String editable = mbengNode.getAttribute("EDITABLE");
        if (editable != null && !editable.equalsIgnoreCase("true"))
            readOnly = true;
        String readOnlyAttr = mbengNode.getAttribute("READONLY");
        if (readOnlyAttr != null)
            readOnly = Boolean.parseBoolean(readOnlyAttr);
        comment = mbengNode.getAttribute("COMMENT");
        source = mbengNode.getAttribute("SOURCE");
        if (getType().equals(TYPE_INFO))
            this.value = mbengNode.getValue();

        if (type.equals(TYPE_TIMER) && mbengNode.getAttribute("UNITS") != null)
            setAcceptedUnits(mbengNode.getAttribute("UNITS"));

        String defaultValue = mbengNode.getAttribute("DEFAULT");
        if (defaultValue != null) {
            if (workflowElement instanceof Activity) {
                Activity activity = (Activity) workflowElement;
                if (type.equals(TYPE_TIMER)) {
                    this.units = Units.valueOf(defaultValue);
                    if (activity.getAttribute(mbengNode.getAttribute("UNITS_ATTR")) == null)
                        activity.setAttribute(mbengNode.getAttribute("UNITS_ATTR"), defaultValue);
                }
                else {
                    this.value = defaultValue;
                    if (activity.getAttribute(mbengNode.getAttribute("NAME")) == null)
                        activity.setAttribute(mbengNode.getAttribute("NAME"), this.value);
                }
            }
        }

        // value options
        if (getType().equals(TYPE_COMBO) || getType().equals(TYPE_PICKLIST)) {
            if (source == null) {
                valueOptions = new ArrayList<String>();
                for (MbengNode optionNode = mbengNode
                        .getFirstChild(); optionNode != null; optionNode = optionNode
                                .getNextSibling()) {
                    valueOptions.add(optionNode.getValue());
                    if (optionNode.getAttribute("PARAMETER") != null
                            || type.equals(TYPE_PARAMETERIZED_COMBO)) {
                        type = TYPE_PARAMETERIZED_COMBO;
                        if (valueConverter == null)
                            valueConverter = new ParameterizedValueConverter(
                                    (AttributeHolder) workflowElement);

                        ParameterizedValueConverter parameterizedValueConverter = (ParameterizedValueConverter) valueConverter;
                        String option = optionNode.getValue();
                        String paramName = optionNode.getAttribute("PARAMETER");
                        String paramSource = optionNode.getAttribute("SOURCE");
                        ComboParameter comboParam = parameterizedValueConverter.new ComboParameter(
                                paramName,
                                paramSource == null ? null : optionNode.getAttribute("TYPE"));
                        parameterizedValueConverter.putOption(option, comboParam);

                        if (parameterizedValueConverter.getOptions().size() < valueOptions.size()) {
                            // previous valueOptions did not have parameters
                            List<String> optsToAdd = new ArrayList<String>();
                            for (int i = 0; i < valueOptions.size() - 1; i++)
                                optsToAdd.add(0, valueOptions.get(i));

                            for (String opt : optsToAdd)
                                parameterizedValueConverter.putOption(opt, null);
                        }
                    }
                }
            }
            else if (source.equals("Process")) {
                valueOptions = getDataAccess().getProcessNames(false);
            }
            else if (source.equals("ProcessVersion")) {
                valueOptions = new ArrayList<String>(); // to be populated
                                                        // dynamically
            }
            else if (source.equals("TaskCategory")) {
                List<String> codesWithNames = new ArrayList<String>();
                for (TaskCategory taskCat : getDataAccess().getTaskCategories(false))
                    codesWithNames.add(taskCat.getCode() + " - " + taskCat.getName());
                valueOptions = codesWithNames;
            }
            else if (source.equals("UserGroup")) {
                valueOptions = getProject().getDesignerDataModel().getWorkgroupNames();
            }
            else if (source.equals("Activities")) {
                if (!(workflowElement instanceof Activity))
                    throw new PropertyEditorException(
                            "Activities source only valid for Activity element.");

                Activity activity = (Activity) workflowElement;
                valueOptions = activity.getUpstreamActivityNames();
                // hard-wire the attribute value since synced activities are
                // read-only
                String syncedActivities = "";
                for (int i = 0; i < valueOptions.size(); i++) {
                    syncedActivities += valueOptions.get(i);
                    if (i < valueOptions.size() - 1)
                        syncedActivities += "#";
                }
                activity.setAttribute("Synced Activities", syncedActivities);
            }
            else if (source.equals("Variables") || source.equals("DocumentVariables")) {
                WorkflowProcess process = getProcess();
                if (process == null)
                    throw new PropertyEditorException(
                            "Variables source only valid for Process, Activity or Transition elements.");

                if (source.equals("DocumentVariables"))
                    valueOptions = process.getDocRefVariableNames();
                else
                    valueOptions = process.getVariableNames();
            }
            else if (source.equals("RuleSets")) {
                // to be dynamically determined in WorkflowAssetEditor
            }
            else {
                // unknown source -- just present empty options
                valueOptions = new ArrayList<String>();
            }
        }
        else if (getType().equals(TYPE_RADIO)) {
            valueOptions = new ArrayList<String>();
            for (MbengNode optionNode = mbengNode
                    .getFirstChild(); optionNode != null; optionNode = optionNode
                            .getNextSibling()) {
                valueOptions.add(optionNode.getValue());
            }
        }
        else if (getType().equals(TYPE_SCRIPT)) {
            scriptType = mbengNode.getAttribute("TYPE");
            scriptLanguages = mbengNode.getAttribute("LANGUAGES");
        }
        else if (getType().equals(TYPE_TEXT)) {
            String multi = mbengNode.getAttribute("MULTILINE");
            if (multi != null && multi.equalsIgnoreCase("true")) {
                multiLine = true;
                if (height == SWT.DEFAULT)
                    height = 50;
            }
        }
    }

    public PropertyEditor(WorkflowElement workflowElement, String type) {
        this.element = workflowElement;
        this.type = type;
    }

    /**
     * Creates the operating system resources for the widget specific to the
     * type of this property editor. Note: Rendering for Script/RULE type
     * property editors are handled in the ScriptSection.
     */
    public void render(Composite parent) {
        if (type.equals(TYPE_TEXT))
            widget = createTextInput(parent);
        else if (type.equals(TYPE_COMBO))
            widget = createCombo(parent);
        else if (type.equals(TYPE_IMAGE_COMBO))
            widget = createImageCombo(parent);
        else if (type.equals(TYPE_LINK))
            widget = createLink(parent);
        else if (type.equals(TYPE_INFO))
            widget = createInfo(parent);
        else if (type.equals(TYPE_RADIO))
            widget = createRadio(parent);
        else if (type.equals(TYPE_BUTTON))
            widget = createButton(parent);
        else if (type.equals(TYPE_PICKLIST))
            widget = createPickList(parent);
        else if (type.equals(TYPE_TIMER))
            widget = createTimer(parent);
        else if (type.equals(TYPE_CHECKBOX))
            widget = createCheckbox(parent);
        else if (type.equals(TYPE_SWITCH))
            widget = createSwitch(parent);
        else if (type.equals(TYPE_SPINNER))
            widget = createSpinner(parent);
        else if (type.equals(TYPE_DATE_PICKER))
            widget = createDatePicker(parent);
        else if (type.equals(TYPE_BAM_MESSAGE))
            widget = createBamEventComposer(parent);
        else if (type.equals(TYPE_SEPARATOR))
            widget = createSeparator(parent);
        else if (type.equals(TYPE_PARAMETERIZED_COMBO))
            widget = createParameterizedCombo(parent);
        else if (type.equals(TYPE_WEB))
            widget = createBrowser(parent);
    }

    public void setEnabled(boolean enabled) {
        if (widget != null)
            widget.setEnabled(enabled);
        if (type.equals(TYPE_RADIO)) {
            Group group = (Group) widget;
            for (Control control : group.getChildren()) {
                if (control instanceof Button)
                    ((Button) control).setEnabled(enabled);
            }
        }
    }

    public void setEditable(boolean editable) {
        if (widget instanceof Text)
            ((Text) widget).setEditable(editable);
        else if (widget instanceof Composer)
            ((Composer) widget).setEditable(editable);
        else if (!(widget instanceof Label))
            setEnabled(editable);
    }

    public void setVisible(boolean visible) {
        if (widget != null)
            widget.setVisible(visible);
        if (labelWidget != null)
            labelWidget.setVisible(visible);
        if (commentWidget != null)
            commentWidget.setVisible(visible);
    }

    public void setFocus() {
        if (widget != null)
            widget.forceFocus();
    }

    public void dispose() {
        disposeWidget();
        valueChangeListeners = new ListenerList();
    }

    public void disposeWidget() {
        if (widget != null && !widget.isDisposed())
            widget.dispose();
        if (labelWidget != null && !labelWidget.isDisposed())
            labelWidget.dispose();
        if (commentWidget != null && !commentWidget.isDisposed()) {
            commentSpacer.dispose();
            commentWidget.dispose();
        }
    }

    protected void createLabel(Composite parent) {
        createLabel(parent, verticalIndent == 0 ? 2 : verticalIndent);
    }

    protected void createComment(Composite parent) {
        commentSpacer = new Label(parent, SWT.NONE);
        commentWidget = new Label(parent, SWT.NONE);
        if (getComment() != null && getComment().trim().length() > 0)
            commentWidget.setText(getComment());
    }

    protected Label createSeparator(Composite parent) {
        createLabel(parent);

        Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridData gridData = new GridData();
        gridData.widthHint = width;
        gridData.heightHint = 3;
        gridData.horizontalSpan = COLUMNS - 1;
        separator.setLayoutData(gridData);
        return separator;
    }

    protected void createLabel(Composite parent, int verticalIndent) {
        boolean wrapLabel = getLabel() != null && getLabel().trim().length() > 30;
        int style = wrapLabel ? SWT.WRAP : SWT.NONE;

        labelWidget = new Label(parent, style);
        int width = LABEL_WIDTH;
        if (getLabel() != null && getLabel().trim().length() > 0) {
            labelWidget.setText(getLabel() + ": ");
            if (!wrapLabel) {
                Point pt = labelWidget.computeSize(-1, -1, true);
                if (pt.x > width)
                    width = pt.x;
            }
        }
        GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
        gd.verticalIndent = verticalIndent;
        gd.widthHint = width;
        labelWidget.setLayoutData(gd);
    }

    private Text createTextInput(Composite parent) {
        if (span != COLUMNS)
            createLabel(parent);

        int style = SWT.BORDER | this.style;
        if (isReadOnly())
            style = style | SWT.READ_ONLY;
        if (isMultiLine())
            style = style | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL;
        else
            style = style | SWT.SINGLE;

        final Text textField = new Text(parent, style);
        if (font != null)
            textField.setFont(new Font(textField.getDisplay(), font));
        if (background != null)
            textField.setBackground(background);
        if (getTextLimit() != 0)
            textField.setTextLimit(getTextLimit());
        GridData gd = new GridData(SWT.LEFT);
        gd.widthHint = getWidth();
        gd.heightHint = getHeight();
        gd.horizontalSpan = span == 0 ? COLUMNS - 1 : span;
        gd.horizontalIndent = indent;
        if (getVerticalIndent() != 0)
            gd.verticalIndent = getVerticalIndent();

        textField.setLayoutData(gd);
        if (getValue() != null)
            textField.setText(getValue());
        textField.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                String oldValue = value;
                value = textField.getText().trim();
                boolean changed = oldValue == null ? value != null && value.length() > 0
                        : !oldValue.replace("\r", "").equals(value.replace("\r", ""));
                if (changed)
                    fireValueChanged(value);
            }
        });

        if (getComment() != null)
            createComment(parent);

        return textField;
    }

    private Combo createCombo(Composite parent) {
        if (valueOptions == null)
            throw new PropertyEditorException("Combo control requires value options.");

        createLabel(parent);

        int style = SWT.DROP_DOWN | SWT.BORDER | this.style;
        if (isReadOnly())
            style = style | SWT.READ_ONLY;
        final Combo comboBox = new Combo(parent, style);
        GridData gd = new GridData(SWT.LEFT);
        gd.horizontalSpan = span == 0 ? COLUMNS - 1 : span;
        gd.widthHint = getWidth();
        gd.horizontalIndent = indent;
        comboBox.setLayoutData(gd);
        comboBox.removeAll();
        for (String valueOption : getValueOptions())
            comboBox.add(valueOption);

        if (getValue() != null)
            comboBox.setText(getValue());

        comboBox.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                String oldValue = value;
                value = comboBox.getText().trim();
                boolean changed = oldValue == null ? value != null && value.length() > 0
                        : !oldValue.equals(value);
                if (changed)
                    fireValueChanged(value);
            }
        });
        return comboBox;
    }

    private ImageCombo createImageCombo(Composite parent) {
        if (valueOptions == null || imageOptions == null)
            throw new PropertyEditorException(
                    "Image Combo control requires value options and image options.");

        createLabel(parent);

        int style = SWT.BORDER | this.style;
        final ImageCombo imageCombo = new ImageCombo(parent, style);
        GridData gd = new GridData(SWT.LEFT);
        gd.horizontalSpan = COLUMNS - 1;
        gd.widthHint = getWidth();
        gd.horizontalIndent = indent;
        imageCombo.setLayoutData(gd);
        imageCombo.removeAll();
        for (int i = 0; i < valueOptions.size(); i++)
            imageCombo.add(valueOptions.get(i), imageOptions.get(i));

        if (getValue() != null)
            imageCombo.setText(getValue());

        imageCombo.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                String oldValue = value;
                value = imageCombo.getText().trim();
                boolean changed = oldValue == null ? value != null && value.length() > 0
                        : !oldValue.equals(value);
                if (changed)
                    fireValueChanged(value);
            }
        });
        return imageCombo;
    }

    private Link createLink(Composite parent) {
        labelWidget = new Label(parent, SWT.NONE);

        int style = SWT.SINGLE | this.style;
        Link link = new Link(parent, style);
        if (font != null)
            link.setFont(new Font(link.getDisplay(), font));
        if (background != null)
            link.setBackground(background);
        GridData gd = new GridData(SWT.CENTER | SWT.LEFT);
        gd.horizontalSpan = span == 0 ? COLUMNS - 1 : span;
        if (height == 0)
            gd.heightHint = 20;
        else
            gd.heightHint = height;
        gd.widthHint = getWidth();
        gd.horizontalIndent = indent;
        if (getVerticalIndent() != 0)
            gd.verticalIndent = getVerticalIndent();
        if (getLabel() != null && getLabel().indexOf("<A>") >= 0)
            link.setText(getLabel());
        else
            link.setText(" <A>" + getLabel() + "</A>\n");
        link.setLayoutData(gd);
        link.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (getValue() != null) {
                    if (getValue().startsWith("/MDWWeb/") || getValue().startsWith("/MDWHub/")) {
                        try {
                            String docPath = Compatibility
                                    .getDocumentationPath(getValue().substring(7));
                            String href = "/" + MdwPlugin.getPluginId() + "/help" + docPath;
                            PlatformUI.getWorkbench().getHelpSystem().displayHelpResource(href);
                        }
                        catch (Exception ex) {
                            PluginMessages.log(ex);
                        }
                    }
                    else {
                        String urlBase = getProject().getServerSettings().getUrlBase();
                        Program.launch(urlBase + getValue());
                    }
                }
                else {
                    fireValueChanged(null, false);
                }
            }
        });
        return link;
    }

    private Label createInfo(Composite parent) {
        labelWidget = new Label(parent, SWT.NONE);

        Label label = new Label(parent, SWT.WRAP | style);
        GridData gd = new GridData(SWT.LEFT);
        gd.widthHint = getWidth();
        gd.horizontalIndent = indent;
        gd.horizontalSpan = 3;
        label.setLayoutData(gd);
        if (getValue() != null)
            label.setText(getValue());
        return label;
    }

    private Group createRadio(Composite parent) {
        if (valueOptions == null || valueOptions.isEmpty())
            throw new PropertyEditorException("Radio control requires value options.");

        createLabel(parent, 6);

        Group radioGroup = new Group(parent, style);
        GridLayout gl = new GridLayout();
        gl.numColumns = valueOptions.size();
        radioGroup.setLayout(gl);
        GridData gd = new GridData(
                GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.horizontalSpan = COLUMNS - 1;
        gd.widthHint = getWidth();
        gd.horizontalIndent = indent;
        radioGroup.setLayoutData(gd);
        for (String option : valueOptions) {
            Button button = new Button(radioGroup, SWT.RADIO | SWT.LEFT);
            button.setText(option);
            if (option.equals(defaultValue))
                button.setSelection(true);
            button.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    Button button = (Button) e.widget;
                    if (button.getSelection()) {
                        value = button.getText();
                        fireValueChanged(value);
                    }
                }
            });
        }
        return radioGroup;
    }

    private Button createButton(Composite parent) {
        String label = comment == null ? "" : comment;
        labelWidget = new Label(parent, SWT.NONE);
        labelWidget.setText(label);
        Button button = new Button(parent, SWT.LEFT | SWT.PUSH);
        GridData gd = new GridData(GridData.CENTER);
        gd.horizontalSpan = COLUMNS - (comment == null ? 1 : 2);
        gd.horizontalIndent = indent;
        if (getVerticalIndent() != 0)
            gd.verticalIndent = getVerticalIndent();
        if (getWidth() != DEFAULT_VALUE_WIDTH)
            gd.widthHint = getWidth();
        button.setLayoutData(gd);
        button.setText(getLabel());
        button.setAlignment(SWT.CENTER);
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                fireValueChanged(value, false);
            }
        });

        return button;
    }

    private Button createCheckbox(Composite parent) {
        createLabel(parent);
        final Button checkbox = new Button(parent, SWT.CHECK);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = COLUMNS - 1;
        gd.horizontalIndent = indent;
        gd.verticalIndent = 2;
        checkbox.setLayoutData(gd);
        checkbox.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                boolean checked = checkbox.getSelection();
                value = Boolean.toString(checked);
                fireValueChanged(new Boolean(checked));
            }
        });

        if (readOnly)
            checkbox.setEnabled(false);

        return checkbox;
    }

    private SwitchButton createSwitch(Composite parent) {
        createLabel(parent);
        final SwitchButton switchBtn = new SwitchButton(parent, SWT.NONE);
        GridData gd = new GridData(GridData.BEGINNING);
        gd.horizontalSpan = comment == null ? COLUMNS - 1 : COLUMNS - 3;
        gd.horizontalIndent = indent;
        gd.verticalIndent = 2;

        switchBtn.setLayoutData(gd);

        switchBtn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                boolean checked = switchBtn.getSelection();
                value = Boolean.toString(checked);
                fireValueChanged(new Boolean(checked));
            }
        });

        if (getComment() != null)
            createComment(parent);

        if (readOnly)
            switchBtn.setEnabled(false);

        return switchBtn;
    }

    private Spinner createSpinner(Composite parent) {
        createLabel(parent);

        int style = SWT.BORDER | this.style;
        if (isReadOnly())
            style = style | SWT.READ_ONLY;
        final Spinner spinner = new Spinner(parent, style);
        GridData gd = new GridData(SWT.LEFT);
        gd.widthHint = getWidth();
        gd.heightHint = getHeight();
        gd.horizontalSpan = COLUMNS - 1;
        gd.horizontalIndent = indent;
        spinner.setLayoutData(gd);
        if (getValue() != null)
            spinner.setSelection(Integer.parseInt(getValue()));
        spinner.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                int oldValue = value == null || value.length() == 0 ? 0 : Integer.parseInt(value);
                int newValue = spinner.getSelection();
                value = String.valueOf(newValue);
                boolean changed = oldValue != newValue;
                if (changed)
                    fireValueChanged(value);
            }
        });

        return spinner;
    }

    private DatePicker createDatePicker(Composite parent) {
        createLabel(parent);

        int style = SWT.BORDER | this.style;
        if (isReadOnly())
            style = style | SWT.READ_ONLY;
        final DatePicker datePicker = new DatePicker(parent, style);
        datePicker.setEnabled(!isReadOnly());
        GridData gd = new GridData(SWT.LEFT);
        gd.widthHint = getWidth();
        gd.heightHint = getHeight();
        gd.horizontalSpan = COLUMNS - 1;
        gd.horizontalIndent = indent;
        datePicker.setLayoutData(gd);
        if (getValue() != null)
            updateWidget(getValue());
        datePicker.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
                String oldValue = value;
                String newValue = "";
                if (datePicker.getSelection() != null)
                    newValue = dateFormat.format(datePicker.getSelection().getTime());
                value = newValue;
                if (oldValue != null) {
                    boolean changed = !oldValue.equals(newValue);
                    if (changed)
                        fireValueChanged(newValue);
                }
            }
        });

        return datePicker;
    }

    private ListComposer createPickList(Composite parent) {
        if (valueOptions == null)
            throw new PropertyEditorException("PickList control requires value options.");

        String title = getLabel();
        int colonIdx = title.indexOf(':');
        if (colonIdx >= 0) {
            setLabel(title.substring(0, colonIdx));
            title = title.substring(colonIdx + 1);
        }

        createLabel(parent, 5);

        int style = SWT.DROP_DOWN | this.style;
        if (isReadOnly())
            style = style | SWT.READ_ONLY;
        final ListComposer pickList = new ListComposer(parent, style, title, valueOptions, 200,
                isReadOnly());
        GridData gd = new GridData(SWT.LEFT);
        gd.horizontalSpan = COLUMNS - 1;
        gd.widthHint = getWidth() + 20;
        gd.horizontalIndent = indent;
        pickList.setLayoutData(gd);
        if (!isReadOnly()) {
            pickList.setDestContentProvider(new PickListContentProvider(new ArrayList<String>()));
            pickList.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    fireValueChanged(value);
                }
            });
        }
        return pickList;
    }

    class PickListContentProvider implements IMutableContentProvider {
        private List<String> content;

        public PickListContentProvider(List<String> content) {
            this.content = content;
        }

        public Object[] getElements(Object inputElement) {
            String valueString = (String) inputElement;
            content = StringHelper.parseList(valueString);
            return content.toArray();
        }

        public void addToDest(Object o) {
            if (!content.contains(o)) {
                content.add((String) o);
                setValue(calcValue());
                fireValueChanged(value);
            }
        }

        public void remFromDest(Object o) {
            if (content.contains(o)) {
                content.remove(o);
                setValue(calcValue());
                fireValueChanged(value);
            }
        }

        private String calcValue() {
            return StringHelper.serialize(content, getProject().checkRequiredVersion(6, 0, 4));
        }

        public void clear() {
            content.clear();
        }

        public boolean contains(Object o) {
            return content.contains(o);
        }

        public void dispose() {
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }
    }

    private BamEventComposer createBamEventComposer(Composite parent) {
        createLabel(parent, 5);

        final BamEventComposer composer = new BamEventComposer(parent, style, width, isReadOnly());
        composer.setElement(getElement());

        GridData gd = new GridData(SWT.LEFT);
        gd.horizontalSpan = COLUMNS - 1;
        gd.horizontalIndent = indent;
        composer.setLayoutData(gd);

        composer.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                BamMessageDefinition bamMsgDef = composer.getBamMessage();
                value = valueConverter.toPropertyValue(bamMsgDef);
                fireValueChanged(bamMsgDef);
            }
        });

        return composer;
    }

    private ParameterizedCombo createParameterizedCombo(Composite parent) {
        createLabel(parent, 5);

        final ParameterizedCombo composer = new ParameterizedCombo(parent, getElement(),
                (ParameterizedValueConverter) valueConverter, style, width, isReadOnly());

        GridData gd = new GridData(SWT.LEFT);
        gd.horizontalSpan = COLUMNS - 1;
        gd.horizontalIndent = indent;
        composer.setLayoutData(gd);

        composer.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                List<String> values = composer.getValues();
                value = valueConverter.toPropertyValue(values);
                fireValueChanged(values);
            }
        });

        return composer;
    }

    private TimeInterval createTimer(Composite parent) {
        createLabel(parent, 5);

        int interval = value == null || value.length() == 0 ? 0 : Integer.parseInt(value);

        boolean allowExpressions = true;
        final TimeInterval timer = new TimeInterval(parent, style, interval, units, 75,
                allowExpressions, acceptedUnits);

        GridData gd = new GridData(SWT.LEFT);
        gd.widthHint = getWidth();
        gd.heightHint = getHeight();
        gd.horizontalSpan = COLUMNS - 1;
        gd.horizontalIndent = indent;
        timer.setLayoutData(gd);

        timer.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                String oldValue = value;
                Units oldUnits = units;
                TimeInterval.TimerValue newValue = timer.getValue();
                value = newValue.getInterval().equals("0") ? null : newValue.getInterval();
                units = newValue.getUnits();
                boolean valueChanged = oldValue == null || oldValue.equals("0")
                        ? value != null && value.length() > 0 && !value.equals("0")
                        : !oldValue.equals(value);
                boolean unitsChanged = oldUnits == null
                        ? units != null && units.toString().length() > 0 : !oldUnits.equals(units);
                if (valueChanged || unitsChanged)
                    fireValueChanged(newValue);
            }
        });

        return timer;
    }

    private Composite createBrowser(Composite parent) {
        Composite composite = new Composite(parent, SWT.BORDER | this.style);
        composite.setLayout(new FillLayout());
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalSpan = COLUMNS;
        gridData.verticalIndent = 5;
        composite.setLayoutData(gridData);
        new Browser(composite, SWT.NONE);
        return composite;
    }

    private ValueConverter valueConverter;

    public ValueConverter getValueConverter() {
        return valueConverter;
    }

    public void setValueConverter(ValueConverter vc) {
        this.valueConverter = vc;
    }

    private ListenerList valueChangeListeners = new ListenerList();

    public void addValueChangeListener(ValueChangeListener listener) {
        valueChangeListeners.add(listener);
    }

    public void removeValueChangeListener(ValueChangeListener listener) {
        valueChangeListeners.remove(listener);
    }

    public void fireValueChanged(Object newValue) {
        fireValueChanged(newValue, fireDirtyStateChange);
    }

    public void fireValueChanged(Object newValue, boolean fireDirtyStateChanged) {
        if (newValue != null && valueConverter != null) {
            if (newValue instanceof String)
                newValue = valueConverter.toModelValue((String) newValue);
            else if (newValue instanceof List)
                newValue = ((List<?>) newValue).get(0).toString();
        }

        for (int i = 0; i < valueChangeListeners.getListeners().length; ++i) {
            ValueChangeListener listener = (ValueChangeListener) valueChangeListeners
                    .getListeners()[i];
            listener.propertyValueChanged(newValue);
        }
        if (fireDirtyStateChanged && element != null)
            element.fireDirtyStateChanged(true);
    }

    protected WorkflowProcess getProcess() {
        WorkflowProcess process = null;
        if (element instanceof Activity)
            process = ((Activity) element).getProcess();
        else if (element instanceof WorkflowProcess)
            process = (WorkflowProcess) element;
        else if (element instanceof Transition)
            process = ((Transition) element).getProcess();

        return process;
    }
}
