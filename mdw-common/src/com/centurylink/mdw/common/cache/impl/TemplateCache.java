/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.cache.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Map;

import com.centurylink.mdw.common.cache.CacheEnabled;
import com.centurylink.mdw.common.email.Template;
import com.centurylink.mdw.common.email.Template.Format;
import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.provider.CacheService;
import com.centurylink.mdw.common.utilities.FileHelper;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.attribute.AssetVersionSpec;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;

public class TemplateCache implements CacheEnabled, CacheService {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static volatile Map<String,Template> templates = new Hashtable<String,Template>();

    public void refreshCache() throws CachingException {
        synchronized (templates) {
            clearCache();
        }
    }

    public void clearCache() {
        templates.clear();
    }

    public int getCacheSize() {
        return templates.size();
    }

    public static Template getTemplate(String templateName) throws CachingException {
        Template template = templates.get(templateName);
        if (template == null) {
            synchronized(templates) {
                template = templates.get(templateName);
                if (template == null)
                    template = loadTemplate(templateName);
                if (template != null)
                    templates.put(templateName, template);
            }
        }
        return template;
    }

    private static Template loadTemplate(String templateName) throws CachingException {
        // try to load from database
        RuleSetVO ruleSet = RuleSetCache.getRuleSet(templateName, RuleSetVO.HTML);
        if (ruleSet == null)
          ruleSet = RuleSetCache.getRuleSet(templateName, RuleSetVO.FACELET);  // try facelets
        if (ruleSet != null) {
            if (logger.isInfoEnabled())
                logger.info("Template: '" + templateName + "' loaded from database.");

            Format format = ruleSet.getLanguage().equals(RuleSetVO.FACELET) ? Format.Facelet : Format.HTML;
            return new Template(ruleSet.getId(), templateName, format, ruleSet.getRuleSet());
        }
        else {
            // try to load from file system
            URL url = Thread.currentThread().getContextClassLoader().getResource(templateName);
            if (url == null)
                throw new CachingException("Cannot find template: '" + templateName + "' in database or on classpath.");

            try {
                String path = getFilePath(url);
                String contents = FileHelper.getFileContents(path);
                if (logger.isInfoEnabled())
                    logger.info("Template: '" + templateName + "' loaded from " + path + ".");
                return new Template(0L, templateName, path.endsWith(".xhtml") ? Format.Facelet : Format.HTML, contents);
            }
            catch (IOException ex) {
                throw new CachingException(-1, "Unable to read template file: " + url, ex);
            }
        }
    }

    private static String getFilePath(URL url) throws FileNotFoundException {
        File file = null;
        try {
            file = new File(url.toURI());
        }
        catch(URISyntaxException e) {
            file = new File(url.getPath());
        }

        if (!file.exists())
            throw new FileNotFoundException(url.getPath());

        return file.getAbsolutePath();
    }

    /**
     * @param assetSpc
     * @return
     */
    public static Template getTemplate(AssetVersionSpec assetSpc) {
        Template template = templates.get(assetSpc.toString());
        if (template == null) {
            synchronized(templates) {
                template = templates.get(assetSpc.toString());
                if (template == null)
                    template = loadTemplate(assetSpc);
                if (template != null)
                    templates.put(assetSpc.toString(), template); // Cache templates based on assetspec
            }
        }
        return template;
    }

    /**
     * @param assetSpc
     * @return
     * TODO : Load from file system??
     */
    private static Template loadTemplate(AssetVersionSpec assetSpc) {
        RuleSetVO ruleSet = RuleSetCache.getRuleSet(assetSpc);
        if (ruleSet != null) {
            Format format = ruleSet.getLanguage().equals(RuleSetVO.FACELET) ? Format.Facelet : Format.HTML;
            return new Template(ruleSet.getId(), assetSpc, format, ruleSet.getRuleSet());
        }
        return null;
    }
}
