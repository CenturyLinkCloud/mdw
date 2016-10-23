/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.observer.task;

import com.centurylink.mdw.model.asset.AssetVersionSpec;


/**
 * Base class for templated notifiers.
 */
public abstract class TemplatedNotifier implements TaskNotifier {

    public static final String DEFAULT_NOTIFIER_IMPL = "com.centurylink.mdw.workflow.task.notifier.TaskEmailNotifier";

    private String template;
    /**
     * @deprecated use {@link #getTemplateSpec()}
     */
    @Deprecated
    public String getTemplate() { return template; }
    public void setTemplate(String template) { this.template = template; }

    private AssetVersionSpec templateAssetVerSpec;
    public AssetVersionSpec getTemplateSpec() { return templateAssetVerSpec; }
    public void setTemplateSpec(AssetVersionSpec templateAssetVerSpec) { this.templateAssetVerSpec = templateAssetVerSpec; }

    public TemplatedNotifier() {
    }
}
