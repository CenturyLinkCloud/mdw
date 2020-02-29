package com.centurylink.mdw.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.centurylink.mdw.dataaccess.AssetRef;
import com.centurylink.mdw.model.asset.CommitInfo;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@Parameters(commandNames="find", commandDescription="Find an asset ref for a path or definition id", separators="=")
public class Find extends Setup {

    @Parameter(names="--path", description="Asset path (with version), eg: \"com.centurylink.mdw.tests.workflow/StartStopProcess.proc v0.9\"")
    private String path;
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    @Parameter(names="--id", description="Asset ID")
    private Long id;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @Override
    public List<Dependency> getDependencies() throws IOException {
        List dependencies = new DbInfo(new Props(this)).getDependencies();
        dependencies.addAll(Git.getDependencies());
        return dependencies;
    }

    @Override
    public Operation run(ProgressMonitor... progressMonitors) throws IOException {

        Props props = new Props(this);
        VcInfo vcInfo = new VcInfo(getGitRoot(), props);
        DbInfo dbInfo = new DbInfo(props);

        Checkpoint checkpoint = new Checkpoint(vcInfo, getAssetRoot(), dbInfo);

        try {
            AssetRef assetRef;
            if (path != null) {
                assetRef = checkpoint.retrieveRef(path);
            }
            else if (id != null) {
                assetRef = checkpoint.retrieveRef(id);
            }
            else {
                throw new IOException("--path or --id is required");
            }

            if (assetRef == null) {
                getErr().println("Asset ref not found");
            }
            else {
                getOut().println("Asset ref: " + assetRef);
                Git git = new Git(vcInfo, "getCommitInfoForRef", assetRef.getRef());
                git.run(progressMonitors); // connect
                CommitInfo commitInfo = (CommitInfo) git.getResult();
                if (commitInfo == null) {
                    getErr().println("Commit info not found: " + assetRef.getRef());
                }
                else {
                    getOut().println("Commit: " + commitInfo.getCommit() + " '" + commitInfo.getMessage() + "'");
                }
            }
        }
        catch (SQLException ex) {
            throw new IOException(ex);
        }

        return this;
    }
}
