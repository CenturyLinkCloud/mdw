/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.maven;

import java.io.File;
import java.util.List;

import org.eclipse.aether.graph.DependencyNode;

public class ResolverResult {
    private DependencyNode root;
    private List<File> resolvedFiles;
    private String resolvedClassPath;

    public ResolverResult(DependencyNode root, List<File> resolvedFiles, String resolvedClassPath) {
        this.root = root;
        this.resolvedFiles = resolvedFiles;
        this.resolvedClassPath = resolvedClassPath;
    }

    public DependencyNode getRoot() {
        return root;
    }

    public List<File> getResolvedFiles() {
        return resolvedFiles;
    }

    public String getResolvedClassPath() {
        return resolvedClassPath;
    }
}
