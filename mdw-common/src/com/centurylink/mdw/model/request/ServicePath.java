package com.centurylink.mdw.model.request;

import java.util.List;

public class ServicePath implements Comparable<ServicePath> {

    public static final String DELIMETER = "->";

    private String path;
    public String getPath() { return path; }

    private final String method;
    public String getMethod() { return method; }

    private final String[] segments;
    public String[] getSegments() { return segments; }

    public ServicePath(String path) {
        this(path, null);
    }

    public ServicePath(String path, String method) {
        this.path = path.startsWith("/") ? path.substring(1) : path;
        if (this.path.endsWith("/"))
            this.path = this.path.substring(0, this.path.length() - 1);
        this.segments = this.path.split("/");
        this.method = method;
    }

    public static ServicePath parse(String value) {
        int delim = value.indexOf(DELIMETER);
        if (delim == -1)
            return new ServicePath(value);
        else
            return new ServicePath(value.substring(delim + DELIMETER.length()), value.substring(0, delim));
    }

    /**
     * Sorting such that best match is found first.
     */
    @Override
    public int compareTo(ServicePath servicePath) {
        // longer paths come first
        if (segments.length != servicePath.segments.length) {
            return servicePath.segments.length - segments.length;
        }
        else {
            for (int i = 0; i < segments.length; i++) {
                boolean segmentIsParam = isParam(segments[i]);
                boolean serviceSegmentIsParam = isParam(servicePath.segments[i]);
                // non-params first
                if (segmentIsParam && !serviceSegmentIsParam)
                    return 1;
                else if (serviceSegmentIsParam && !segmentIsParam)
                    return -1;
            }
            return path.compareTo(servicePath.path);
        }
    }

    public boolean matchesPath(String runtimePath) {
        if (this.path.equals(runtimePath))
            return true;
        String[] runtimeSegments = new ServicePath(runtimePath).segments;
        if (runtimeSegments.length == segments.length) {
            for (int i = 0; i < runtimeSegments.length; i++) {
                String segment = segments[i];
                if (!runtimeSegments[i].equals(segment) && !isParam(segment)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public static boolean isParam(String segment) {
        return segment.charAt(0) == '{' && segment.charAt(segment.length() -1) == '}';
    }

    public ServicePath normalize(List<ServicePath> pathSpecs) {
        for (ServicePath pathSpec : pathSpecs) {
            if (pathSpec.matchesPath(path)) {
                return new ServicePath(pathSpec.path, method);
            }
        }
        return new ServicePath(path, method);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ServicePath))
            return false;
        ServicePath other = (ServicePath) o;
        if (!path.equals(other.path))
            return false;
        if (method == null)
            return other.method == null;
        else
            return method.equals(other.method);
    }

    @Override
    public String toString() {
        if (method == null)
            return path;
        else
            return method + DELIMETER + path;
    }
}
