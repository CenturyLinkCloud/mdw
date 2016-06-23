/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf.renderers;

import java.io.IOException;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.myfaces.shared_tomahawk.renderkit.RendererUtils;
import org.apache.myfaces.shared_tomahawk.renderkit.html.HTML;
import org.apache.myfaces.shared_tomahawk.renderkit.html.HtmlRenderer;
import org.apache.myfaces.shared_tomahawk.renderkit.html.HtmlRendererUtils;
import org.apache.myfaces.shared_tomahawk.renderkit.html.util.FormInfo;

import com.centurylink.mdw.web.jsf.components.SubForm;

/**
 * TODO: This is copied from the Tomahawk Sandbox component library.  It is the only Sandbox dependency
 * still remaining.  Should be replaced with JSF2-style execute/render ajax attributes (maybe in conjunction
 * with the RichFaces region tag).
 *
 *         Date: 19.01.2006
 *         Time: 14:01:35
 */
public class SubFormRenderer extends HtmlRenderer
{
    private static final String SUBMIT_FUNCTION_SUFFIX = "_submit";
    private static final String HIDDEN_PARAM_NAME = "org.apache.myfaces.custom.subform.submittedId";

    public void encodeBegin(FacesContext context, UIComponent component) throws IOException
    {
        super.encodeBegin(context, component);

        ResponseWriter writer = context.getResponseWriter();

        HtmlRendererUtils.writePrettyLineSeparator(context);
        writer.startElement(HTML.SCRIPT_ELEM, null);
        writer.writeAttribute("type", "text/javascript", null);

        FormInfo parentFormInfo = RendererUtils.findNestingForm(component,context);
        if(parentFormInfo!=null)
        {
            writer.writeText(createPartialSubmitJS(component.getId(), parentFormInfo.getFormName()), null);
        }

        writer.endElement("script");
        HtmlRendererUtils.writePrettyLineSeparator(context);
    }

    @SuppressWarnings("rawtypes")
    public void decode(FacesContext context, UIComponent component)
    {
        super.decode(context, component);

        Map paramValuesMap = context.getExternalContext().getRequestParameterMap();
        String reqValue = (String) paramValuesMap.get(HIDDEN_PARAM_NAME);
        if (reqValue != null && component.getId().equals(reqValue))
        {
            ((SubForm) component).setSubmitted(true);
        }
    }

    /**
     *
     */
    protected String createPartialSubmitJS(String subFormId, String parentFormClientId)
    {
        StringBuffer script = new StringBuffer();
        script.append("function ");
        script.append(subFormId).append(SUBMIT_FUNCTION_SUFFIX + "()");
        script.append(" {\n");
        script.append("var form = document.forms['").append(parentFormClientId).append("'];\n");
        script.append("var el = document.createElement(\"input\");\n");
        script.append("el.type = \"hidden\";\n");
        script.append("el.name = \"" + HIDDEN_PARAM_NAME + "\";\n");
        script.append("el.value = \"").append(subFormId).append("\";\n");
        script.append("form.appendChild(el);\n");
        script.append("form.submit();\n");
        script.append("}\n");

        return script.toString();
    }

}
