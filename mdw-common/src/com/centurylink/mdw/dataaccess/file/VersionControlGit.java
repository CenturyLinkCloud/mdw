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
package com.centurylink.mdw.dataaccess.file;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode;
import org.eclipse.jgit.api.DiffCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.ObjectStream;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.notes.Note;
import org.eclipse.jgit.notes.NoteMap;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;

import com.centurylink.mdw.config.PropertyException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.dataaccess.AssetRevision;
import com.centurylink.mdw.dataaccess.VersionControl;
import com.centurylink.mdw.dataaccess.file.GitDiffs.DiffType;
import com.centurylink.mdw.model.asset.CommitInfo;
import com.centurylink.mdw.util.file.FileHelper;

/**
 * TODO: Caching of VCS info (probably not need for file system info).
 */
public class VersionControlGit implements VersionControl {

    public static final String HEAD_REF = "refs/heads/master";
    public static final String NOTES_REF = "refs/notes/mdw";
    public static final String VERSIONS_FILE = ".mdw/versions";
    public static final char NEWLINE_CHAR = 0x0a;

    /**
     * should be sufficient, but we could change to 8 or more
     * (com.centurylink.mdw.abbreviated.id.length system property)
     */
    public static int ABBREVIATED_ID_LENGTH = 7;

    private String repositoryUrl;
    protected String getRepositoryUrl() { return repositoryUrl; }

    private File localDir;
    private Repository localRepo;
    private Git git;

    private Map<File,Long> file2id;
    private Map<Long,File> id2file;

    boolean gitNotesVersioning = false;
    // only one of the following is used
    private Map<File,AssetRevision> file2rev;
    private Map<File,Properties> pkg2versions;

    private CredentialsProvider credentialsProvider;
    public CredentialsProvider getCredentialsProvider() { return this.credentialsProvider; };
    public void setCredentialsProvider(CredentialsProvider provider) { this.credentialsProvider = provider; }

    public VersionControlGit() {
        System.out.println("CREATING VC Git: " + this.hashCode());
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

        file2id = new HashMap<File,Long>();
        id2file = new HashMap<Long,File>();
        if (gitNotesVersioning)
            file2rev = new HashMap<File,AssetRevision>();
        else
            pkg2versions = new HashMap<File,Properties>();

        String idLengthProp = System.getProperty("com.centurylink.mdw.abbreviated.id.length");
        if (idLengthProp != null)
            ABBREVIATED_ID_LENGTH = Integer.parseInt(idLengthProp);
    }

    public String toString() {
        return localDir + "->" + repositoryUrl;
    }

    /**
     * Cannot use git object hash since identical files would return duplicate
     * ids. Hash using git algorithm based on the relative file path.
     */
    public long getId(File file) throws IOException {
        Long id = file2id.get(file);
        if (id == null) {
            id = gitHash(file);
            file2id.put(file, id);
            id2file.put(id, file);
        }
        return id;
    }

    protected long getLongId(ObjectId objectId) {
        String h = objectId.abbreviate(ABBREVIATED_ID_LENGTH).name();
        return Long.parseLong(h, 16);
    }

    public File getFile(long id) {
        return id2file.get(id);
    }

    public void clearId(File file) {
        Long id = file2id.remove(file);
        if (id != null)
            id2file.remove(id);
    }

    public void clear() {
        file2id.clear();
        id2file.clear();
        if (gitNotesVersioning)
            file2rev.clear();
        else
            pkg2versions.clear();
    }

    public void deleteRev(File file) throws IOException {
        if (gitNotesVersioning) {
            file2rev.remove(file); // TODO remove git note
        }
        else {
            Properties verProps = getVersionProps(file.getParentFile());
            String val = verProps.getProperty(file.getName());
            if (val != null) {
                verProps.remove(file.getName());
                saveVersionProps(file.getParentFile());
            }
        }
    }

    public AssetRevision getRevision(File file) throws IOException {
        return getRevisionInVersionsFile(file);
    }

    public void setRevision(File file, AssetRevision rev) throws IOException {
        setRevisionInVersionsFile(file, rev);
    }

    public AssetRevision getRevisionInVersionsFile(File file) throws IOException {
        Properties verProps = getVersionProps(file.getParentFile());
        String propVal = verProps.getProperty(file.getName());
        if (propVal == null) {
            return null;
        }
        else {
            return parseAssetRevision(propVal.trim());
        }
    }

    public void setRevisionInVersionsFile(File file, AssetRevision rev) throws IOException {
        Properties verProps = getVersionProps(file.getParentFile());
        verProps.setProperty(file.getName(), formatVersionProp(rev));
        saveVersionProps(file.getParentFile());
    }

