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

import java.util.ArrayList;
import java.util.List;

import com.centurylink.mdw.model.asset.Pagelet.Widget;

public class MonitoringWidget extends Widget {

    public MonitoringWidget(String name) {
        super(name, "table");
        setAttribute("section", "Monitoring");
        setAttribute("noButtons", "true");
        List<Widget> columns = new ArrayList<>();
        setWidgets(columns);
        Widget enabledColumn = new Widget("Enabled", "checkbox");
        enabledColumn.setAttribute("label", "Enabled");
        columns.add(enabledColumn);
        Widget nameColumn = new Widget("Name", "text");
        nameColumn.setAttribute("label", "Name");
        nameColumn.setAttribute("readonly", "true");
        columns.add(nameColumn);
        Widget implColumn = new Widget("Implementation", "asset");
        implColumn.setAttribute("label", "Implementation");
        implColumn.setAttribute("readonly", "true");
        columns.add(implColumn);
        Widget optionsColumn = new Widget("Options", "text");
        optionsColumn.setAttribute("label", "Options");
        columns.add(optionsColumn);
    }
}
