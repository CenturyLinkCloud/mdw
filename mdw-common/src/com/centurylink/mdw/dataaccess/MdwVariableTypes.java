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
            variableTypes.add(new VariableType("java.lang.String", "com.centurylink.mdw.common.translator.impl.StringTranslator", false, 101));
            variableTypes.add(new VariableType("java.lang.Boolean", "com.centurylink.mdw.common.translator.impl.BooleanTranslator", false, 104));
            variableTypes.add(new VariableType("java.lang.Exception", "com.centurylink.mdw.common.translator.impl.JsonableTranslator", true,  213));
            variableTypes.add(new VariableType("java.lang.Integer", "com.centurylink.mdw.common.translator.impl.IntegerTranslator", false, 103));
            variableTypes.add(new VariableType("java.lang.Long", "com.centurylink.mdw.common.translator.impl.LongTranslator", false, 102));
            variableTypes.add(new VariableType("java.lang.Object", "com.centurylink.mdw.common.translator.impl.JavaObjectTranslator", true, 203));
            variableTypes.add(new VariableType("java.net.URI", "com.centurylink.mdw.common.translator.impl.URITranslator", false, 110));
            variableTypes.add(new VariableType("java.util.Date", "com.centurylink.mdw.common.translator.impl.DateTranslator", false, 105));
            variableTypes.add(new VariableType("java.util.List<String>", "com.centurylink.mdw.common.translator.impl.StringListTranslator", true, 111));
            variableTypes.add(new VariableType("java.util.List<Integer>", "com.centurylink.mdw.common.translator.impl.IntegerListTranslator", true, 112));
            variableTypes.add(new VariableType("java.util.List<Long>", "com.centurylink.mdw.common.translator.impl.LongListTranslator", true, 113));
            variableTypes.add(new VariableType("java.util.Map<String,String>", "com.centurylink.mdw.common.translator.impl.StringStringMapTranslator", true, 114));
            variableTypes.add(new VariableType("org.json.JSONObject", "com.centurylink.mdw.common.translator.impl.JsonObjectTranslator", true, 204));
            variableTypes.add(new VariableType("com.centurylink.mdw.model.Jsonable", "com.centurylink.mdw.common.translator.impl.JsonableTranslator", true, 214));
            variableTypes.add(new VariableType("com.centurylink.mdw.model.StringDocument", "com.centurylink.mdw.common.translator.impl.StringDocumentTranslator", true, 207));
            variableTypes.add(new VariableType("org.w3c.dom.Document", "com.centurylink.mdw.common.translator.impl.DomDocumentTranslator", true, 201));
            variableTypes.add(new VariableType("javax.xml.bind.JAXBElement", "com.centurylink.mdw.jaxb.JaxbElementTranslator", true, 210));
            variableTypes.add(new VariableType("org.apache.xmlbeans.XmlObject", "com.centurylink.mdw.common.translator.impl.XmlBeanTranslator", true, 202));
            variableTypes.add(new VariableType("org.yaml.snakeyaml.Yaml", "com.centurylink.mdw.common.translator.impl.YamlTranslator", true, 212));

            // requires the mdw-camel bundle installed (TODO: annotation-driven)
            variableTypes.add(new VariableType("org.apache.camel.component.cxf.CxfPayload", "com.centurylink.mdw.camel.cxf.CxfPayloadTranslator", true, 310));
        }

        return variableTypes;
    }

    /**
     * These are only loaded for inflight purposes (not in design-time variable type dropdown).
     */
    private List<VariableType> deprecatedTypes;
    @Deprecated
    public List<VariableType> getDeprecatedTypes() {
        if (deprecatedTypes == null) {
            deprecatedTypes = new ArrayList<>();
            deprecatedTypes.add(new VariableType("groovy.util.Node", "com.centurylink.mdw.common.translator.impl.GroovyNodeTranslator", true, 205));
            deprecatedTypes.add(new VariableType("com.centurylink.mdw.xml.XmlBeanWrapper", "com.centurylink.mdw.common.translator.impl.XmlBeanWrapperTranslator", true, 206));
            deprecatedTypes.add(new VariableType("com.centurylink.mdw.model.HTMLDocument", "com.centurylink.mdw.common.translator.impl.HtmlDocumentTranslator", true, 209));
            deprecatedTypes.add(new VariableType("com.centurylink.mdw.common.service.Jsonable", "com.centurylink.mdw.common.translator.impl.JsonableTranslator", true, 211));
        }
        return deprecatedTypes;
    }
}
