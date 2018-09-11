/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.canvas;

import com.centurylink.mdw.draw.Impl;
import org.json.JSONObject;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;

public class Implementors extends LinkedHashMap<String, Impl> {

    public Implementors() {
        loadImplemetors(assetLoc);
    }

    private void loadImplemetors(File assetLoc) {
        for (File file : assetLoc.listFiles()) {
            if (file.isDirectory()) {
                loadImplemetors(file);
            }
            else if (file.exists()) {
                try {
                    if (file.getName().endsWith("impl")) {
                        add(new Impl(file.getPath(), new JSONObject(new String(Files.readAllBytes(file.toPath())))));
                    }
                    else if (file.getName().endsWith(".java")) {
                        // TODO annotation-based implementors

                    }
                    else if (file.getName().endsWith(".kt")) {
                        // TODO annotation-based implementors
                    }
                }
                catch (Exception e) {
                    System.out.println("Problem loading implementor " + file + ": " + e);
                }
            }
        }
    }

    public void add(Impl impl) {
        String iconAsset = impl.getIconName();
        if (iconAsset != null && !iconAsset.startsWith("shape:")) {
            String iconPkg = Impl.BASE_PKG;
            int slash = iconAsset.lastIndexOf('/');
            if (slash > 0) {
                iconPkg = iconAsset.substring(0, slash);
                iconAsset = iconAsset.substring(slash + 1);
            }
            else {
                String implClass = impl.getImplementorClassName();
                String pkg = implClass.substring(0, implClass.lastIndexOf('.'));
                if (new File(assetLoc + "/" + pkg.replace('.', '/') + "/" + iconAsset).isFile())
                    iconPkg = pkg;
            }
            impl.setIcon(getIcon(assetLoc + "/" + iconPkg.replace('.', '/') + "/" + iconAsset));
        }
        put(impl.getImplementorClassName(), impl);
    }

    public static File assetLoc;

    private ImageIcon getIcon(String filePath) {
        try {
            File imgFile = new File(filePath);
            return new ImageIcon(Files.readAllBytes(Paths.get(imgFile.getPath())));
        }
        catch (IOException e) {
            System.out.println("Unable to load icon " + filePath + ": " + e);
        }
        return null;
    }
}
