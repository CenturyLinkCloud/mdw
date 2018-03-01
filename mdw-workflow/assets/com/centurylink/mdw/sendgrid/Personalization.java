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
package com.centurylink.mdw.sendgrid;

import java.util.List;

import com.centurylink.mdw.model.Jsonable;

public class Personalization implements Jsonable {

    private String subject;
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    private List<Address> to;
    public List<Address> getTo() { return to; }
    public void setTo(List<Address> to) { this.to = to; }

    private List<Address> cc;
    public List<Address> getCc() { return cc; }
    public void setCc(List<Address> cc) { this.cc = cc; }

}
