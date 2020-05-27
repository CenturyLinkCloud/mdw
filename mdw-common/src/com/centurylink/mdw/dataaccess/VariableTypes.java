package com.centurylink.mdw.dataaccess;

import com.centurylink.mdw.model.variable.VariableType;

import java.util.ArrayList;
import java.util.List;

public class VariableTypes {

    private List<VariableType> variableTypes;
    public List<VariableType> getVariableTypes() {
        if (variableTypes == null) {
            variableTypes = new ArrayList<VariableType>(25);
            variableTypes.add(new VariableType(101L, "java.lang.String", "com.centurylink.mdw.common.translator.impl.StringTranslator"));
            variableTypes.add(new VariableType(102L, "java.lang.Long", "com.centurylink.mdw.common.translator.impl.LongTranslator"));
            variableTypes.add(new VariableType(103L, "java.lang.Integer", "com.centurylink.mdw.common.translator.impl.IntegerTranslator"));
            variableTypes.add(new VariableType(104L, "java.lang.Boolean", "com.centurylink.mdw.common.translator.impl.BooleanTranslator"));
            variableTypes.add(new VariableType(105L, "java.util.Date", "com.centurylink.mdw.common.translator.impl.DateTranslator"));
            variableTypes.add(new VariableType(110L, "java.net.URI", "com.centurylink.mdw.common.translator.impl.URITranslator"));

            // collections types
            variableTypes.add(new VariableType(111L, "java.util.List<String>", "com.centurylink.mdw.common.translator.impl.StringListTranslator"));
            variableTypes.add(new VariableType(112L, "java.util.List<Integer>", "com.centurylink.mdw.common.translator.impl.IntegerListTranslator"));
            variableTypes.add(new VariableType(113L, "java.util.List<Long>", "com.centurylink.mdw.common.translator.impl.LongListTranslator"));
            variableTypes.add(new VariableType(114L, "java.util.Map<String,String>", "com.centurylink.mdw.common.translator.impl.StringStringMapTranslator"));

            // document variables
            variableTypes.add(new VariableType(201L, "org.w3c.dom.Document", "com.centurylink.mdw.common.translator.impl.DomDocumentTranslator"));
            variableTypes.add(new VariableType(202L, "org.apache.xmlbeans.XmlObject", "com.centurylink.mdw.common.translator.impl.XmlBeanTranslator"));
            variableTypes.add(new VariableType(203L, "java.lang.Object", "com.centurylink.mdw.common.translator.impl.JavaObjectTranslator"));
            variableTypes.add(new VariableType(204L, "org.json.JSONObject", "com.centurylink.mdw.common.translator.impl.JsonObjectTranslator"));
            variableTypes.add(new VariableType(205L, "groovy.util.Node", "com.centurylink.mdw.common.translator.impl.GroovyNodeTranslator"));
            variableTypes.add(new VariableType(206L, "com.centurylink.mdw.xml.XmlBeanWrapper", "com.centurylink.mdw.common.translator.impl.XmlBeanWrapperTranslator"));
            variableTypes.add(new VariableType(207L, "com.centurylink.mdw.model.StringDocument", "com.centurylink.mdw.common.translator.impl.StringDocumentTranslator"));
            variableTypes.add(new VariableType(209L, "com.centurylink.mdw.model.HTMLDocument", "com.centurylink.mdw.common.translator.impl.HtmlDocumentTranslator"));
            variableTypes.add(new VariableType(210L, "javax.xml.bind.JAXBElement", "com.centurylink.mdw.jaxb.JaxbElementTranslator"));
            variableTypes.add(new VariableType(211L, "com.centurylink.mdw.common.service.Jsonable", "com.centurylink.mdw.common.translator.impl.JsonableTranslator"));
            variableTypes.add(new VariableType(212L, "org.yaml.snakeyaml.Yaml", "com.centurylink.mdw.common.translator.impl.YamlTranslator"));
            variableTypes.add(new VariableType(213L, "java.lang.Exception", "com.centurylink.mdw.common.translator.impl.JsonableTranslator"));
            variableTypes.add(new VariableType(214L, "com.centurylink.mdw.model.Jsonable", "com.centurylink.mdw.common.translator.impl.JsonableTranslator"));
            // requires the mdw-camel bundle installed
            variableTypes.add(new VariableType(310L, "org.apache.camel.component.cxf.CxfPayload", "com.centurylink.mdw.camel.cxf.CxfPayloadTranslator"));
        }

        return variableTypes;
    }

    public String getVariableType(Object value) {
        for (VariableType varType : getVariableTypes()) {
            try {
                if (!varType.isJavaObjectType() && (Class.forName(varType.getVariableType()).isInstance(value)))
                    return varType.getVariableType();
            }
            catch (Exception ex) {
                return Object.class.getName();
            }
        }
        return null;
    }
}
