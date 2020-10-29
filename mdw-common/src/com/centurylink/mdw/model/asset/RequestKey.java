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
        return path + " " + method;
    }

    public boolean match(RequestKey other) {
        if (!method.equals(other.method))
            return false;
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
        return String.join("/", segs1).equals(String.join("/", segs2));
    }

    /**
     * Not to be used for equality tests (see match).
     */
    @Override
    public int compareTo(RequestKey other) {
        return toString().compareTo(other.toString());
    }
}
