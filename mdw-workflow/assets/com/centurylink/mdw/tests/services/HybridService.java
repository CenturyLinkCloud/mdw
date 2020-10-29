package com.centurylink.mdw.tests.services;

import java.util.Map;

import javax.ws.rs.Path;

import org.apache.xmlbeans.XmlObject;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.services.rest.JsonXmlRestService;

/**
 * Payload should be either JSON or XML, conforming with model for Employee.java class from this pkg
 */
@Path("/HybridService")
public class HybridService extends JsonXmlRestService
{
    public JSONObject get(String path, Map<String,String> headers)
            throws ServiceException, JSONException {
        Employee emp = new Employee();
        emp.setSapId("jxb123456");
        emp.setWorkstationId("X700123456");
        emp.setFirstName("Jack");
        emp.setLastName("Brojde");

        return emp.getJson();
    }

    public JSONObject post(String path, JSONObject content, Map<String,String> headers)
            throws ServiceException, JSONException {
        return null;
    }

    /*
     * In the case of the GET HTTP method, the response is an Employee.java JSON string, which needs converting to XML.
     * Since response is not null, we override getXml() here in concrete service class to do that, as mentioned in JsonXmlRestService.java
     */
    @Override
    public String getXml(XmlObject xml, Map<String, String> metaInfo) throws ServiceException {
        String response = super.getXml(xml, metaInfo);
        if (response != null) {
            try {
                response = getJaxbTranslator(getPkg(metaInfo)).toString(new Employee(new JSONObject(response)), Employee.class.getName());
            }
            catch (Exception e) {
                throw new ServiceException(e.getMessage());
            }
        }
        return response;
    }
}
