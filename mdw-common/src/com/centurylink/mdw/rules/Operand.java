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
package com.centurylink.mdw.rules;

import java.time.Instant;
import java.util.Map;

import com.centurylink.mdw.model.Status;
import com.centurylink.mdw.model.workflow.RuntimeContext;

public class Operand {

    public Operand() {
    }

    public Operand(Object input) {
        this.input = input;
    }

    private Object input;
    public Object getInput() { return input; }
    public void setInput(Object input) { this.input = input; }

    private Object result;
    public Object getResult() { return result; }
    public void setResult(Object result) { this.result = result; }

    public <T> T resultAs(Class<T> resultClass) throws ReflectiveOperationException {
        if (result == null)
            result = resultClass.newInstance();
        return resultClass.cast(result);
    }

    private Map<String,String> meta;
    public Map<String,String> getMeta() { return meta; }
    public void setMeta(Map<String,String> meta) { this.meta = meta; }

    private RuntimeContext context;
    public RuntimeContext getContext() { return context; }
    public void setContext(RuntimeContext context) { this.context = context; }

    private Map<String,String> params;
    public Map<String,String> getParams() { return params; }
    public void setParams(Map<String,String> params) { this.params = params; }

    private Status status;
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    private Instant start;
    public Instant getStart() { return start; }
    public void setStart(Instant start) { this.start = start; }

    private Instant end;
    public Instant getEnd() { return end; }
    public void setEnd(Instant end) { this.end = end; }
}
