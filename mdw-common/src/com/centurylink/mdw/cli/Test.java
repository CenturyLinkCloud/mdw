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
package com.centurylink.mdw.cli;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandNames="test", commandDescription="Run Automated Test(s)", separators="=")
public class Test extends Setup {

    @Parameter(names="--include", description="Test case include(s)")
    private String include = "**/*.test,**/*.postman";
    public String getInclude() { return include; }
    public void setInclude(String include) {
        this.include = include;
    }

    List<PathMatcher> getIncludeMatchers() {
        List<PathMatcher> matchers = new ArrayList<>();
        for (String in : include.trim().split("\\s*,\\s*")) {
            matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + in));
        }
        return matchers;
    }

    @Parameter(names="--exclude", description="Test case exclude(s)")
    private String exclude;
    public String getExclude() { return exclude; }
    public void setExclude(String exclude) {
        this.exclude = exclude;
    }

    List<PathMatcher> getExcludeMatchers() {
        List<PathMatcher> matchers = new ArrayList<>();
        if (exclude != null) {
            for (String ex : exclude.trim().split("\\s*,\\s*")) {
                matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + ex));
            }
        }
        return matchers;
    }

    @Override
    public Operation run(ProgressMonitor... progressMonitors) throws IOException {
        List<File> caseFiles = findCaseFiles();

        System.out.println("CASE FILES:");
        for (File caseFile : caseFiles) {
            System.out.println("  " + caseFile);
        }

        return this;
    }

    protected List<File> findCaseFiles() throws IOException {
        List<File> caseFiles = new ArrayList<>();
        List<PathMatcher> inMatchers = getIncludeMatchers();
        List<PathMatcher> exMatchers = getExcludeMatchers();
        Files.walkFileTree(Paths.get(getProjectDir().getPath()), new SimpleFileVisitor<Path>() {
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                if (matches(inMatchers, path) && !matches(exMatchers, path)) {
                    caseFiles.add(path.toFile());
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return caseFiles;
    }

    private boolean matches(List<PathMatcher> matchers, Path path) {
        for (PathMatcher matcher : matchers) {
            if (matcher.matches(path))
                return true;
        }
        return false;
    }
}
