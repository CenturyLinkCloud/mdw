package com.centurylink.mdw.services.asset;

import com.centurylink.mdw.common.service.ServiceException;

public class RenderingException extends ServiceException {

    public RenderingException(int code, String message) {
        super(code, message);
    }

    public RenderingException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
