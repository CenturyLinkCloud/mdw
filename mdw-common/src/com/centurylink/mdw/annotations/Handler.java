package com.centurylink.mdw.annotations;

import com.centurylink.mdw.request.RequestHandler;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Handler {

    RequestHandler.Routing match();

    /**
     * For content match, path is XPath or JSON Path.
     * For path match, path is service endpoint path expression.
     */
    String path();
}
