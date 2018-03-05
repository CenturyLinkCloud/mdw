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

public class Email implements Jsonable {

    private List<Personalization> personalizations;
    public List<Personalization> getPersonalizations() { return personalizations; }
    public void setPersonalizations(List<Personalization> personalizations) {
        this.personalizations = personalizations;
    }

    private Address from;
    public Address getFrom() { return from; }
    public void setFrom(Address from) { this.from = from; }

    private List<Message> content;
    public List<Message> getContent() { return content; }
    public void setContent(List<Message> content) { this.content = content; }

    private List<Attachment> attachments;
    public List<Attachment> getAttachments() { return attachments; }
    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }
}
