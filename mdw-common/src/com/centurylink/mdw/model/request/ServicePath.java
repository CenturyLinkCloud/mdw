package com.centurylink.mdw.model.request;

import java.util.List;

public class ServicePath implements Comparable<ServicePath> {

    private String path;
    public String getPath() { return path; }

    private String[] segments;
    public String[] getSegments() { return segments; }

    public ServicePath(String path) {
        this.path = path.startsWith("/") ? path.substring(1) : path;
        if (this.path.endsWith("/"))
            this.path = this.path.substring(0, this.path.length() - 1);
        this.segments = this.path.split("/");
    }

    @Override
    public int compareTo(ServicePath servicePath) {
        // longer paths come first
        if (path.startsWith(servicePath.path))
            return servicePath.path.length() - path.length();
        else if (servicePath.path.startsWith(path))
            return path.length() - servicePath.path.length();
        else
            return path.compareTo(servicePath.path);
    }

    public boolean matches(ServicePath runtimePath) {
        if (path.equals(runtimePath))
            return true;
        String[] runtimeSegments = runtimePath.segments;
        if (runtimeSegments.length == segments.length) {
            for (int i = 0; i < runtimeSegments.length; i++) {
                String segment = segments[i];
                if (!runtimeSegments[i].equals(segment) &&
                        !(segment.charAt(0) == '{' && segment.charAt(segment.length() -1) == '}')) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public String normalize(List<ServicePath> pathSpecs) {
        for (ServicePath pathSpec : pathSpecs) {
            if (pathSpec.matches(this))
                return pathSpec.getPath();
        }
        return path;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ServicePath && path.equals(((ServicePath)o).path);
    }

    @Override
    public String toString() {
        return path;
    }
}
