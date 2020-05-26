package com.centurylink.mdw.tests.services;

import com.centurylink.mdw.annotations.Handler;
import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.request.RequestHandler;
import com.centurylink.mdw.request.RequestHandlerException;
import com.centurylink.mdw.services.request.ProcessNotifyHandler;
import org.apache.xmlbeans.SimpleValue;
import org.apache.xmlbeans.XmlObject;

import java.util.Map;

@Handler(match= RequestHandler.Routing.Content, path="MyKafkaHandler")
public class MyKafkaHandler extends ProcessNotifyHandler {

    @Override
    protected String getEventName(Request request, Object message, Map<String,String> headers)
            throws RequestHandlerException {
        return "KafkaMessage-" + ((SimpleValue)((XmlObject)message).selectChildren("", "MyKafkaHandler")[0]).getStringValue();
    }
}
