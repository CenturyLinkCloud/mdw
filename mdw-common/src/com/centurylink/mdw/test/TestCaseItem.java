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
package com.centurylink.mdw.test;

import java.util.Date;

import org.json.JSONObject;

import com.centurylink.mdw.model.Jsonable;
import com.centurylink.mdw.test.TestCase.Status;

import io.swagger.annotations.ApiModelProperty;

/**
 * Represents an item within a test case (eg: in a postman collection).
 */
public class TestCaseItem implements Jsonable {

    public TestCaseItem(JSONObject json) {
        bind(json);
    }

    public TestCaseItem(String name) {
        this.object = new JSONObject();
        this.object.put("name", name);
    }

    private Status status;
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    private Date start;
    public Date getStart() { return start; }
    public void setStart(Date start) { this.start = start; }

    private Date end;
    public Date getEnd() { return end; }
    public void setEnd(Date end) { this.end = end; }

    private String message;
    public String getMessage() { return message; }
    public void setMessage(String msg) { this.message = msg; }

    // package/asset path for expected results
    private String expected;
    public String getExpected() { return expected; }
    public void setExpected(String expected) { this.expected = expected; }

    // file path relative to expected results dir
    private String actual;
    public String getActual() { return actual; }
    public void setActual(String actual) { this.actual = actual; }

    // file path relative to expected results dir
    private String executeLog;
    public String getExecuteLog() { return executeLog; }
    public void setExecuteLog(String executeLog) { this.executeLog = executeLog; }

    /**
     * Must have a 'name' property.
     */
    private JSONObject object;
    public JSONObject getObject() { return object; }
    public void setObject(JSONObject object) { this.object = object; }

    @ApiModelProperty(hidden=true)
    public String getName() {
        return object == null ? null : object.optString("name");
    }
}
