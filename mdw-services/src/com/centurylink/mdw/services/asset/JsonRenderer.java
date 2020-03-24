package com.centurylink.mdw.services.asset;

import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.model.workflow.Process;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

@SuppressWarnings("unused")
public class JsonRenderer implements Renderer {

    private AssetInfo asset;

    public JsonRenderer(AssetInfo asset) {
        this.asset = asset;
    }

    @Override
    public byte[] render(Map<String,String> options) throws RenderingException {
        File assetFile = asset.getFile();
        if (assetFile.getName().endsWith(".proc")) {
            try {
                String procContent = new String(Files.readAllBytes(assetFile.toPath()));
                Process process = Process.fromString(procContent);
                JSONObject json =  process.getJson();
                json.put("name", asset.getRootName());
                return json.toString(2).getBytes();
            }
            catch (IOException ex) {
                throw new RenderingException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
            }
        }

        throw new RenderingException(ServiceException.NOT_IMPLEMENTED, "Cannot convert " + asset.getExtension() + " to JSON");
    }

    @Override
    public String getFileName() {
        return asset.getName();
    }
}
