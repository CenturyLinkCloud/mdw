package com.centurylink.mdw.git;

import com.centurylink.mdw.git.GitDiffs.DiffType;
import com.centurylink.mdw.model.asset.CommitInfo;
import com.centurylink.mdw.file.VersionProperties;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.pgm.Main;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class VersionControlGit {

    /**
     * should be sufficient, but we could change to 8 or more
     * (com.centurylink.mdw.abbreviated.id.length system property)
     */
    private String repositoryUrl;

    private File localDir;
    public File getLocalDir() { return localDir; }

    private Repository localRepo;
    private Git git;

    private CredentialsProvider credentialsProvider;
    public void setCredentialsProvider(CredentialsProvider provider) { this.credentialsProvider = provider; }

    private final boolean allowFetch;

    /**
     * For CLI.
     */
    public VersionControlGit() {
        this(false);
    }

    public VersionControlGit(boolean allowFetch) {
        this.allowFetch = allowFetch;
    }

    public void connect(String repositoryUrl, String user, String password, File localDir) throws IOException {

        if (repositoryUrl != null && !repositoryUrl.isEmpty()) {
            if (user == null) {
                this.repositoryUrl = repositoryUrl;
            }
            else {
                int slashSlash = repositoryUrl.indexOf("//");
                this.repositoryUrl = repositoryUrl.substring(0, slashSlash + 2) + user + ":" + password + "@" + repositoryUrl.substring(slashSlash + 2);
            }
        }

        this.localDir = localDir;
        localRepo = new FileRepository(new File(localDir + "/.git"));
        git = new Git(localRepo);
        if (credentialsProvider == null) {
            if (user != null && password != null)
              credentialsProvider = new UsernamePasswordCredentialsProvider(user, password);
        }
    }

    public synchronized void reconnect() throws IOException {
        Repository newLocalRepo = new FileRepository(new File(localDir + "/.git"));
        git = new Git(newLocalRepo);
        localRepo.close();
        localRepo = newLocalRepo;
    }

    public String toString() {
        return localDir + "->" + repositoryUrl;
    }

    public void deleteRev(File file) throws IOException {
        VersionProperties verProps = getVersionProps(file.getParentFile());
        String val = verProps.getProperty(file.getName());
        if (val != null) {
            verProps.remove(file.getName());
            verProps.save();
        }
    }

    private VersionProperties getVersionProps(File pkgDir) throws IOException {
        File file = new File(pkgDir + "/.mdw/versions");
        if (file.exists()) {
            return new VersionProperties(file);
        }
        else if (!file.getAbsolutePath().contains(localDir.getAbsolutePath())) {
            return getVersionProps(new File(localDir.getAbsolutePath() + "/" + pkgDir.getPath()));
        }
        throw new FileNotFoundException("Version props not found for " + pkgDir);
    }

    public String getCommit() throws IOException {
        ObjectId head = localRepo.resolve(Constants.HEAD);
        if (head != null)
            return head.getName();
        else
            return null;
    }

    public String getCommitForTag(String tag) throws Exception {
        fetch();
        List<Ref> tagRefs = git.tagList().call();
        for (Ref tagRef : tagRefs) {
            if (tagRef.getName().equals("refs/tags/" + tag))
                return tagRef.getObjectId().name();
        }

        return null;
    }

    /**
     * Get remote HEAD commit.
     */
    public String getRemoteCommit(String branch) throws Exception {
        fetch();
        ObjectId commit = localRepo.resolve("origin/" + branch);
        if (commit != null)
            return commit.getName();
        else
            return null;
    }

    public long getCommitTime(String commitId) throws Exception {
        try (RevWalk revWalk = new RevWalk(localRepo)) {
            return revWalk.parseCommit(ObjectId.fromString(commitId)).getCommitterIdent().getWhen().getTime();
        }
    }

    public String getBranch() throws IOException {
        return localRepo.getBranch();
    }

    public List<GitBranch> getRemoteBranches() throws Exception {
        fetch();
        List<Ref> refs = new Git(localRepo).branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call();
        List<GitBranch> branches = new ArrayList<>();
        for (Ref ref : refs) {
            String name = ref.getName();
            if (name.startsWith("refs/remotes/origin/"))
                name = name.substring(20);
            branches.add(new GitBranch(ObjectId.toString(ref.getObjectId()), name));
        }
        return branches;
    }

    /**
     * Does not do anything if already on target branch.
     */
    public void checkout(String branch) throws Exception {
        if (!branch.equals(getBranch())) {
            fetch(); // in case the branch is not known locally
            createBranchIfNeeded(branch);
            CheckoutCommand checkout = git.checkout().setName(branch)
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK);
            checkout.setStartPoint("origin/" + branch);
            checkout.call();
            // for some reason jgit needs this when branch is switched
            git.checkout().setName(branch).call();
        }
    }

    public void checkoutTag(String tag) throws Exception {
        if (localRepo.getTags().get(tag) != null)
            git.checkout().setName(localRepo.getTags().get(tag).getName()).call();
    }

    public void hardCheckout(String branch) throws Exception {
        hardCheckout(branch, false);
    }

    public void hardTagCheckout(String tag) throws Exception {
        hardTagCheckout(tag, false);
    }

    /**
     * Performs a HARD reset and FORCED checkout.
     */
    public void hardCheckout(String branch, Boolean hard) throws Exception {
        fetch(); // in case the branch is not known locally
        if (hard)
            hardReset();
        checkout(branch);
        pull(branch);  // pull before delete or next pull may add non-path items back
    }

    public void hardTagCheckout(String tag, Boolean hard) throws Exception {
        fetch(); // in case the tag is not known locally
        if (hard)
            hardReset();
        checkoutTag(tag);
    }

    public void hardReset() throws Exception {
        git.reset().setMode(ResetType.HARD).call();
        git.clean().call();
    }

    /**
     * Create a local branch for remote tracking if it doesn't exist already.
     */
    protected void createBranchIfNeeded(String branch) throws Exception {
        if (localRepo.findRef(branch) == null) {
            git.branchCreate()
               .setName(branch)
               .setUpstreamMode(SetupUpstreamMode.TRACK)
               .setStartPoint("origin/" + branch)
               .call();
        }
    }

    public void commit(String path, String msg) throws Exception {
        git.commit().setOnly(path).setMessage(msg).call();
    }

    public void commit(List<String> paths, String msg) throws Exception {
        CommitCommand commit = git.commit().setMessage(msg);
        for (String path : paths)
            commit.setOnly(path);
        commit.call();
    }

    public void commit(String msg) throws Exception {
        git.commit().setMessage(msg).call();
    }

    public void push() throws Exception {
        PushCommand push = git.push();
        if (credentialsProvider != null)
            push.setCredentialsProvider(credentialsProvider);
        push.call();
    }

    public void fetch() throws Exception {
        if (allowFetch) {
            FetchCommand fetchCommand = git.fetch().setRemoveDeletedRefs(true);
            if (credentialsProvider != null)
                fetchCommand.setCredentialsProvider(credentialsProvider);
            try {
                fetchCommand.call();
            }
            catch (JGitInternalException | TransportException ex) {
                // LocalRepo object might be out of sync with actual local repo, so recreate objects for next time
                reconnect();
                throw ex;
            }
        }
    }

    public void clone(String branch, ProgressMonitor progressMonitor) throws Exception {
        CloneCommand clone = Git.cloneRepository()
                .setURI(repositoryUrl)
                .setDirectory(localRepo.getDirectory().getParentFile());
        if (branch != null)
            clone.setBranch(branch);
        if (credentialsProvider != null)
            clone.setCredentialsProvider(credentialsProvider);
        if (progressMonitor != null)
            clone.setProgressMonitor(progressMonitor);
        clone.call();
    }

    public Status getStatus() throws Exception {
        return getStatus(null);
    }

    public Status getStatus(String path) throws Exception {
        fetch();
        StatusCommand sc = git.status();
        if (path != null)
            sc.addPath(path);
        return sc.call();
    }

    public void add(String path) throws Exception {
        git.add().addFilepattern(path).call();
    }

    public void add(List<String> paths) throws Exception {
        AddCommand add = git.add();
        for (String path : paths)
            add.addFilepattern(path);
        add.call();
    }

    public void rm(String path) throws Exception {
        git.rm().addFilepattern(path).call();
    }

    public void cloneNoCheckout() throws Exception {
        cloneNoCheckout(false);
    }

    /**
     * In lieu of sparse checkout since it's not supported in JGit:
     * https://bugs.eclipse.org/bugs/show_bug.cgi?id=383772
     */
    public void cloneNoCheckout(boolean withProgress) throws Exception {
        CloneCommand clone = Git.cloneRepository().setURI(repositoryUrl).setDirectory(localRepo.getDirectory().getParentFile()).setNoCheckout(true);
        if (credentialsProvider != null)
            clone.setCredentialsProvider(credentialsProvider);
        if (withProgress)
            clone.setProgressMonitor(new TextProgressMonitor(new PrintWriter(System.out)));
        clone.call();
    }

    public boolean localRepoExists() {
        return localRepo.getDirectory() != null && localRepo.getDirectory().exists();
    }

    public void pull(String branch) throws Exception {
        pull(branch, null);
    }

    public void pull(String branch, ProgressMonitor progressMonitor) throws Exception {
        PullCommand pull = git.pull().setRemote("origin").setRemoteBranchName(branch);
        if (credentialsProvider != null)
            pull.setCredentialsProvider(credentialsProvider);
        if (progressMonitor != null)
            pull.setProgressMonitor(progressMonitor);
        pull.call();
    }

    /**
     * Creates a new local branch and pushes to remote.
     * @return ref id of newly-created branch
     */
    public String createBranch(String branch, String fromBranch, ProgressMonitor progressMonitor) throws Exception {
        Ref ref = localRepo.findRef(branch);
        if (ref == null) {
            CreateBranchCommand createBranch = git.branchCreate().setName(branch);
            if (fromBranch != null)
                createBranch.setStartPoint(fromBranch);
            ref = createBranch.call();
        }
        PushCommand push = git.push();
        push.setRemote("origin");
        push.setRefSpecs(new RefSpec(branch));
        if (credentialsProvider != null)
            push.setCredentialsProvider(credentialsProvider);
        if (progressMonitor != null)
            push.setProgressMonitor(progressMonitor);
        push.call();

        return ObjectId.toString(ref.getObjectId());
    }

    @SuppressWarnings("unused")
    public String getRelativePath(Path path) {
        return localDir.toPath().normalize().relativize(path.normalize()).toString().replace('\\', '/');
    }

    public GitDiffs getDiffs(String branch, String path) throws Exception {
        fetch();
        ObjectId obj = localRepo.resolve("origin/" + branch + "^{tree}");
        if (obj == null)
            throw new IOException("Unable to determine Git Diffs: path " + path + " not found on branch " + branch);
        return getDiffs(obj, path);
    }

    @SuppressWarnings("unused")
    public GitDiffs getDiffsForTag(String tag, String path) throws Exception {
        fetch();
        ObjectId obj = localRepo.resolve("refs/tags/" + tag + "^{tree}");
        if (obj == null)
            throw new IOException("Unable to determine Git Diffs: path " + path + " not found on tag " + tag);
        return getDiffs(obj, path);
    }

    private GitDiffs getDiffs(ObjectId objectId, String path) throws Exception {
        GitDiffs diffs = new GitDiffs();
        CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
        newTreeIter.reset(localRepo.newObjectReader(), objectId);
        DiffCommand dc = git.diff().setNewTree(newTreeIter);
        if (path != null)
            dc.setPathFilter(PathFilter.create(path));
        dc.setShowNameAndStatusOnly(true);
        for (DiffEntry diff : dc.call()) {
            if (diff.getChangeType() == ChangeType.ADD || diff.getChangeType() == ChangeType.COPY) {
                diffs.add(DiffType.MISSING, diff.getNewPath());
            }
            else if (diff.getChangeType() == ChangeType.MODIFY) {
                diffs.add(DiffType.DIFFERENT, diff.getNewPath());
            }
            else if (diff.getChangeType() == ChangeType.DELETE) {
                diffs.add(DiffType.EXTRA, diff.getOldPath());
            }
            else if (diff.getChangeType() == ChangeType.RENAME) {
                diffs.add(DiffType.MISSING, diff.getNewPath());
                diffs.add(DiffType.EXTRA, diff.getOldPath());
            }
        }
        // we're purposely omitting folders
        Status status = git.status().addPath(path).call();
        for (String untracked : status.getUntracked()) {
            if (!untracked.startsWith(path + "/Archive/"))
                diffs.add(DiffType.EXTRA, untracked);
        }
        for (String added : status.getAdded()) {
            diffs.add(DiffType.EXTRA, added);
        }
        for (String missing : status.getMissing()) {
            diffs.add(DiffType.MISSING, missing);
        }
        for (String removed : status.getRemoved()) {
            diffs.add(DiffType.MISSING, removed);
        }
        for (String changed : status.getChanged()) {
            diffs.add(DiffType.DIFFERENT, changed);
        }
        for (String modified : status.getModified()) {
            diffs.add(DiffType.DIFFERENT, modified);
        }
        for (String conflict : status.getConflicting()) {
            diffs.add(DiffType.DIFFERENT, conflict);
        }
        return diffs;
    }

    /**
     * Does not fetch.
     */
    public CommitInfo getCommitInfo(String path) throws Exception {
        Iterator<RevCommit> revCommits = git.log().addPath(path).setMaxCount(1).call().iterator();
        if (revCommits.hasNext()) {
            return getCommitInfo(revCommits.next());
        }
        return null;
    }

    public List<CommitInfo> getCommits(String path) throws Exception {
        List<CommitInfo> commits = new ArrayList<>();
        Iterator<RevCommit> revCommits = git.log().addPath(path).call().iterator();
        while (revCommits.hasNext()) {
            commits.add(getCommitInfo(revCommits.next()));
        }
        return commits;
    }

    public CommitInfo getCommitInfoForRef(String ref) throws Exception {
        ObjectId commitId = ObjectId.fromString(ref);
        try (RevWalk revWalk = new RevWalk(localRepo)) {
            RevCommit revCommit = revWalk.parseCommit(commitId);
            if (revCommit != null) {
                return getCommitInfo(revCommit);
            }
        }
        return null;
    }

    private CommitInfo getCommitInfo(RevCommit revCommit) {
        CommitInfo commitInfo = new CommitInfo(revCommit.getId().name());
        PersonIdent committerIdent = revCommit.getCommitterIdent();
        commitInfo.setCommitter(committerIdent.getName());
        commitInfo.setEmail(committerIdent.getEmailAddress());
        if ((commitInfo.getCommitter() == null || commitInfo.getCommitter().isEmpty()) && commitInfo.getEmail() != null)
            commitInfo.setCommitter(commitInfo.getEmail());
        commitInfo.setDate(committerIdent.getWhen());
        commitInfo.setMessage(revCommit.getShortMessage());
        return commitInfo;
    }

    public ObjectStream getRemoteContentStream(String branch, String path) throws Exception {
        ObjectId id = localRepo.resolve("refs/remotes/origin/" + branch);
        try (ObjectReader reader = localRepo.newObjectReader();
                RevWalk walk = new RevWalk(reader)) {
            RevCommit commit = walk.parseCommit(id);
            RevTree tree = commit.getTree();
            TreeWalk treewalk = TreeWalk.forPath(reader, path, tree);
            if (treewalk != null) {
                return reader.open(treewalk.getObjectId(0)).openStream();
            }
            else {
                return null;
            }
        }
    }

    public String getRemoteContentString(String branch, String path) throws Exception {
        InputStream in = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            in = getRemoteContentStream(branch, path);
            if (in == null)
                return null;
            int read;
            byte[] bytes = new byte[1024];
            while ((read = in.read(bytes)) != -1)
                out.write(bytes, 0, read);
        }
        finally {
            if (in != null)
                in.close();
        }
        return out.toString();
    }

    public boolean exists() {
        return new File(localDir + "/.git").isDirectory();
    }

    /**
     * Execute an arbitrary git command.
     */
    @SuppressWarnings("unused")
    public void git(String... args) throws Exception {
        Main.main(args);
    }

    public byte[] readFromCommit(String commitId, String path) throws Exception {
        try (RevWalk revWalk = new RevWalk(localRepo)) {
            RevCommit commit = revWalk.parseCommit(ObjectId.fromString(commitId));
            // use commit's tree to find the path
            RevTree tree = commit.getTree();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (TreeWalk treeWalk = new TreeWalk(localRepo)) {
                treeWalk.addTree(tree);
                treeWalk.setRecursive(true);
                treeWalk.setFilter(PathFilter.create(path));
                if (!treeWalk.next()) {
                    return null;
                }

                ObjectId objectId = treeWalk.getObjectId(0);
                ObjectLoader loader = localRepo.open(objectId);

                loader.copyTo(baos);
            }
            revWalk.dispose();
            return baos.toByteArray();
        }
    }

    /**
     * Find package assets that are present at the specified commit.
     */
    public List<String> getAssetsAtCommit(String commitId, String packagePath) throws Exception {
        try (RevWalk revWalk = new RevWalk(localRepo)) {
            RevCommit commit = revWalk.parseCommit(ObjectId.fromString(commitId));
            // use commit's tree to find the path
            RevTree tree = commit.getTree();
            try (TreeWalk treeWalk = new TreeWalk(localRepo)) {
                treeWalk.addTree(tree);
                treeWalk.setRecursive(true);
                treeWalk.setFilter(PathFilter.create(packagePath));
                List<String> assets = new ArrayList<>();
                while (treeWalk.next()) {
                    if (treeWalk.getPathString().equals(packagePath + "/" + treeWalk.getNameString())) {
                        // direct member of package
                        assets.add(treeWalk.getNameString());
                    }
                }
                return assets;
            }
            finally {
                revWalk.dispose();
            }
        }
    }

    @SuppressWarnings("unused")
    public byte[] readFromHead(String filePath) throws Exception {
        try {
            return readFromCommit(ObjectId.toString(localRepo.resolve(Constants.HEAD)), filePath);
        }
        catch (Exception e) {  // MDW Studio throws MissingObjectExceptions after a pull, on Windows at least
            reconnect();
            return readFromCommit(ObjectId.toString(localRepo.resolve(Constants.HEAD)), filePath);
        }
    }

    public void merge(String fromBranch, String toBranch) throws Exception {
        fetch();
        CheckoutCommand checkout = git.checkout().setName(toBranch).setCreateBranch(false);
        checkout.call();
        ObjectId fromId = localRepo.resolve("refs/remotes/origin/" + fromBranch);
        MergeCommand merge = git.merge().include(fromId);
        MergeResult mergeResult = merge.call();
        if (mergeResult.getMergeStatus().equals(MergeResult.MergeStatus.CONFLICTING)){
            throw new IOException("Merge conflicts: " + mergeResult.getConflicts());
        }
    }
}
