package com.centurylink.mdw.services.asset;

import java.util.Map;

public interface Renderer {
    byte[] render(Map<String,String> options) throws RenderingException;

    String getFileName();
}
