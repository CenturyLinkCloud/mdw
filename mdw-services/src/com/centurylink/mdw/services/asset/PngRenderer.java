package com.centurylink.mdw.services.asset;

import com.centurylink.mdw.app.ApplicationContext;
import com.centurylink.mdw.common.service.ServiceException;
import com.centurylink.mdw.model.asset.api.AssetInfo;
import com.centurylink.mdw.services.ServiceLocator;
import com.centurylink.mdw.services.SystemServices;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

@SuppressWarnings("unused")
public class PngRenderer implements Renderer {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private AssetInfo asset;

    public PngRenderer(AssetInfo asset) {
        this.asset = asset;
    }

    @Override
    public byte[] render(Map<String,String> options) throws RenderingException {
        if (!"proc".equals(asset.getExtension()))
            throw new RenderingException(RenderingException.BAD_REQUEST, "Unsupported asset: " + asset);
        String pkgPath = ApplicationContext.getAssetRoot().toPath().relativize(asset.getFile().getParentFile().toPath()).normalize().toString();
        String pkg = pkgPath.replace('/', '.');
        String assetPath = pkg + "/" + asset.getName();
        logger.debug("Exporting process: " + assetPath + " to PDF");
        File procFile = asset.getFile();
        try {
            SystemServices systemServices = ServiceLocator.getSystemServices();
            File exportDir = new File(ApplicationContext.getTempDirectory() + "/export/" + pkgPath);
            if (!exportDir.isDirectory() && !exportDir.mkdirs())
                throw new IOException("Unable to create export directory: " + exportDir);
            File exportFile = new File(exportDir + "/" + asset.getRootName() + ".png");
            String cliCommand = "export --process=" + assetPath + " --format=png --output=" + exportFile;
            logger.debug("CLI command: '" + cliCommand + "'");
            String output = systemServices.runCliCommand(cliCommand);
            if (!exportFile.exists())
                throw new FileNotFoundException(exportFile.toString() + " -- CLI output:\n" + output);
            return Files.readAllBytes(exportFile.toPath());
        }
        catch (Exception ex) {
            throw new RenderingException(ServiceException.INTERNAL_ERROR, ex.getMessage(), ex);
        }
    }

    @Override
    public String getFileName() {
        return asset.getRootName() + ".png";
    }
}
