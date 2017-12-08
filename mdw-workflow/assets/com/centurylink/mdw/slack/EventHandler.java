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
package com.centurylink.mdw.slack;

import org.json.JSONObject;

import com.centurylink.mdw.common.service.ServiceException;

/**
 * Interface for handling incoming slack events api messages:
 * https://api.slack.com/events-api
 */
@FunctionalInterface
public interface EventHandler {
    JSONObject handleEvent(SlackEvent event) 
            throws ServiceException;
}
