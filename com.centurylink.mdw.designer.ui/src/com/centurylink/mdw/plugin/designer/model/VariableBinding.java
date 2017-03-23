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