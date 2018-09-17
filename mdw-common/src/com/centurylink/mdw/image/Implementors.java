/*
 * Copyright (C) 2018 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.image;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;

import org.json.JSONObject;

import com.centurylink.mdw.draw.model.Data;
import com.centurylink.mdw.model.workflow.ActivityImplementor;

public class Implementors extends LinkedHashMap<String,ActivityImplementor> {

    private File assetLoc;

    public Implementors(File assetLoc) {
        this.assetLoc = assetLoc;
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
                        add(new ActivityImplementor(new JSONObject(new String(Files.readAllBytes(file.toPath())))));
                    }
                    else if (file.getName().endsWith(".java") || file.getName().endsWith(".kt")) {
                        ActivityImplementor impl = getImpl(file);
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
    private ActivityImplementor getImpl(File file) throws IOException {
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
            return new ActivityImplementor(implClass, category, label, icon, "{}");
        }
        return null;
    }

    public void add(ActivityImplementor impl) {
        String iconAsset = impl.getIcon();
        if (iconAsset != null && !iconAsset.startsWith("shape:")) {
            String iconPkg = Data.BASE_PKG;
            int slash = iconAsset.lastIndexOf('/');
            if (slash > 0) {
                iconPkg = iconAsset.substring(0, slash);
                iconAsset = iconAsset.substring(slash + 1);
            }
            else {
                String implClass = impl.getImplementorClass();
                String pkg = implClass.substring(0, implClass.lastIndexOf('.'));
                if (new File(assetLoc + "/" + pkg.replace('.', '/') + "/" + iconAsset).isFile())
                    iconPkg = pkg;
            }
            String iconPath = assetLoc + "/" + iconPkg.replace('.', '/') + "/" + iconAsset;
            try {
                impl.setImageIcon(getIcon(iconPath));
            }
            catch (IOException ex) {
                System.err.println("Cannot find icon " + iconPath + " for implementor " + impl.getImplementorClass());
            }
        }
        put(impl.getImplementorClass(), impl);
    }

    private ImageIcon getIcon(String iconPath) throws IOException {
        File imgFile = new File(iconPath);
        return new ImageIcon(Files.readAllBytes(Paths.get(imgFile.getPath())));
    }
}
