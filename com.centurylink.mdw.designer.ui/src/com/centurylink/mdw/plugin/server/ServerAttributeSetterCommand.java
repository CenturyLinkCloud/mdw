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
package com.centurylink.mdw.plugin.server;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IServerWorkingCopy;

public class ServerAttributeSetterCommand extends AbstractOperation {
    private IServerWorkingCopy server;
    private String attribute;
    private Object setting;
    private Object defaultSetting;
    private Object oldSetting;

    public ServerAttributeSetterCommand(IServerWorkingCopy server, String attribute, String setting,
            String defaultSetting) {
        super(attribute);
        this.server = server;
        this.attribute = attribute;
        this.setting = setting;
        this.defaultSetting = defaultSetting;
    }

    public ServerAttributeSetterCommand(IServerWorkingCopy server, String attribute, int setting,
            int defaultSetting) {
        super(attribute);
        this.server = server;
        this.attribute = attribute;
        this.setting = new Integer(setting);
        this.defaultSetting = new Integer(defaultSetting);
    }

    public ServerAttributeSetterCommand(IServerWorkingCopy server, String attribute,
            boolean setting, boolean defaultSetting) {
        super(attribute);
        this.server = server;
        this.attribute = attribute;
        this.setting = new Boolean(setting);
        this.defaultSetting = new Boolean(defaultSetting);
    }

    public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
        if (setting instanceof String) {
            oldSetting = server.getAttribute(attribute, (String) defaultSetting);
            server.setAttribute(attribute, (String) setting);
        }
        else if (setting instanceof Integer) {
            oldSetting = server.getAttribute(attribute, (Integer) defaultSetting);
            server.setAttribute(attribute, (Integer) setting);
        }
        else if (setting instanceof Boolean) {
            oldSetting = server.getAttribute(attribute, (Boolean) defaultSetting);
            server.setAttribute(attribute, (Boolean) setting);
        }

        return Status.OK_STATUS;
    }

    public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
        return execute(monitor, info);
    }

    public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
        if (setting instanceof String)
            server.setAttribute(attribute, (String) oldSetting);
        else if (setting instanceof Integer)
            server.setAttribute(attribute, (Integer) oldSetting);
        else if (setting instanceof Boolean)
            server.setAttribute(attribute, (Boolean) oldSetting);
        return Status.OK_STATUS;
    }
}