    public static AssetRevision parseAssetRevision(String propertyValue) {
        AssetRevision assetRevision = new AssetRevision();
        int firstSpace = propertyValue.indexOf(' ');
        if (firstSpace > 0) {
            // includes comment
            assetRevision.setVersion(Integer.parseInt(propertyValue.substring(0, firstSpace)));
            assetRevision.setComment(propertyValue.substring(firstSpace + 1).replace(NEWLINE_CHAR, '\n'));
        }
        else {
            assetRevision.setVersion(Integer.parseInt(propertyValue));
        }
        return assetRevision;
    }

    private String formatVersionProp(AssetRevision assetRevision) {
        String propVal = String.valueOf(assetRevision.getVersion());
        if (assetRevision.getComment() != null)
            propVal += " " + assetRevision.getComment().replaceAll("\\r", "").replace('\n', NEWLINE_CHAR);
        return propVal;
    }

    private Properties getVersionProps(File pkgDir) throws IOException {
        Properties props = pkg2versions.get(pkgDir);
        if (props == null) {
            // make sure the keys are sorted in a predictable order
            // also avoid setting date comment which causes Git conflicts
            props = new Properties() {
                @Override
                public synchronized Enumeration<Object> keys() {
                    return Collections.enumeration(new TreeSet<Object>(super.keySet()));
                }
                @Override
                public void store(OutputStream out, String comments) throws IOException {
                    if (comments != null) {
                        super.store(out, comments);
                    }
                    else {
                        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out, "8859_1"));
                        synchronized (this) {
                            for (Enumeration<?> e = keys(); e.hasMoreElements();) {
                                String key = (String)e.nextElement();
                                String val = (String)get(key);
                                bw.write(key.replaceAll(" ", "\\\\ ").replaceAll("!", "\\\\!") + "=" + val);
                                bw.newLine();
                            }
                        }
                        bw.flush();
                    }
                }
            };
            File propFile = new File(pkgDir + "/" + VERSIONS_FILE);
            if (propFile.exists()) {
                InputStream in = null;
                try {
                    in = new FileInputStream(propFile);
                    props.load(in);
                }
                finally {
                    if (in != null)
                        in.close();
                }
            }
            pkg2versions.put(pkgDir, props);
        }
        return props;
    }

    private void saveVersionProps(File pkgDir) throws IOException {
        Properties props = getVersionProps(pkgDir);
        File propFile = new File(pkgDir + "/" + VERSIONS_FILE);
        if (props.isEmpty()) {
            if (propFile.exists() && !propFile.delete())
                throw new IOException("Unable to delete file: " + propFile);
        }
        else {
            OutputStream out = null;
            try {
                out = new FileOutputStream(propFile);
                props.store(out, null);
            }
            catch (FileNotFoundException ex) {
                // dimensions annoyingly makes files read-only
                propFile.setWritable(true);
                out = new FileOutputStream(propFile);
                props.store(out, null);
            }
            finally {
                if (out != null)
                    out.close();
            }
        }
    }

    /**
     * Storing revisions in Git Notes causes object hash computation, which is expensive.
     */
    public AssetRevision getRevisionGitNotes(File file) throws IOException {
        AssetRevision rev = file2rev.get(file);
        if (rev == null) {
            ObjectId workingId = getObjectId(file);

            RevWalk walk = new RevWalk(localRepo);
            try {
                Ref ref = localRepo.getRef(NOTES_REF);
                if (ref != null) {
                    RevCommit notesCommit = walk.parseCommit(ref.getObjectId());
                    NoteMap notes = NoteMap.read(walk.getObjectReader(), notesCommit);
                    Note note = notes.getNote(workingId);
                    if (note != null) {
                        ObjectLoader loader = localRepo.open(note.getData());
                        String noteStr = new String(loader.getBytes()).trim();
                        rev = parseAssetRevision(noteStr);
                        file2rev.put(file, rev);
                        return rev;
                    }
                }
            }
            finally {
                walk.release();
            }
        }

        return rev;
    }

    /**
     * Storing revisions in Git Notes causes object hash computation, which is expensive.
     */
    public void setRevisionGitNotes(File file, AssetRevision rev) throws IOException {
        ObjectId workingId = getObjectId(file);
        RevWalk walk = new RevWalk(localRepo);
        try {
            RevObject notesObj;
            try {
                notesObj = walk.parseAny(workingId);
            }
            catch (MissingObjectException ex) {
                // new files need "git add" before attaching notes
                git.add().addFilepattern(getRepoPath(file)).call();
                notesObj = walk.parseAny(workingId);
            }

            String note = formatVersionProp(rev);
            git.notesAdd().setNotesRef(NOTES_REF).setMessage(note).setObjectId(notesObj).call();
        }
        catch (Exception ex) {
            throw new IOException(ex.getMessage(), ex);
        }
        finally {
            walk.dispose();
        }
    }

    /**
     * Use git hashing algorithm.
     * http://stackoverflow.com/questions/7225313/how-does-git-compute-file-hashes
     */
    protected Long gitHash(File input) throws IOException {
        String path = getLogicalPath(input);
        String blob = "blob " + path.length() + "\0" + path;
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
            byte[] bytes = md.digest(blob.getBytes());
            String h = byteArrayToHexString(bytes).substring(0, 7);
            return Long.parseLong(h, 16);
        }
        catch (NoSuchAlgorithmException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }

    /**
     * Seems slightly slower than gitHash().
     */
    protected ObjectId gitHashJgit(File input) throws IOException {
        String path = getLogicalPath(input);
        String blob = "blob " + path.length() + "\0" + path;
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
            byte[] bytes = md.digest(blob.getBytes());
            return ObjectId.fromRaw(bytes);
        }
        catch (NoSuchAlgorithmException ex) {
            throw new IOException(ex.getMessage(), ex);
        }
    }

    /**
     * TODO: see if performance can be improved
     */
    private ObjectId getObjectId(File file) throws IOException {
        TreeWalk treeWalk = new TreeWalk(localRepo);
        treeWalk.addTree(new FileTreeIterator(localRepo));

        String path = getRepoPath(file);
        treeWalk.setFilter(PathFilter.create(path));

        while (treeWalk.next()) {
            WorkingTreeIterator workingTreeIterator = treeWalk.getTree(0, WorkingTreeIterator.class);
            if (treeWalk.getPathString().equals(path))
                return workingTreeIterator.getEntryObjectId();
            if (workingTreeIterator.getEntryFileMode().equals(FileMode.TREE))
                treeWalk.enterSubtree();
        }

        return ObjectId.zeroId(); // not found
    }

    private String getRepoPath(File file) {
        return file.getPath().substring(localRepo.getDirectory().getPath().length() - 4).replace('\\', '/');
    }

    private String getLogicalPath(File file) {
        return file.getPath().replace('\\', '/');
    }

    public String getCommit() throws IOException {
        ObjectId head = localRepo.resolve(Constants.HEAD);
        if (head != null)
            return head.getName();
        else
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

    public String getBranch() throws IOException {
        return localRepo.getBranch();
    }

    /**
     * Does not do anything if already on target branch.
     */
    public void checkout(String branch) throws Exception {
        if (!branch.equals(getBranch())) {
            createBranchIfNeeded(branch);
            git.checkout().setName(branch).setStartPoint("origin/" + branch)
                .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK).call();
            // for some reason jgit needs this when branch is switched
            git.checkout().setName(branch).call();
        }
    }

    /**
     * Actually a workaround since JGit does not support sparse checkout:
     * https://bugs.eclipse.org/bugs/show_bug.cgi?id=383772.
     * Performs a HARD reset and FORCED checkout then deletes non-path items.
     * Only to be used on server (not Designer).
     */
    public void sparseCheckout(String branch, String path) throws Exception {
        fetch(); // in case the branch is not known locally
        hardReset();
        checkout(branch);
        pull(branch);  // pull before delete or next pull may add non-path items back

        // delete non-path items
        List<File> preserveList = new ArrayList<File>();
        preserveList.add(new File(localDir + "/.git"));
        preserveList.add(new File(localDir + "/" + path));
        FileHelper.deleteRecursive(localDir, preserveList);
    }

    /**
     * Performs a HARD reset and FORCED checkout.
     * Only to be used on server (not Designer).
     */
    public void hardCheckout(String branch, String path) throws Exception {
        fetch(); // in case the branch is not known locally
        hardReset();
        checkout(branch);
        pull(branch);  // pull before delete or next pull may add non-path items back
    }

    public void hardReset() throws Exception {
        git.reset().setMode(ResetType.HARD).call();
        git.clean().call();
    }

    /**
     * Create a local branch for remote tracking if it doesn't exist already.
     */
    protected void createBranchIfNeeded(String branch) throws Exception {
        fetch(); // in case the branch is not known locally
        if (localRepo.getRef(branch) == null) {
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

    public void push() throws Exception {
        PushCommand push = git.push();
        if (credentialsProvider != null)
            push.setCredentialsProvider(credentialsProvider);
        push.call();
    }

    public void fetch() throws Exception {
        FetchCommand fetchCommand = git.fetch();
        if (credentialsProvider != null)
            fetchCommand.setCredentialsProvider(credentialsProvider);
        fetchCommand.call();
    }

    public void cloneRepo() throws Exception {
        cloneRepo(null);
    }

    public void cloneRepo(String branch) throws Exception {
        CloneCommand cloneCommand = Git.cloneRepository().setURI(repositoryUrl).setDirectory(localRepo.getDirectory().getParentFile());
        if (branch != null)
            cloneCommand.setBranch(branch);
        if (credentialsProvider != null)
            cloneCommand.setCredentialsProvider(credentialsProvider);
        cloneCommand.call();
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

    public void add(String filePattern) throws GitAPIException {
        git.add().addFilepattern(filePattern).call();
    }

    /**
     * In lieu of sparse checkout since it's not yet supported in JGit:
     * https://bugs.eclipse.org/bugs/show_bug.cgi?id=383772
     */
    public void cloneNoCheckout() throws Exception {
        CloneCommand clone = Git.cloneRepository().setURI(repositoryUrl).setDirectory(localRepo.getDirectory().getParentFile()).setNoCheckout(true);
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=442029
        if (credentialsProvider != null)
            clone.setCredentialsProvider(credentialsProvider);
        clone.call();
    }

    public boolean localRepoExists() {
        return localRepo.getDirectory() != null && localRepo.getDirectory().exists();
    }

    public boolean isTracked(String path) throws IOException {
        ObjectId objectId = localRepo.resolve(Constants.HEAD);
        RevTree tree;
        if (objectId != null)
          tree = new RevWalk(localRepo).parseTree(objectId);
        else
          tree = null;

        TreeWalk treeWalk = new TreeWalk(localRepo);
        treeWalk.setRecursive(true);
        if (tree != null)
          treeWalk.addTree(tree);
        else
          treeWalk.addTree(new EmptyTreeIterator());

        treeWalk.addTree(new DirCacheIterator(localRepo.readDirCache()));
        treeWalk.setFilter(PathFilterGroup.createFromStrings(Collections.singleton(path)));
        return treeWalk.next();
    }

    public void pull(String branch) throws Exception {
        PullCommand pull = git.pull().setRemote("origin").setRemoteBranchName(branch);
        if (credentialsProvider != null)
            pull.setCredentialsProvider(credentialsProvider);
        pull.call();
    }

    public static String byteArrayToHexString(byte[] b) {
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

    /**
     * TODO: More thorough path normalization.
     * see Repository.stripWorkDir()
     */
    public String getRelativePath(File file) {
        String localPath = localDir.getAbsolutePath();
        if (localPath.endsWith("\\.") || localPath.endsWith("/."))
            localPath = localPath.substring(0, localPath.length() - 2);

        return file.getAbsolutePath().substring(localPath.length() + 1).replace('\\', '/');
    }

    public GitDiffs getDiffs(String branch, String path) throws Exception {
        fetch();
        GitDiffs diffs = new GitDiffs();
        ObjectId remoteHead = localRepo.resolve("origin/" + branch + "^{tree}");
        if (remoteHead == null)
            throw new IOException("Unable to determine Git Diffs due to missing remote HEAD");
        CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
        newTreeIter.reset(localRepo.newObjectReader(), remoteHead);
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
            RevCommit revCommit = revCommits.next();
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
        return null;
    }

    public ObjectStream getRemoteContentStream(String branch, String path) throws Exception {
        ObjectId id = localRepo.resolve("refs/remotes/origin/" + branch);
        ObjectReader reader = localRepo.newObjectReader();
        try {
            RevWalk walk = new RevWalk(reader);
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
        finally {
            reader.release();
        }
    }

    public String getRemoteContentString(String branch, String path) throws Exception {
        InputStream in = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            in = getRemoteContentStream(branch, path);
            if (in == null)
                return null;
            int read = 0;
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

    public static VersionControlGit getFrameworkGit() throws PropertyException, IOException {
        String gitRoot = PropertyManager.getProperty(PropertyNames.MDW_GIT_LOCAL_PATH);
        if (gitRoot == null)
            throw new PropertyException("Missing required property: " + PropertyNames.MDW_GIT_LOCAL_PATH);
        String gitRemoteUrl = PropertyManager.getProperty(PropertyNames.MDW_GIT_REMOTE_URL);
        if (gitRemoteUrl == null)
            throw new PropertyException("Missing required property: " + PropertyNames.MDW_GIT_REMOTE_URL);
        String gitBranch = PropertyManager.getProperty(PropertyNames.MDW_GIT_BRANCH);
        if (gitBranch == null)
            throw new PropertyException("Missing required property: " + PropertyNames.MDW_GIT_BRANCH);
        String user = PropertyManager.getProperty(PropertyNames.MDW_GIT_USER);
        String password = PropertyManager.getProperty(PropertyNames.MDW_GIT_PASSWORD);
        VersionControlGit vcGit = new VersionControlGit();
        vcGit.connect(gitRemoteUrl, user, password, new File(gitRoot));
        return vcGit;
    }
}
