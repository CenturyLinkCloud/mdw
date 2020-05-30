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
package com.centurylink.mdw.model.variable;

import com.centurylink.mdw.cache.asset.PackageCache;
import com.centurylink.mdw.model.workflow.Package;

import java.io.Serializable;
import java.util.Date;

public class Document implements Serializable {

    private String type;
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    @Deprecated
    public String getDocumentType() { return type; }

    private Long id;
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    @Deprecated
    public Long getDocumentId() { return id; }

    private Date createDate;
    public Date getCreateDate() {
        return createDate;
    }
    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    private Date modifyDate;
    public Date getModifyDate() {
        return modifyDate;
    }
    public void setModifyDate(Date modifyDate) {
        this.modifyDate = modifyDate;
    }

    private String ownerType;
    public String getOwnerType() {
        return ownerType;
    }
    public void setOwnerType(String ownerType) {
        this.ownerType = ownerType;
    }

    private Long ownerId;
    public Long getOwnerId() {
        return ownerId;
    }
    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    private Integer statusCode;
    public Integer getStatusCode() {
        return statusCode;
    }
    public void setStatusCode(Integer code) {
        this.statusCode = code;
    }

    private String statusMessage;
    public String getStatusMessage() {
        return statusMessage;
    }
    public void setStatusMessage(String message) { this.statusMessage = message; }

    private String path;
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    private String content; // string content
    public String getContent(Package pkg) {
        if (content == null && object != null) {
            if (pkg == null)
                pkg = PackageCache.getMdwBasePackage();
            content = pkg.getStringValue(type, object, true);
        }
        return content;
    }
    @Deprecated
    public String getContent() {
        return getContent(null);
    }
    public void setContent(String content) {
        this.content = content;
        object = null;
    }

    private Object object;  // object content
    public Object getObject(String type, Package pkg) {
        if (pkg == null)
            pkg = PackageCache.getMdwBasePackage();
        if (type == null || type.equals(this.type)) {
            if (object == null && content != null) {
                object = pkg.getObjectValue(this.type, content, true);
            }
        } else {
            if (content != null) {
                this.type = type;
                object = pkg.getObjectValue(this.type, content, true);
            } else if (object != null) {
                content = pkg.getStringValue(this.type, object, true);
                this.type = type;
                object = pkg.getStringValue(this.type, content, true);
            }
        }
        return object;
    }
    public void setObject(Object object, String type) {
        this.object = object;
        if (type != null)
            this.type = type;
        content = null;
    }
}
