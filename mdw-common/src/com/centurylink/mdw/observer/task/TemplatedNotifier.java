package com.centurylink.mdw.observer.task;

import com.centurylink.mdw.model.asset.AssetVersionSpec;

/**
 * Base class for templated notifiers.
 */
public abstract class TemplatedNotifier implements TaskNotifier {

    private AssetVersionSpec templateAssetVerSpec;
    public AssetVersionSpec getTemplateSpec() { return templateAssetVerSpec; }
    public void setTemplateSpec(AssetVersionSpec templateAssetVerSpec) {
        this.templateAssetVerSpec = templateAssetVerSpec;
    }
}
