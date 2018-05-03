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
package com.centurylink.mdw.model.system;

import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;

public class SystemMessage implements Jsonable {

    public static SystemMessage info(String message) {
        return new SystemMessage(Level.Info, message);
    }

    public static SystemMessage error(String message) {
        return new SystemMessage(Level.Error, message);
    }

    public enum Level {
        Info,
        Error
    }

    public SystemMessage(JSONObject json) {
        bind(json);
    }

    public SystemMessage(Level level, String message) {
        this.level = level;
        this.message = message;
    }

    private Level level;
    public Level getLevel() { return level; }
    public void setLevel(Level level) { this.level = level; }

    private String message;
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String toString() {
        return level + ": " + message;
    }
}
