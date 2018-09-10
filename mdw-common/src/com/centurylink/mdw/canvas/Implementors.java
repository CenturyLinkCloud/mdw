/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.canvas;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;

import javax.swing.ImageIcon;

import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.draw.Impl;

public class Implementors extends LinkedHashMap<String, Impl> {
   public static final String BASE_PKG = "com.centurylink.mdw.base";
    public static final String START_IMPL = "com.centurylink.mdw.workflow.activity.process.ProcessStartActivity";
    public static final String STOP_IMPL = "com.centurylink.mdw.workflow.activity.process.ProcessFinishActivity";

    public Implementors() {
        loadImplemetors(assetLoc);
    }

    private void loadImplemetors(File assetLoc) {
        for (File file : assetLoc.listFiles()) {
            if (file.exists() && !file.isDirectory() && file.getPath().endsWith("impl")) {
                Impl impl;
                try {
                    impl = new Impl(file.getPath(), new JSONObject(
                            new String(Files.readAllBytes(Paths.get(file.getPath())))));

                    String iconAsset = impl.getIconName();
                    if (iconAsset != null && !iconAsset.startsWith("shape:")) {
                        String iconPkg = BASE_PKG;
                        int slash = iconAsset.lastIndexOf('/');
                        if (slash > 0) {
                            iconPkg = iconAsset.substring(0, slash).replace(".", "\\");
                            iconAsset = iconAsset.substring(slash + 1);
                        }
                        else
                            iconPkg = file.getPath().substring(0, file.getPath().lastIndexOf('\\'));
                        impl.setIcon(getIcon(iconPkg + "\\" + iconAsset));
                    }
                    put(impl.getImplementorClassName(), impl);
                }
                catch (JSONException e) {
                    System.out.println(" Unable to load implementors");
                }
                catch (IOException e) {
                   System.out.println(" Unable to load implementors");
                }
            }
            else if (file.isDirectory()){
                loadImplemetors(file);
            }
        }
    }

    public static File assetLoc;

    private ImageIcon getIcon(String filePath) {
        try {
            File imgFile = new File(filePath);
            return new ImageIcon(Files.readAllBytes(Paths.get(imgFile.getPath())));
        }
        catch (IOException e) {
            System.out.println("Unable to load icon --- " + filePath);
        }
        return null;
    }
}
