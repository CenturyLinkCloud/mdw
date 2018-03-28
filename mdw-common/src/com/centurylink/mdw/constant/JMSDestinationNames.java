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
package com.centurylink.mdw.constant;

public class JMSDestinationNames {

    public static final String PROCESS_HANDLER_QUEUE = "com.centurylink.mdw.process.handler.queue";

    public static final String EXTERNAL_EVENT_HANDLER_QUEUE = "com.centurylink.mdw.external.event.queue";
    public static final String INTRA_MDW_EVENT_HANDLER_QUEUE = "com.centurylink.mdw.intra.event.queue";

    public static final String CONFIG_HANDLER_TOPIC = "com.centurylink.mdw.config.topic";
}
