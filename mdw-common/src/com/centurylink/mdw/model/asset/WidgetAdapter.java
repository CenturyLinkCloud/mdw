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
package com.centurylink.mdw.model.asset;

import com.centurylink.mdw.model.asset.Pagelet.Widget;

public interface WidgetAdapter {

    /**
     * Widget value has been initialized.
     * Adapt widget.value to different type as needed.
     * Example: set widget.value to json array for picklist widget.
     */
    void didInit(Widget widget);

    /**
     * Widget value is about to be set on persistable model.
     * Restore widget.value to generic type before saving.
     * Example: reset widget.value to serialized json array for picklist widget.
     */
    void willUpdate(Widget widget);
}
