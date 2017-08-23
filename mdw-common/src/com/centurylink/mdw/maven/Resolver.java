/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.maven;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;

public class Resolver {

    private String remoteRepository;

    private RepositorySystem repositorySystem;

    private LocalRepository localRepository;

    public Resolver(String remoteRepository, String localRepository) {
        this.remoteRepository = remoteRepository;
        this.repositorySystem = Booter.newRepositorySystem();
        this.localRepository = new LocalRepository(localRepository);
    }

    private RepositorySystemSession newSession() {
        DefaultRepositorySystemSession session = Booter
                .newRepositorySystemSession(repositorySystem);
        session.setLocalRepositoryManager(
                repositorySystem.newLocalRepositoryManager(session, localRepository));
        return session;
    }

    public ResolverResult resolve(String groupId, String artifactId, String type, String version)
            throws DependencyResolutionException {
        RepositorySystemSession session = newSession();
        Dependency dependency = new Dependency(
                new DefaultArtifact(groupId, artifactId, "", type, version), "runtime");
        RemoteRepository central = new RemoteRepository.Builder("central", "default",
                remoteRepository).build();

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(dependency);
        collectRequest.addRepository(central);

        DependencyRequest dependencyRequest = new DependencyRequest();
        dependencyRequest.setCollectRequest(collectRequest);

        DependencyNode rootNode = repositorySystem.resolveDependencies(session, dependencyRequest)
                .getRoot();
        PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
        rootNode.accept(nlg);
        return new ResolverResult(rootNode, nlg.getFiles(), nlg.getClassPath());
    }
}
