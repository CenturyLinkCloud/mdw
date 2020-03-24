package com.centurylink.mdw.services.asset;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.html.HtmlExportHelper;
import com.centurylink.mdw.image.PngProcessExporter;
import com.centurylink.mdw.model.asset.AssetInfo;
import com.centurylink.mdw.model.project.Data;
import com.centurylink.mdw.model.project.Project;
import com.centurylink.mdw.model.system.MdwVersion;
import com.centurylink.mdw.model.workflow.Process;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

@SuppressWarnings("unused")
public class PngRenderer implements Renderer {

    private AssetInfo asset;

    public PngRenderer(AssetInfo asset) {
        this.asset = asset;
    }

    @Override
    public byte[] render(Map<String,String> options) throws RenderingException {
        File assetFile = asset.getFile();
        if (assetFile.getName().endsWith(".proc")) {
            try {
                String procContent = new String(Files.readAllBytes(assetFile.toPath()));
                Process process = Process.fromString(procContent);
                process.setName(asset.getRootName());
                PngProcessExporter exporter =  new PngProcessExporter(new Project() {
                    public File getAssetRoot() throws IOException {
                        return ApplicationContext.getAssetRoot();
                    }
                    public String getHubRootUrl() throws IOException {
                        return ApplicationContext.getMdwHubUrl();
                    }
                    public MdwVersion getMdwVersion() throws IOException {
                        return new MdwVersion(ApplicationContext.getMdwVersion());
                    }
                    private Data data;
                    public Data getData() {
                        if (data == null)
                            data = new Data(this);
                        return data;
                    }

                });
                return exporter.export(process);
            }
            catch (IOException ex) {
                throw new RenderingException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
            }
        }
        throw new RenderingException(ServiceException.NOT_IMPLEMENTED, "Cannot convert " + asset.getExtension() + " to Png Image");
    }

    @Override
    public String getFileName() {
        return asset.getRootName() + ".png";
    }
}
