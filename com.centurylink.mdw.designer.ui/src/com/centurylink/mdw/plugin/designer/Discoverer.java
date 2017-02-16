/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.designer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.IProgressMonitor;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.centurylink.mdw.common.utilities.HttpHelper;
import com.centurylink.mdw.plugin.CodeTimer;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.designer.model.File;
import com.centurylink.mdw.plugin.designer.model.Folder;

public class Discoverer {
    private URL url;
    private boolean latestVersionsOnly;
    private IProgressMonitor progressMonitor;
    private int topLevelFolders;

    public Discoverer(URL url) {
        this.url = url;
    }

    public Folder getAssetTopFolder(boolean latestVersionsOnly, IProgressMonitor progressMonitor)
            throws DiscoveryException, IOException, InterruptedException {
        this.latestVersionsOnly = latestVersionsOnly;
        this.progressMonitor = progressMonitor;
        progressMonitor.worked(10);
        Folder topFolder = new Folder(url.toString());
        CodeTimer timer = new CodeTimer("crawl for assets");
        crawlForPackageFiles(topFolder);
        timer.stopAndLog();
        return topFolder;
    }

    /**
     * Crawls to find any package XML or JSON files.
     */
    private void crawlForPackageFiles(Folder parent)
            throws DiscoveryException, IOException, InterruptedException {
        if (progressMonitor.isCanceled())
            throw new InterruptedException();
        String parentUrl = getFullUrl(parent);
        HttpHelper httpHelper = new HttpHelper(new URL(parentUrl));
        httpHelper.setConnectTimeout(MdwPlugin.getSettings().getHttpConnectTimeout());
        httpHelper.setReadTimeout(MdwPlugin.getSettings().getHttpReadTimeout());

        if (!parent.hasParent()) // topLevel
            httpHelper.setFollowHtmlHeadMetaRefresh(true);

        String content = httpHelper.get();
        if (content.startsWith("<!"))
            content = content.substring(content.indexOf("\n") + 1);
        if (content.contains("&nbsp;"))
            content = content.replaceAll("&nbsp;", "")
                    .replace("<HR size=\"1\" noshade=\"noshade\">", "");
        if (!parent.hasParent() && httpHelper.getRedirect() != null)
            parent.setName(httpHelper.getRedirect().toString());
        try {
            List<String> links = parseDirectoryResponse(content);
            if (!parent.hasParent())
                topLevelFolders = links.size();
            if (latestVersionsOnly) {
                List<String> latestLinks = new ArrayList<String>();
                String latestDir = null;
                for (String link : links) {
                    if (!link.endsWith("-SNAPSHOT/")) // snapshots excluded from
                                                      // "latest only"
                    {
                        if (link.matches("[0-9.]*/")) {
                            if ((latestDir == null || latestDir.compareTo(link) < 0))
                                latestDir = link;
                        }
                        else {
                            latestLinks.add(link);
                        }
                    }
                }

                if (latestDir != null)
                    latestLinks.add(latestDir);

                links = latestLinks;
            }

            for (String link : links) {
                if (link.endsWith("/") && (MdwPlugin.getSettings().isIncludePreviewBuilds()
                        || !link.endsWith("-SNAPSHOT/"))) {
                    // directory
                    if (!parent.hasParent())
                        progressMonitor.subTask("Scanning " + link);
                    Folder child = new Folder(link.substring(0, link.length() - 1));
                    if (link.matches("[0-9.]*/"))
                        parent.addChild(0, child);
                    else
                        parent.addChild(child);
                    crawlForPackageFiles(child);
                    if (!parent.hasParent()) // topLevel
                        progressMonitor.worked(80 / topLevelFolders);
                }
                else if (link.endsWith(".xml") || link.endsWith(".json")) {
                    // XML or JSON file
                    File child = new File(parent, link);
                    parent.addChild(0, child);
                    child.setUrl(new URL(getFullUrl(child)));
                }
            }
        }
        catch (InterruptedException iex) {
            throw iex;
        }
        catch (Exception ex) {
            throw new DiscoveryException("Error crawling: " + parentUrl, ex);
        }
    }

    public String getFullUrl(Folder folder) {
        String url = folder.getName();
        Folder parent = folder;
        while ((parent = folder.getParent()) != null) {
            if (parent.getName().endsWith("/"))
                url = parent.getName() + url;
            else
                url = parent.getName() + "/" + url;
            folder = parent;
        }
        return url;
    }

    public String getFullUrl(File file) {
        String url = getFullUrl((Folder) file.getParent());
        if (url.endsWith("/"))
            url += file.getName();
        else
            url += "/" + file.getName();
        return url;
    }

    /**
     * Use SAX for quick processing.
     */
    private List<String> parseDirectoryResponse(String content)
            throws IOException, SAXException, ParserConfigurationException {
        final List<String> urls = new ArrayList<String>();
        InputStream xmlStream = new ByteArrayInputStream(content.getBytes());
        InputSource src = new InputSource(xmlStream);
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        SAXParser parser = parserFactory.newSAXParser();
        parser.parse(src, new DefaultHandler() {
            boolean inHtml;
            boolean inUl;
            boolean inLi;
            boolean inA;
            String a;
            String href;

            public void startElement(String uri, String localName, String qName, Attributes attrs)
                    throws SAXException {
                if (qName.equals("html"))
                    inHtml = true;
                else if (qName.equals("ul") || qName.equals("tr")) // New Apache
                                                                   // format
                                                                   // uses "tr"
                                                                   // and "td",
                                                                   // instead of
                                                                   // "ul" and
                                                                   // "li"
                                                                   // elements
                    inUl = true;
                else if (qName.equals("li") || qName.equals("td"))
                    inLi = true;
                else if (qName.equals("a"))
                    inA = true;

                if (inHtml && inUl && inLi && inA && href == null)
                    href = attrs.getValue("href");
            }

            public void endElement(String uri, String localName, String qName) throws SAXException {

                if (qName.equals("a") && inHtml && inUl && inLi && href != null
                        && (href.equals(a) || href.equals(a + "/")
                                || href.substring(href.lastIndexOf("/", href.length() - 2) + 1)
                                        .equals(a))) {
                    if (!href.startsWith("/") && !href.startsWith("../")) // parent
                                                                          // directories
                        urls.add(href);
                    else if (!href.substring(href.lastIndexOf("/", href.length() - 2) + 1)
                            .startsWith("/")) // For new Apache directory
                                              // listing format
                        urls.add(href.substring(href.lastIndexOf("/", href.length() - 2) + 1));
                }

                if (qName.equals("html"))
                    inHtml = false;
                else if (qName.equals("ul") || qName.equals("tr"))
                    inUl = false;
                else if (qName.equals("li") || qName.equals("td"))
                    inLi = false;
                else if (qName.equals("a")) {
                    inA = false;
                    href = null;
                }
            }

            public void characters(char[] ch, int start, int length) throws SAXException {
                if (inHtml && inUl && inLi && inA)
                    a = new String(ch).substring(start, start + length).trim();
                else
                    a = null;
            }
        });

        return urls;
    }
}
