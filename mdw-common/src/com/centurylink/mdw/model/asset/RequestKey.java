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
package com.centurylink.mdw.model.asset;

import com.centurylink.mdw.model.asset.AssetRequest.HttpMethod;

/**
 * Uniquely identifies a request by its method/path.
 */
public class RequestKey implements Comparable<RequestKey> {

    /**
     * This is the full service path (whereas AssetRequest.path is to asset package).
     */
    private String path;
    public String getPath() { return path; }

    private HttpMethod method;
    public HttpMethod getMethod() { return method; }

    public RequestKey(HttpMethod method, String path) {
        this.method = method;
        this.path = path;
    }

    public RequestKey(String method, String path) {
        this.method = HttpMethod.valueOf(method.toUpperCase());
        this.path = path;
    }

    public String toString() {
        return method + " " + path;
    }

    public boolean equals(Object other) {
        return other instanceof RequestKey && compareTo((RequestKey)other) == 0;
    }

    @Override
    public int compareTo(RequestKey other) {
        // handle dynamic path elements
        String[] segs1 = path.split("/");
        String[] segs2 = other.path.split("/");
        for (int i = 0; i < segs1.length; i++) {
            String seg = segs1[i];
            if (seg.startsWith("{") && seg.endsWith("}")) {
                segs1[i] = "{}";
                if (segs2.length > i)
                    segs2[i] = "{}";
            }
        }
        for (int i = 0; i < segs2.length; i++) {
            String seg = segs2[i];
            if (seg.startsWith("{") && seg.endsWith("}") && !seg.equals("{}")) {
                segs2[i] = "{}";
                if (segs1.length > i)
                    segs1[i] = "{}";
            }
        }
        int res = String.join("/", segs1).compareTo(String.join("/", segs2));
        if (res == 0)
            res = method.compareTo(other.method);
        return res;
    }
}
