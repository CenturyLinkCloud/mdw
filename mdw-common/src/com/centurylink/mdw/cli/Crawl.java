package com.centurylink.mdw.cli;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Find mdw releases.  Supported page formats are Maven Central and
 * Tomcat Directory Listing.
 */
public class Crawl implements Operation {

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

    public Crawl run(ProgressMonitor... progressMonitors) throws IOException {
        String page = new Fetch(url).run().getData();
        String release = withSnapshots ? "[\\d\\.]*(\\-SNAPSHOT)?" : "[\\d\\.]*";

        // try tomcat list format (internal repo)
        releases = findReleases(page, "<tt>", release, "/</tt>");
        if (releases.isEmpty()) {
            // try maven-central list format
            if (withSnapshots)
                releases = findReleases(page, "<a href=\"" + url, release, "/\"");
            else
                releases = findReleases(page, "<a href=\"", release, "/\"");
        }
        return this;
    }

    private List<String> findReleases(String page, String start, String release, String end) {
        Pattern pattern = Pattern.compile(start + release + end);
        Matcher matcher = pattern.matcher(page);
        List<String> releases = new ArrayList<>();
        while (matcher.find()) {
            String match = matcher.group();
            String rel = match.substring(start.replaceAll("\\\\", "").length(),
                    match.length() - end.replaceAll("\\\\", "").length());
            if (!releases.contains(rel) && !"..".equals(rel))
                releases.add(rel);
        }
        return releases;
    }

}
