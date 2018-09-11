/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.image;

import com.centurylink.mdw.draw.Impl;
import org.json.JSONObject;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Implementors extends LinkedHashMap<String, Impl> {

    public static File assetLoc;

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
                    else if (file.getName().endsWith(".java") || file.getName().endsWith(".kt")) {
                        Impl impl = getImpl(file);
                        if (impl != null)
                            add(impl);
                    }
                }
                catch (Exception e) {
                    System.out.println("Problem loading implementor " + file + ": " + e);
                }
            }
        }
    }

    // ignore closing paren within strings (https://stackoverflow.com/a/6464500)
    private static final String ACTIVITY_REGEX = "@Activity\\s*\\((.*?\\)(?=([^\"\\\\]*(\\\\.|\"([^\"\\\\]*\\\\.)*[^\"\\\\]*\"))*[^\"]*$))";
    private static final Pattern ACTIVITY_ANNOTATION = Pattern.compile(ACTIVITY_REGEX, Pattern.DOTALL);
    private static final Pattern ICON_VALUE = Pattern.compile("icon\\s*=\\s*\"(.*?)\"");

    /**
     * For annotation-based implementors.  Custom impl classes cannot be compiled, so this crude
     * parsing mechanism is used to determine image icon.  Kotlin limitation: file name must be the same as impl class name.
     */
    private Impl getImpl(File file) throws IOException {
        String contents = new String(Files.readAllBytes(file.toPath()));

        Matcher matcher = ACTIVITY_ANNOTATION.matcher(contents);
        if (matcher.find()) {
            // only implClass and image are needed (other values are hardcoded)
            String asset = file.getAbsolutePath().substring(assetLoc.getAbsolutePath().length() + 1).replace('/', '.');
            String implClass = asset.substring(0, asset.lastIndexOf('.'));
            String label = implClass.substring(implClass.lastIndexOf('.') + 1);
            String category = "com.centurylink.mdw.activity.types.GeneralActivity";
            String icon = "shape:activity";
            // set icon if specified
            String params = matcher.group(1);
            Matcher iconMatcher = ICON_VALUE.matcher(params);
            if (iconMatcher.find()) {
                icon = iconMatcher.group(1);
            }
            return new Impl(category, label, icon, implClass, "{}");
        }
        return null;
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
            String iconPath = assetLoc + "/" + iconPkg.replace('.', '/') + "/" + iconAsset;
            try {
                impl.setIcon(getIcon(iconPath));
            }
            catch (IOException ex) {
                System.err.println("Cannot find icon " + iconPath + " for implementor " + impl.getImplementorClassName());
            }
        }
        put(impl.getImplementorClassName(), impl);
    }

    private ImageIcon getIcon(String iconPath) throws IOException {
        File imgFile = new File(iconPath);
        return new ImageIcon(Files.readAllBytes(Paths.get(imgFile.getPath())));
    }
}
