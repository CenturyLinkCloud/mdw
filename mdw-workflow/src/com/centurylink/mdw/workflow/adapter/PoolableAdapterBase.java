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
package com.centurylink.mdw.workflow.adapter;

import com.centurylink.mdw.util.log.StandardLogger.LogLevel;
import com.centurylink.mdw.util.timer.Tracked;

/**
 * @deprecated Extend {@link TextAdapterActivity} whose name better
 * describes its purpose.  Adapter poolability is no longer a concept
 * in MDW as we rely on underlying protocol pooling mechanisms.
 */
@Deprecated
@Tracked(LogLevel.TRACE)
public abstract class PoolableAdapterBase extends TextAdapterActivity {

}
