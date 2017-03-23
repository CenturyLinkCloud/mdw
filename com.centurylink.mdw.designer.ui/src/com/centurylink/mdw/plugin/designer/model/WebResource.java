/*
 * Copyright (C) 2017 CenturyLink, Inc.
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
package com.centurylink.mdw.plugin.designer.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.centurylink.mdw.common.utilities.FileHelper;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;

public class WebResource extends WorkflowAsset {
    public enum ResourceType {
        Image, CSS, Script, HTML
    };

    public enum ImageFormat {
        JPEG, GIF, PNG
    };

    private ResourceType resourceType;

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType rt) {
        this.resourceType = rt;
    }

    private ImageFormat imageFormat;

    public ImageFormat getImageFormat() {
        return imageFormat;
    }

    public void setImageFormat(ImageFormat imageFormat) {
        this.imageFormat = imageFormat;
        switch (imageFormat) {
        case JPEG:
            setLanguage(RuleSetVO.IMAGE_JPEG);
            break;
        case GIF:
            setLanguage(RuleSetVO.IMAGE_GIF);
            break;
        case PNG:
            setLanguage(RuleSetVO.IMAGE_PNG);
            break;
        }
    }

    private static Map<String, ImageFormat> imageTypes = new HashMap<String, ImageFormat>();
    static {
        imageTypes.put(RuleSetVO.IMAGE_JPEG, ImageFormat.JPEG);
        imageTypes.put(RuleSetVO.IMAGE_GIF, ImageFormat.GIF);
        imageTypes.put(RuleSetVO.IMAGE_PNG, ImageFormat.PNG);
    }

    public static List<String> getTextBasedTypes() {
        List<String> types = new ArrayList<String>();
        types.add(ResourceType.CSS.toString());
        types.add(ResourceType.Script.toString());
        types.add(ResourceType.HTML.toString());
        return types;
    }

    public static List<String> getBinaryTypes() {
        List<String> types = new ArrayList<String>();
        for (ImageFormat format : imageTypes.values())
            types.add(format.toString());

        return types;
    }

    public WebResource() {
        super();
    }

    public WebResource(ResourceType resourceType) {
        super();
        this.resourceType = resourceType;
        switch (resourceType) {
        case Image:
            setLanguage(RuleSetVO.IMAGE_JPEG);
            imageFormat = ImageFormat.JPEG;
            break;
        case CSS:
            setLanguage(RuleSetVO.CSS);
            break;
        case Script:
            setLanguage(RuleSetVO.JAVASCRIPT);
            break;
        case HTML:
            setLanguage(RuleSetVO.HTML);
            break;
        }
    }

    public WebResource(RuleSetVO ruleSetVO, WorkflowPackage packageVersion) {
        super(ruleSetVO, packageVersion);
        initResourceType();
        initImageFormat();
    }

    public void initImageFormat() {
        if (RuleSetVO.IMAGE_JPEG.equals(getLanguage()))
            imageFormat = ImageFormat.JPEG;
        else if (RuleSetVO.IMAGE_GIF.equals(getLanguage()))
            imageFormat = ImageFormat.GIF;
        else if (RuleSetVO.IMAGE_PNG.equals(getLanguage()))
            imageFormat = ImageFormat.PNG;
    }

    public WebResource(WebResource cloneFrom) {
        super(cloneFrom);
        initResourceType();
    }

    public boolean isImage() {
        return getRuleSetVO().isImage();
    }

    private void initResourceType() {
        if (isImage())
            resourceType = ResourceType.Image;
        else if (getLanguage().equals(RuleSetVO.CSS))
            resourceType = ResourceType.CSS;
        else if (getLanguage().equals(RuleSetVO.WEBSCRIPT))
            resourceType = ResourceType.Script;
        else if (getLanguage().equals(RuleSetVO.JAVASCRIPT))
            resourceType = ResourceType.Script;
        else if (getLanguage().equals(RuleSetVO.HTML))
            resourceType = ResourceType.HTML;
        else
            throw new IllegalStateException("Unsupported resource type: " + getLanguage());
    }

    @Override
    public String getTitle() {
        if (resourceType == null)
            return "Web Resource";

        if (isImage())
            return imageTypes.get(getLanguage()).toString();
        else
            return resourceType.toString();
    }

    @Override
    public String getIcon() {
        switch (resourceType) {
        case Image:
            return "image.gif";
        case CSS:
            return "css.gif";
        case Script:
            return "javascript.gif";
        case HTML:
            return "page.gif";
        default:
            return null;
        }
    }

    public String getTempFileName() {
        String ext = null;
        if (resourceType.equals(ResourceType.Image)) {
            imageFormat = imageTypes.get(getLanguage());
            switch (imageFormat) {
            case JPEG:
                ext = ".jpg";
                break;
            case GIF:
                ext = ".gif";
                break;
            case PNG:
                ext = ".png";
                break;
            }
        }
        else {
            ext = WorkflowElement.getArtifactFileExtensions().get(getLanguage());
        }

        if (ext == null)
            throw new IllegalStateException("Cannot infer file extension");

        return FileHelper.stripDisallowedFilenameChars(getName()) + ext;
    }

    private static List<String> webResourceLanguages;

    @Override
    public List<String> getLanguages() {
        if (webResourceLanguages == null) {
            webResourceLanguages = new ArrayList<String>();
            webResourceLanguages.addAll(getTextBasedTypes());
            webResourceLanguages.addAll(getBinaryTypes());
        }
        return webResourceLanguages;
    }

}