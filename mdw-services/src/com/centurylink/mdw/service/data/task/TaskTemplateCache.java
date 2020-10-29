package com.centurylink.mdw.service.data.task;

import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.cache.PreloadableCache;
import com.centurylink.mdw.cache.asset.AssetCache;
import com.centurylink.mdw.dataaccess.DataAccessException;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.task.TaskCategory;
import com.centurylink.mdw.model.task.TaskTemplate;
import com.centurylink.mdw.util.log.LoggerUtil;
import com.centurylink.mdw.util.log.StandardLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches task definitions.
 */
public class TaskTemplateCache implements PreloadableCache {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static Map<Long,TaskTemplate> templates = new ConcurrentHashMap<>();
    private static Map<String,TaskTemplate> latestTemplates = new ConcurrentHashMap<>();

    public void loadCache() throws CachingException {
        load();
    }

    public void clearCache() {
        synchronized(TaskTemplateCache.class) {
            templates.clear();
            latestTemplates.clear();
        }
    }

    public void refreshCache() throws CachingException {
        clearCache();
        loadCache();
    }

    private static synchronized void load() {
        for (Asset asset : AssetCache.getAssets("task", false)) {
            // latest templates are all loaded initially
            try {
                TaskTemplate template = loadTaskTemplate(asset);
                templates.put(template.getId(), template);
                latestTemplates.put(template.getPath(), template);
            } catch (IOException ex) {
                logger.error("Task template could not be loaded: " + asset.getLabel());
            }
        }
    }

    public static TaskTemplate getTaskTemplate(Long id) throws IOException {
        TaskTemplate template = templates.get(id);
        if (template == null) {
            // lazy loading for non-latest templates
            synchronized (TaskTemplateCache.class) {
                Asset asset = AssetCache.getAsset(id);
                if (asset != null) {
                    template = loadTaskTemplate(asset);
                    templates.put(template.getId(), template);
                }
            }
        }
        return template;
    }

    /**
     * Return the latest task id for the specified asset path.
     */
    public static TaskTemplate getTaskTemplate(String assetPath) {
        return latestTemplates.get(assetPath);
    }

    /**
     * Smart version retrieval
     */
    public static TaskTemplate getTaskTemplate(AssetVersionSpec spec) throws IOException {
        if (!spec.getPath().endsWith(".task"))
            spec = new AssetVersionSpec(spec.getPath() + ".task", spec.getVersion());
        Asset asset = AssetCache.getAsset(spec);
        return asset == null ? null : getTaskTemplate(asset.getId());
    }

    public static List<TaskTemplate> getTaskTemplatesForCategory(int categoryId) throws DataAccessException {

        TaskCategory taskCategory = null;
        for (TaskCategory category : TaskDataAccess.getTaskRefData().getCategories().values()) {
            if (category.getId().longValue() == categoryId) {
                taskCategory = category;
                break;
            }
        }
        if (taskCategory == null) {
            throw new DataAccessException(-1, "No category found for id " + categoryId);
        }
        List<TaskTemplate> templates = new ArrayList<>();
        for (TaskTemplate template : latestTemplates.values()) {
            if (taskCategory.getCode().equals(template.getTaskCategory()))
                templates.add(template);
        }
        return templates;
    }

    public static List<TaskTemplate> getTaskTemplates() {
        List<TaskTemplate> templates = new ArrayList<>(latestTemplates.values());
        Collections.sort(templates);
        return templates;
    }

    private static TaskTemplate loadTaskTemplate(Asset asset) throws IOException {
        Asset loadedAsset = AssetCache.getAsset(asset.getId());
        TaskTemplate taskTemplate = new TaskTemplate(new JsonObject(loadedAsset.getText()));
        taskTemplate.setId(loadedAsset.getId());
        taskTemplate.setName(loadedAsset.getName());
        taskTemplate.setVersion(loadedAsset.getVersion());
        taskTemplate.setPackageName(loadedAsset.getPackageName());
        taskTemplate.setFile(asset.getFile());
        taskTemplate.setArchived(asset.isArchived());
        return taskTemplate;
    }
}
