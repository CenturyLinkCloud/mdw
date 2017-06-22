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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Find mdw releases.  Supported page formats are Maven Central and
 * Tomcat Directory Listing.
 */
public class Crawl {

    private URL url;
    private boolean withSnapshots;

    List<String> releases;
    /**
     * @return releases sorted in ascending order
     */
    public List<String> getReleases() {
        return releases;
    }

    public Crawl(URL url) {
        this.url = url;
    }

    public Crawl(URL url, boolean withSnapshots) {
        this.url = url;
        this.withSnapshots = withSnapshots;
    }

    public void run() throws IOException {
        String page = retrieve();
        String release = withSnapshots ? "[\\d\\.]*(\\-SNAPSHOT)?" : "[\\d\\.]*";

        // try tomcat list format (internal repo)
        releases = findReleases(page, "<tt>", release, "/</tt>");
        if (releases.isEmpty()) {
            // try maven-central list format
            releases = findReleases(page, "<a href=\"", release, "/\"");
        }
    }

    private List<String> findReleases(String page, String start, String release, String end) {
        Pattern pattern = Pattern.compile(start + release + end);
        Matcher matcher = pattern.matcher(page);
        List<String> releases = new ArrayList<String>();
        while (matcher.find()) {
            String match = matcher.group();
            String rel = match.substring(start.replaceAll("\\\\", "").length(),
                    match.length() - end.replaceAll("\\\\", "").length());
            if (!releases.contains(rel) && !"..".equals(rel))
                releases.add(rel);
        }
        return releases;
    }

    private String retrieve() throws IOException {
        try (InputStream urlIn = new BufferedInputStream(url.openStream());
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len = urlIn.read(buffer);
            while (len >= 0) {
                out.write(buffer, 0, len);
                len = urlIn.read(buffer);
            }
            return out.toString();
        }
    }

}
