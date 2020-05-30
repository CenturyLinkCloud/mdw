package com.centurylink.mdw.dataaccess;

import com.centurylink.mdw.model.variable.VariableType;

import java.util.ArrayList;
import java.util.List;

/**
 * Built-in variable types.
 */
public class MdwVariableTypes {

    private List<VariableType> variableTypes;
    public List<VariableType> getVariableTypes() {
        if (variableTypes == null) {
            variableTypes = new ArrayList<>();
            variableTypes.add(new VariableType("java.lang.String", "com.centurylink.mdw.common.translator.impl.StringTranslator", 101));
            variableTypes.add(new VariableType("java.lang.Long", "com.centurylink.mdw.common.translator.impl.LongTranslator", 102));
            variableTypes.add(new VariableType("java.lang.Integer", "com.centurylink.mdw.common.translator.impl.IntegerTranslator", 103));
            variableTypes.add(new VariableType("java.lang.Boolean", "com.centurylink.mdw.common.translator.impl.BooleanTranslator", 104));
            variableTypes.add(new VariableType("java.util.Date", "com.centurylink.mdw.common.translator.impl.DateTranslator", 105));
            variableTypes.add(new VariableType("java.net.URI", "com.centurylink.mdw.common.translator.impl.URITranslator", 110));

            // collection types
            variableTypes.add(new VariableType("java.util.List<String>", "com.centurylink.mdw.common.translator.impl.StringListTranslator", 111));
            variableTypes.add(new VariableType("java.util.List<Integer>", "com.centurylink.mdw.common.translator.impl.IntegerListTranslator", 112));
            variableTypes.add(new VariableType("java.util.List<Long>", "com.centurylink.mdw.common.translator.impl.LongListTranslator", 113));
            variableTypes.add(new VariableType("java.util.Map<String,String>", "com.centurylink.mdw.common.translator.impl.StringStringMapTranslator", 114));

            // document variables
            variableTypes.add(new VariableType("org.w3c.dom.Document", "com.centurylink.mdw.common.translator.impl.DomDocumentTranslator", 201));
            variableTypes.add(new VariableType("org.apache.xmlbeans.XmlObject", "com.centurylink.mdw.common.translator.impl.XmlBeanTranslator", 202));
            variableTypes.add(new VariableType("java.lang.Object", "com.centurylink.mdw.common.translator.impl.JavaObjectTranslator", 203));
            variableTypes.add(new VariableType("org.json.JSONObject", "com.centurylink.mdw.common.translator.impl.JsonObjectTranslator", 204));
            variableTypes.add(new VariableType("groovy.util.Node", "com.centurylink.mdw.common.translator.impl.GroovyNodeTranslator", 205));
            variableTypes.add(new VariableType("com.centurylink.mdw.xml.XmlBeanWrapper", "com.centurylink.mdw.common.translator.impl.XmlBeanWrapperTranslator", 206));
            variableTypes.add(new VariableType("com.centurylink.mdw.model.StringDocument", "com.centurylink.mdw.common.translator.impl.StringDocumentTranslator", 207));
            variableTypes.add(new VariableType("com.centurylink.mdw.model.HTMLDocument", "com.centurylink.mdw.common.translator.impl.HtmlDocumentTranslator", 209));
            variableTypes.add(new VariableType("javax.xml.bind.JAXBElement", "com.centurylink.mdw.jaxb.JaxbElementTranslator", 210));
            variableTypes.add(new VariableType("com.centurylink.mdw.common.service.Jsonable", "com.centurylink.mdw.common.translator.impl.JsonableTranslator", 211));
            variableTypes.add(new VariableType("org.yaml.snakeyaml.Yaml", "com.centurylink.mdw.common.translator.impl.YamlTranslator", 212));
            variableTypes.add(new VariableType("java.lang.Exception", "com.centurylink.mdw.common.translator.impl.JsonableTranslator", 213));
            variableTypes.add(new VariableType("com.centurylink.mdw.model.Jsonable", "com.centurylink.mdw.common.translator.impl.JsonableTranslator", 214));

            // requires the mdw-camel bundle installed (TODO: annotation-driven)
            variableTypes.add(new VariableType("org.apache.camel.component.cxf.CxfPayload", "com.centurylink.mdw.camel.cxf.CxfPayloadTranslator", 310));
        }

        return variableTypes;
    }
}
