/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.hub.report.birt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;

import org.eclipse.birt.report.engine.api.IParameterDefn;
import org.eclipse.birt.report.engine.api.IScalarParameterDefn;

import com.centurylink.mdw.web.ui.UIException;
import com.centurylink.mdw.web.ui.filter.Filter;
import com.centurylink.mdw.web.ui.input.DateInput;
import com.centurylink.mdw.web.ui.input.DigitInput;
import com.centurylink.mdw.web.ui.input.Input;
import com.centurylink.mdw.web.ui.input.SelectInput;
import com.centurylink.mdw.web.ui.input.TextInput;

public class BirtFilter extends Filter
{
    private String id;
    public String getId() { return id; }

    private List<IParameterDefn> parameterDefs;
    public List<IParameterDefn> getParameterDefs() { return parameterDefs; }

    private Map<String,List<SelectItem>> selectItemsMap;
    public Map<String,List<SelectItem>> getSelectItemsMap() { return selectItemsMap; }
    public List<SelectItem> getSelectItems(String paramName) {
      return selectItemsMap.get(paramName);
    }

    private Map<String,String> requestParams;
    public Map<String,String> getRequestParams() { return requestParams; }

    public BirtFilter(String id, List<IParameterDefn> parameterDefs, Map<String,List<SelectItem>> selectItemsMap, Map<String,String> requestParams) throws UIException {
      this.id = id;
      this.parameterDefs = parameterDefs;
      this.selectItemsMap = selectItemsMap;
      this.requestParams = requestParams;
      populateInputs();
    }

    private String name;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    private int width = 150;
    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    private DataModel<Input> criteria;
    public DataModel<Input> getCriteria() { return criteria; }

    @SuppressWarnings("unchecked")
    public List<Input> getCriteriaList() {
      if (criteria == null)
        return null;
      return (List<Input>) criteria.getWrappedData();
    }

    public void populateInputs() throws UIException {
        try {
            List<Input> list = new ArrayList<Input>();

            for (int i = 0; i < parameterDefs.size(); i++) {
                IParameterDefn parameterDef = parameterDefs.get(i);
                Input input = null;
                String attr = parameterDef.getName();
                String label = parameterDef.getPromptText();

                IScalarParameterDefn scalarParamDef = (IScalarParameterDefn) parameterDef;
                int dataType = scalarParamDef.getDataType();
                int inputType = scalarParamDef.getControlType();

                if (dataType == IScalarParameterDefn.TYPE_STRING) {
                    if (inputType == IScalarParameterDefn.TEXT_BOX) {
                        input = new TextInput(attr, label);
                    }
                    else if (inputType == IScalarParameterDefn.SELECTION_LIST_STATIC
                            || inputType == IScalarParameterDefn.SELECTION_LIST_DYNAMIC) {
                        input = new SelectInput(attr, label, getSelectItems(attr));
                    }
                    if (input != null)
                        input.setModelType("java.lang.String");
                }
                else if (dataType == IScalarParameterDefn.TYPE_DECIMAL
                        || dataType == IScalarParameterDefn.TYPE_INTEGER) {
                    input = new DigitInput(attr, label);
                    input.setModelType("java.lang.Integer");
                }
                else if (dataType == IScalarParameterDefn.TYPE_DATE) {
                    input = new DateInput(attr, label);
                    input.setModelType("java.util.Date");
                }

                if (input == null)
                    throw new UIException("Unsupported report parameter dataType: " + dataType);

                input.setSequenceId(i);
                input.setExpandable(true);
                input.setHidden(parameterDef.isHidden());

                String defaultString = null;
                // highest precedence: request params
                if (requestParams != null) {
                    defaultString = requestParams.get(attr);
                }
                // next highest precedence: user prefs
                if (defaultString == null) {
                    defaultString = getDefaultValue(attr);
                }
                // lowest precedence: base default
                if (defaultString == null) {
                    defaultString = scalarParamDef.getDefaultValue();
                }

                Object defaultValue = input.isInputTypeDate() ? getDateValue(defaultString) : defaultString;

                if (defaultValue != null)
                    input.setValue(defaultValue);

                list.add(input);
            }

            criteria = new ListDataModel<Input>(list);
        }
        catch (Exception ex) {
            throw new UIException(ex.getMessage(), ex);
        }
    }

    @Override
    public String getActionListener() {
      return BirtFilterListener.class.getName();
    }
}
