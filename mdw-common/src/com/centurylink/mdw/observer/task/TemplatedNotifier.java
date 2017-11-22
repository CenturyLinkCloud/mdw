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
package com.centurylink.mdw.observer.task;

import com.centurylink.mdw.model.asset.AssetVersionSpec;


/**
 * Base class for templated notifiers.
 */
public abstract class TemplatedNotifier implements TaskNotifier {

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
