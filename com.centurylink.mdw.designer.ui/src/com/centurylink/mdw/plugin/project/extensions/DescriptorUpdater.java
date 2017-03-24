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
package com.centurylink.mdw.plugin.project.extensions;

import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Updates text-based library descriptor files when an extension is being added
 * or removed.
 */
public interface DescriptorUpdater {
    public String getFilePath();

    public String processContents(String raw, IProgressMonitor monitor) throws IOException;
}
