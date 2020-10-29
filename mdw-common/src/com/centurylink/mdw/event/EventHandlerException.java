package com.centurylink.mdw.event;

import com.centurylink.mdw.request.RequestHandlerException;

/**
 * @deprecated
 * use {@link com.centurylink.mdw.request.RequestHandlerException}
 */
@Deprecated
public class EventHandlerException extends RequestHandlerException {
    @Deprecated
    public EventHandlerException(String message){
        super(message);
    }
    @Deprecated
    public EventHandlerException(String message, Throwable th){
        super(message, th);
    }
}
