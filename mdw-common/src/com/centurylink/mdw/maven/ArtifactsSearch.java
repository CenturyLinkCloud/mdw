/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.maven;
/* * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.Field;
import org.apache.maven.index.Indexer;
import org.apache.maven.index.IteratorSearchRequest;
import org.apache.maven.index.IteratorSearchResponse;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.expr.SourcedSearchExpression;
import org.apache.maven.index.updater.IndexUpdateRequest;
import org.apache.maven.index.updater.IndexUpdateResult;
import org.apache.maven.index.updater.IndexUpdater;
import org.apache.maven.index.updater.ResourceFetcher;
import org.apache.maven.index.updater.WagonHelper;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.observers.AbstractTransferListener;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.resolution.DependencyResolutionException;

public class ArtifactsSearch {

    private final PlexusContainer plexusContainer;

    private final Indexer indexer;

    private final IndexUpdater indexUpdater;

    private final Wagon httpWagon;

    private final String repoUrl;

    private final String groupId;

    private final String type;

    public ArtifactsSearch() throws PlexusContainerException, ComponentLookupException {
        // here we create Plexus container, the Maven default IoC container
        // Plexus falls outside of MI scope, just accept the fact that
        // MI is a Plexus component ;)
        // If needed more info, ask on Maven Users list or Plexus Users list
        // google is your friend!
        final DefaultContainerConfiguration config = new DefaultContainerConfiguration();
        config.setClassWorld(new ClassWorld("plexus.core", ClassWorld.class.getClassLoader()))
                .setName("mavenCore");

        this.plexusContainer = new DefaultPlexusContainer(config);

        // lookup the indexer components from plexus
        this.indexer = plexusContainer.lookup(Indexer.class);
        this.indexUpdater = plexusContainer.lookup(IndexUpdater.class);
        // lookup wagon used to remotely fetch index
        this.httpWagon = plexusContainer.lookup(Wagon.class, "http");
        this.repoUrl = "http://repo1.maven.org/maven2";
        this.groupId = "com.centurylink.mdw";
        this.type = "zip";

    }

    public Map<String, String> performSearch(boolean updateIndex)
            throws IOException, ComponentLookupException {
        // Files where local cache is (if any) and Lucene Index should be
        // located
        File centralLocalCache = new File("target/central-cache");
        File centralIndexDir = new File("target/central-index");

        // Creators we want to use (search for fields it defines)
        List<IndexCreator> indexers = new ArrayList<>();
        indexers.add((IndexCreator) plexusContainer.lookup(IndexCreator.class, "min"));
        indexers.add((IndexCreator) plexusContainer.lookup(IndexCreator.class, "jarContent"));
        indexers.add((IndexCreator) plexusContainer.lookup(IndexCreator.class, "maven-plugin"));

        // Create context for central repository index
        IndexingContext centralContext = indexer.createIndexingContext("central-context", "central",
                centralLocalCache, centralIndexDir, repoUrl, null, true, true, indexers);

        // Update the index (incremental update will happen if this is not 1st
        // run and files are not deleted)
        // This whole block below should not be executed on every app start, but
        // rather controlled by some configuration
        // since this block will always emit at least one HTTP GET. Central
        // indexes are updated once a week, but
        // other index sources might have different index publishing frequency.
        // Preferred frequency is once a week.
        if (updateIndex) {
            System.out.println("Updating Index...");
            System.out.println("This might take a while on first run, so please be patient!");
            // Create ResourceFetcher implementation to be used with
            // IndexUpdateRequest
            // Here, we use Wagon based one as shorthand, but all we need is a
            // ResourceFetcher implementation
            TransferListener listener = new AbstractTransferListener() {
                @Override
                public void transferStarted(TransferEvent transferEvent) {
                    System.out.print("  Downloading " + transferEvent.getResource().getName());
                }

                @Override
                public void transferCompleted(TransferEvent transferEvent) {
                    System.out.println(" - Done");
                }
            };
            ResourceFetcher resourceFetcher = new WagonHelper.WagonFetcher(httpWagon, listener,
                    null, null);

            Date centralContextCurrentTimestamp = centralContext.getTimestamp();
            IndexUpdateRequest updateRequest = new IndexUpdateRequest(centralContext,
                    resourceFetcher);
            IndexUpdateResult updateResult = indexUpdater.fetchAndUpdateIndex(updateRequest);
            if (updateResult.isFullUpdate()) {
                System.out.println("Full update happened!");
            }
            else if (updateResult.getTimestamp().equals(centralContextCurrentTimestamp)) {
                System.out.println("No update needed, index is up to date!");
            }
            else {
                System.out.println("Incremental update happened, change covered "
                        + centralContextCurrentTimestamp + " - " + updateResult.getTimestamp()
                        + " period.");
            }

            System.out.println();
        }

        System.out.println();
        System.out.println("Using index");
        System.out.println("===========");
        System.out.println();

        // construct the query for known G
        final Query groupIdQ = indexer.constructQuery(MAVEN.GROUP_ID,
                new SourcedSearchExpression(groupId));
        final BooleanQuery query = new BooleanQuery();
        query.add(groupIdQ, Occur.MUST);

        // we want "jar" artifacts only
        query.add(indexer.constructQuery(MAVEN.PACKAGING, new SourcedSearchExpression(type)),
                Occur.MUST);
        // we want main artifacts only (no classifier)
        // Note: this below is unfinished API, needs fixing
        query.add(indexer.constructQuery(MAVEN.CLASSIFIER,
                new SourcedSearchExpression(Field.NOT_PRESENT)), Occur.MUST_NOT);

        System.out.println("Searching for all the artifacts under the given group id for the given type");
        final IteratorSearchRequest request = new IteratorSearchRequest(query,
                Collections.singletonList(centralContext), null);
        final IteratorSearchResponse response = indexer.searchIterator(request);
        Map<String, String> artifactDetails = new HashMap<>();
        for (ArtifactInfo ai : response) {
            String artifactName = ai.artifactId;
            String artifactVersion = ai.version;
            String existingVersion = artifactDetails.get(artifactName);
            if (existingVersion != null) {
                if (existingVersion.compareTo(artifactVersion) < 0)
                    artifactDetails.put(artifactName, artifactVersion);
            }
            else
                artifactDetails.put(artifactName, artifactVersion);
        }

        // close cleanly
        indexer.closeIndexingContext(centralContext, false);
        return artifactDetails;
    }

    public static void main(String[] args) throws Exception {
        final ArtifactsSearch artifactsSearch = new ArtifactsSearch();
        artifactsSearch.downloadArtifact(artifactsSearch.performSearch(true));
    }

    public void downloadArtifact(Map<String, String> artifacts) throws DependencyResolutionException {
        for (Map.Entry<String, String> artifactEntry : artifacts.entrySet()) {
            Resolver resolver = new Resolver(repoUrl, "target/aether-repo");
            ResolverResult result = resolver.resolve(groupId,
                    artifactEntry.getKey(), type, artifactEntry.getValue());
            System.out.println(result.getResolvedFiles());
        }
    }
}
