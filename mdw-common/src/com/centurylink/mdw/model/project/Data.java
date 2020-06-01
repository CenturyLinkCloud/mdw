package com.centurylink.mdw.model.project;

import com.centurylink.mdw.activity.types.GeneralActivity;
import com.centurylink.mdw.model.workflow.ActivityImplementor;
import org.json.JSONArray;

import java.io.IOException;
import java.util.*;

/**
 * Project data overridden/supplemented by custom values from project.yaml.
 */
public class Data {

    public static final String GIT_BASE_URL = "https://github.com/CenturyLinkCloud";
    public static final String GIT_REPO_URL = GIT_BASE_URL + "/mdw.git";
    public static final String DOCS_URL = "https://centurylinkcloud.github.io/mdw/docs";
    public static final String BASE_PKG = "com.centurylink.mdw.base";

    private Project project;
    public Data(Project project) {
        this.project = project;
    }

    public List<String> getWorkgroups() throws IOException {
        List<String> workgroups = project.readDataList("data.workgroups");
        if (workgroups == null)
            workgroups = DEFAULT_WORKGROUPS;
        return workgroups;
    }
    /**
     * excludes Site Admin on purpose
     */
    public static final List<String> DEFAULT_WORKGROUPS;
    static {
        DEFAULT_WORKGROUPS = Arrays.asList(
                "MDW Support",
                "Developers"
        );
    }

    public Map<String,String> getTaskCategories() throws IOException {
        Map<String,String> taskCategories = project.readDataMap("data.task.categories");
        if (taskCategories == null)
            taskCategories = DEFAULT_TASK_CATEGORIES;
        return taskCategories;
    }
    public static final Map<String,String> DEFAULT_TASK_CATEGORIES;
    static {
        DEFAULT_TASK_CATEGORIES = new HashMap<String, String>() {{
            put("Ordering", "ORD");
            put("General Inquiry", "GEN");
            put("Billing", "BIL");
            put("Complaint", "COM");
            put("Portal Support", "POR");
            put("Training", "TRN");
            put("Repair", "RPR");
            put("Inventory", "INV");
            put("Test", "TST");
            put("Vacation Planning", "VAC");
            put("Customer Contact", "CNT");
        }};
    }

    public static final List<String> TASK_OUTCOMES;
    static {
        TASK_OUTCOMES = Arrays.asList(
                "Open",
                "Assigned",
                "Completed",
                "Cancelled",
                "In Progress",
                "Alert",
                "Jeopardy",
                "Forward"
        );
    }

    public static final String DEFAULT_TASK_NOTIFIER = "com.centurylink.mdw.workflow.task.notifier.TaskEmailNotifier";
    public static final JSONArray DEFAULT_TASK_NOTICES;
    static {
        DEFAULT_TASK_NOTICES = new JSONArray();
        for (String taskOutcome : TASK_OUTCOMES) {
            JSONArray notices = new JSONArray();
            notices.put(taskOutcome);
            notices.put(""); // template
            notices.put(""); // version
            notices.put(DEFAULT_TASK_NOTIFIER); // notifier
            DEFAULT_TASK_NOTICES.put(notices);
        }
    }

    public List<String> getBinaryAssetExts() throws IOException {
        List<String> binaryAssetExts = project.readDataList("data.binary.asset.exts");
        if (binaryAssetExts == null)
            binaryAssetExts = DEFAULT_BINARY_ASSET_EXTS;
        return binaryAssetExts;
    }
    public static final List<String> DEFAULT_BINARY_ASSET_EXTS;
    static {
        DEFAULT_BINARY_ASSET_EXTS = Arrays.asList(
                "png",
                "jpg",
                "gif",
                "svg",
                "xlsx",
                "docx",
                "class",
                "jar",
                "zip",
                "eot",
                "ttf",
                "woff",
                "woff2"
        );
    }

    public static class Implementors {
        public static final String START_IMPL = "com.centurylink.mdw.workflow.activity.process.ProcessStartActivity";
        public static final String STOP_IMPL = "com.centurylink.mdw.workflow.activity.process.ProcessFinishActivity";
        public static final String PAUSE_IMPL = "com.centurylink.mdw.base.PauseActivity";
        public static final String DUMMY_ACTIVITY = "com.centurylink.mdw.workflow.activity.DefaultActivityImpl";

        public static final List<ActivityImplementor> PSEUDO_IMPLEMENTORS = new ArrayList<>();
        static {
            PSEUDO_IMPLEMENTORS.add(new ActivityImplementor("Exception Handler", "subflow", "" +
                    "Exception Handler Subflow", BASE_PKG + "/subflow.png", null));
            PSEUDO_IMPLEMENTORS.add(new ActivityImplementor("Cancellation Handler", "subflow",
                    "Cancellation Handler Subflow", BASE_PKG + "/subflow.png", null));
            PSEUDO_IMPLEMENTORS.add(new ActivityImplementor("Delay Handler", "subflow",
                    "Delay Handler Subflow", BASE_PKG + "/subflow.png", null));
            PSEUDO_IMPLEMENTORS.add(new ActivityImplementor("TextNote", "note",
                    "Text Note", BASE_PKG + "/note.png", null));
            PSEUDO_IMPLEMENTORS.add(new ActivityImplementor("com.centurylink.mdw.workflow.activity.DefaultActivityImpl",
                    GeneralActivity.class.getName(), "Dummy Activity", "shape:activity", "{}"));
        }
    }

    public List<String> getDbTables() throws IOException {
        List<String> dbTables = project.readDataList("data.db.tables");
        if (dbTables == null)
            dbTables = DEFAULT_DB_TABLES;
        return dbTables;
    }
    public static final List<String> DEFAULT_DB_TABLES;
    static {
        DEFAULT_DB_TABLES = Arrays.asList(
                "USER_INFO",
                "USER_GROUP",
                "USER_GROUP_MAPPING",
                "USER_ROLE",
                "USER_ROLE_MAPPING",
                "PROCESS_INSTANCE",
                "ACTIVITY_INSTANCE",
                "WORK_TRANSITION_INSTANCE",
                "VARIABLE_INSTANCE",
                "DOCUMENT",
                "DOCUMENT_CONTENT",
                "TASK_INSTANCE",
                "TASK_INST_GRP_MAPP",
                "EVENT_INSTANCE",
                "EVENT_WAIT_INSTANCE",
                "ATTRIBUTE",
                "VALUE",
                "INSTANCE_INDEX",
                "INSTANCE_NOTE",
                "ATTACHMENT",
                "SOLUTION",
                "SOLUTION_MAP"
        );
    }

    /**
     * Tables excluded from export which need to be purged on import.
     */
    public List<String> getExcludedTables() throws IOException {
        List<String> dbTables = project.readDataList("data.db.excluded");
        if (dbTables == null)
            dbTables = DEFAULT_EXCLUDED_TABLES;
        return dbTables;
    }

    public static final List<String> DEFAULT_EXCLUDED_TABLES;
    static {
        DEFAULT_EXCLUDED_TABLES = Arrays.asList(
                "EVENT_LOG",
                "INSTANCE_TIMING"
        );
    }
}
