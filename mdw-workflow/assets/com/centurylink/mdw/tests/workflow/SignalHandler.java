package com.centurylink.mdw.tests.workflow;

import com.centurylink.mdw.annotations.Handler;
import com.centurylink.mdw.model.request.Request;
import com.centurylink.mdw.request.RequestHandler;
import com.centurylink.mdw.services.request.ProcessNotifyHandler;
import org.apache.xmlbeans.SimpleValue;
import org.apache.xmlbeans.XmlObject;

import java.util.Map;

@Handler(match=RequestHandler.Routing.Content, path="Signal")
public class SignalHandler extends ProcessNotifyHandler {

    @Override
    protected String getEventName(Request request, Object message, Map<String,String> headers) {
        return "Signal-" + ((SimpleValue)((XmlObject)message).selectChildren("", "Signal")[0]).getStringValue();
    }
}
