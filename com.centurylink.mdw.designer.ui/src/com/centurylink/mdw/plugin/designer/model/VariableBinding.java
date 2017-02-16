/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer.model;

import com.centurylink.mdw.model.value.variable.VariableVO;

public class VariableBinding implements Comparable<VariableBinding> {
    private VariableVO variableVO;

    public VariableVO getVariableVO() {
        return variableVO;
    }

    public void setVariableVO(VariableVO variableVO) {
        this.variableVO = variableVO;
    }

    private String expression;

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public VariableBinding(VariableVO variableVO, String expression) {
        this.variableVO = variableVO;
        this.expression = expression;
    }

    public int compareTo(VariableBinding other) {
        return this.getVariableVO().getVariableName()
                .compareTo(other.getVariableVO().getVariableName());
    }
}