/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.dataaccess.file;

import java.util.List;

import com.centurylink.mdw.model.value.variable.VariableTypeVO;

/**
 * Can be extended by workflow apps to provide compability for 5.2 variable types.
 */
public class CompatibilityBaselineData extends MdwBaselineData {

    private List<VariableTypeVO> variableTypes;

    @Override
    public List<VariableTypeVO> getVariableTypes() {
        if (variableTypes == null) {
            variableTypes = super.getVariableTypes();
            variableTypes.add(new VariableTypeVO(1L, "java.lang.String","com.centurylink.mdw.common.translator.impl.StringTranslator"));
            variableTypes.add(new VariableTypeVO(2L, "java.lang.Long","com.centurylink.mdw.common.translator.impl.LongTranslator"));
            variableTypes.add(new VariableTypeVO(3L, "java.lang.Integer","com.centurylink.mdw.common.translator.impl.IntegerTranslator"));
            variableTypes.add(new VariableTypeVO(4L, "java.lang.Boolean","com.centurylink.mdw.common.translator.impl.BooleanTranslator"));
            variableTypes.add(new VariableTypeVO(5L, "java.util.Date","com.centurylink.mdw.common.translator.impl.DateTranslator"));
            variableTypes.add(new VariableTypeVO(6L, "java.lang.String[]","com.centurylink.mdw.common.translator.impl.StringArrayTranslator"));
            variableTypes.add(new VariableTypeVO(7L, "java.lang.Integer[]","com.centurylink.mdw.common.translator.impl.IntegerArrayTranslator"));
            variableTypes.add(new VariableTypeVO(8L, "java.lang.Long[]","com.centurylink.mdw.common.translator.impl.LongArrayTranslator"));
            variableTypes.add(new VariableTypeVO(9L, "java.util.Map","com.centurylink.mdw.common.translator.impl.StringMapTranslator"));

            variableTypes.add(new VariableTypeVO(11L, "org.w3c.dom.Document", "com.centurylink.mdw.common.translator.impl.DomDocumentTranslator"));
            variableTypes.add(new VariableTypeVO(12L, "org.apache.xmlbeans.XmlObject", "com.centurylink.mdw.common.translator.impl.XmlBeanTranslator"));
            variableTypes.add(new VariableTypeVO(15L, "java.lang.Object", "com.centurylink.mdw.common.translator.impl.JavaObjectTranslator"));
            variableTypes.add(new VariableTypeVO(16L, "org.json.JSONObject", "com.centurylink.mdw.common.translator.impl.JsonObjectTranslator"));
            variableTypes.add(new VariableTypeVO(19L, "com.centurylink.mdw.xml.XmlBeanWrapper", "com.centurylink.mdw.common.translator.impl.XmlBeanWrapperTranslator"));
            variableTypes.add(new VariableTypeVO(20L, "com.centurylink.mdw.model.StringDocument", "com.centurylink.mdw.common.translator.impl.StringDocumentTranslator"));

            variableTypes.add(new VariableTypeVO(10L, "com.qwest.mbeng.MbengTableArray","com.qwest.mdw.common.translator.base.TableDocumentTranslator"));
            variableTypes.add(new VariableTypeVO(11L, "org.w3c.dom.Document","com.qwest.mdw.common.translator.base.DomDocumentTranslator"));
            variableTypes.add(new VariableTypeVO(12L, "org.apache.xmlbeans.XmlObject","com.qwest.mdw.common.translator.base.XmlBeanTranslator"));
            variableTypes.add(new VariableTypeVO(13L, "com.qwest.mdw.model.FormDataDocument","com.qwest.mdw.common.translator.base.FormDataDocumentTranslator"));
            variableTypes.add(new VariableTypeVO(14L, "com.qwest.mbeng.MbengDocument","com.qwest.mdw.common.translator.base.ServiceOrderTranslator"));
            variableTypes.add(new VariableTypeVO(15L, "java.lang.Object", "com.qwest.mdw.common.translator.base.JavaObjectTranslator"));
            variableTypes.add(new VariableTypeVO(16L, "org.json.JSONObject", "com.qwest.mdw.common.translator.base.JsonObjectTranslator"));
            variableTypes.add(new VariableTypeVO(17L, "java.net.URI", "com.qwest.mdw.common.translator.base.URITranslator"));
            variableTypes.add(new VariableTypeVO(18L, "groovy.util.Node", "com.qwest.mdw.common.translator.base.GroovyNodeTranslator"));
            variableTypes.add(new VariableTypeVO(19L, "com.qwest.mdw.xml.XmlBeanWrapper","com.qwest.mdw.common.translator.base.XmlBeanWrapperTranslator"));
            variableTypes.add(new VariableTypeVO(20L, "com.qwest.mdw.model.StringDocument","com.qwest.mdw.common.translator.base.StringDocumentTranslator"));
        }
        return variableTypes;
    }
}
