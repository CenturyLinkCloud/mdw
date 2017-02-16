/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.properties.convert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.centurylink.mdw.plugin.designer.model.AttributeHolder;

public class ParameterizedValueConverter implements ValueConverter {
    private AttributeHolder activity;

    public AttributeHolder getActivity() {
        return activity;
    }

    public ParameterizedValueConverter(AttributeHolder activity) {
        this.activity = activity;
    }

    private Map<String, ComboParameter> optionMap = new HashMap<String, ComboParameter>();

    public void putOption(String option, ComboParameter param) {
        optionMap.put(option, param);
    }

    public ComboParameter getOptionParam(String option) {
        return optionMap.get(option);
    }

    public Set<String> getOptions() {
        return optionMap.keySet();
    }

    public Object toModelValue(String propertyValue) throws ConversionException {
        List<String> values = new ArrayList<String>();
        values.add(propertyValue);
        ComboParameter param = optionMap.get(propertyValue);
        if (param != null && param.getName() != null)
            values.add(activity.getAttribute(param.getName()));

        return values;
    }

    /**
     * Side-effect: sets the parameter attribute value on the activity
     */
    public String toPropertyValue(Object modelValue) throws ConversionException {
        String value = null;
        if (modelValue instanceof String) {
            value = (String) modelValue;
        }
        else if (modelValue instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> values = (List<String>) modelValue;
            value = values.get(0);
            ComboParameter param = optionMap.get(value);
            if (param != null && param.getName() != null && values.size() > 1)
                activity.setAttribute(param.getName(), values.get(1));
        }

        return value;
    }

    public class ComboParameter {
        public ComboParameter(String name, String types) {
            this.name = name;
            if (types != null)
                this.types = types.split(",");
        }

        private String name;

        public String getName() {
            return name;
        }

        private String[] types;

        public String[] getTypes() {
            return types;
        }
    }

}
