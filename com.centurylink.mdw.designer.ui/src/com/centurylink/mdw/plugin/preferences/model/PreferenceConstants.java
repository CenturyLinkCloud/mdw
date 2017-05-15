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
package com.centurylink.mdw.plugin.preferences.model;

import com.centurylink.mdw.plugin.PluginMessages;

/**
 * Constant definitions for plug-in preferences
 */
public interface PreferenceConstants {
    // TODO: externalize these URLs
    public static final String PREFS_DEFAULT_ASSET_DISCOVERY_URL = "https://mdw.useast.appfog.ctl.io/mdw";
    public static final String PREFS_DEFAULT_PROJECT_DISCOVERY_URL = "http://lxdenvmtc143.dev.qintra.com:7021/Discovery";
    public static final String PREFS_DEFAULT_MDW_RELEASES_URL = "http://repo.maven.apache.org/maven2";
    public static final String PREFS_DEFAULT_WORKSPACE_SETUP_URL = "http://lxdenvmtc143.dev.qintra.com:7021/Environment";

    public static final String PREFS_DEFAULT_COPYRIGHT_NOTICE = "/**\n * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.\n */";

    public static final String PREFS_MDW_RELEASES_URL = "MdwPrefsReleasesUrl";
    public static final String PREFS_WORKSPACE_SETUP_URL = "MdwPrefsWorkspaceSetupUrl";
    public static final String PREFS_ASSET_DISCOVERY_URL = "MdwAssetDiscoveryUrl";
    public static final String PREFS_PROJECT_DISCOVERY_URL = "MdwProjectDiscoveryUrl";

    public static final String PREFS_JDBC_FETCH_SIZE = "JdbcFetchSize";
    public static final String PREFS_DEFAULT_JDBC_FETCH_SIZE = "200";
    public static final String PREFS_COPYRIGHT_NOTICE = "MdwPrefsCopyrightNotice";

    public static final String PREFS_CURRENT_CODE_TEMPLATES = "MdwCurrentCodeTemplates";
    public static final String PREFS_CURRENT_CODE_FORMATTER = "MdwCurrentCodeFormatter";

    public static final String PREFS_ALLOW_ASSETS_WITHOUT_EXTENSIONS = "MdwAllowAssetNamesWithoutExtensions";
    public static final String PREFS_COMPARE_CONFLICTING_ASSETS = "MdwCompareConflictingAssets";
    public static final String PREFS_ALLOW_DELETE_ARCHIVED_PROCESSES = "AllowDeleteArchivedProcesses";
    public static final String PREFS_DOUBLE_CLICK_OPENS_SUBPROCESSES_AND_SCRIPTS = "DoubleClickOpensSubprocessesAndScripts";
    public static final String PREFS_INFER_SMART_SUBPROC_VERSION_SPEC = "InferSmartSubprocVersionSpec";
    public static final String PREFS_EMBEDDED_EDITOR_FOR_EXCEL = "UseEmbeddedEditorForExcel";
    public static final String PREFS_READONLY_BG_RED = "MdwReadOnlyBackgroundRed";
    public static final String PREFS_READONLY_BG_GREEN = "MdwReadOnlyBackgroundGreen";
    public static final String PREFS_READONLY_BG_BLUE = "MdwReadOnlyBackgroundBlue";
    public static final String PREFS_WEB_BASED_PROCESS_LAUNCH = "MdwWebBasedProcessLaunch";
    public static final String PREFS_WARN_OVERRIDE_ATTRS_NOT_CARRIED_FORWARD = "MdwWarnOverrideAttrsNotCarriedForward";
    public static final String PREFS_VALIDATE_PROCESS_VERSIONS = "MdwValidateProcessVersions";

    // TODO: error reporting via REST service instead of SMTP
    public static final String PREFS_SMTP_HOST = "MdwSmtpHost";
    public static final String PREFS_DEFAULT_SMTP_HOST = "mailgate.uswc.uswest.com";
    public static final String PREFS_SMTP_PORT = "MdwSmtpPort";
    public static final Integer PREFS_DEFAULT_SMTP_PORT = 25;
    public static final String PREFS_MDW_REPORTING_LEVEL = "MdwReportingLevel";
    public static final int PREFS_DEFAULT_MDW_REPORTING_LEVEL = PluginMessages.WARNING_MESSAGE;
    public static final String PREFS_LOG_TIMINGS = "MdwLogTimings";
    public static final String PREFS_LOG_CONNECT_ERRORS = "MdwLogConnectErrors";
    public static final String PREFS_INCLUDE_PREVIEW_BUILDS = "MdwIncludePreviewBuilds";
    public static final String PREFS_SWING_LAUNCH_EVENT_MANAGER = "MdwSwingLaunchEventManager";
    public static final String PREFS_SWING_LAUNCH_THREAD_POOL_MANAGER = "MdwSwingLaunchThreadPoolManager";
    public static final String PREFS_USE_DISCOVERED_VCS_CREDS = "MdwUseDiscoveredVcsCredentials";

    public static final String PREFS_TEMPLATE_INPUT_FILE = "MdwTemplateInputFile";
    public static final String PREFS_TEMPLATE_INPUT_LOCATION = "MdwTemplateInputLocation";
    public static final String PREFS_TEMPLATE_OUTPUT_LOCATION = "MdwTemplateOutputLocation";
    public static final String PREFS_VELOCITY_PROPERTY_FILE_LOCATION = "MdwVelocityPropertyFileLocation";
    public static final String PREFS_VELOCITY_TOOLBOX_FILE_LOCATION = "MdwVelocityToolboxFileLocation";

    public static final String PREFS_PROCESS_SAVE_INCREMENT = "MdwProcessSaveIncrement";
    public static final String PREFS_ASSET_SAVE_INCREMENT = "MdwAssetSaveIncrement";
    public static final String PREFS_CARRY_FORWARD_OVERRIDE_ATTRS = "MdwCarryForwardOverrideAttrs";
    public static final String PREFS_KEEP_PROCESSES_LOCKED_WHEN_SAVING = "MdwKeepProcessesLockedWhenSaving";
    public static final String PREFS_KEEP_RESOURCES_LOCKED_WHEN_SAVING = "MdwKeepResourcesLockedWhenSaving";
    public static final String PREFS_ENFORCE_PROCESS_VALIDATION_RULES = "MdwEnforceProcessValidationRules";
    public static final String PREFS_ENFORCE_ASSET_VALIDATION_RULES = "MdwEnforceAssetValidationRules";
    public static final String PREFS_DESIGNER_CANVAS_NODE_ID_TYPE = "MdwDesignerCanvasNodeIdType";
    public static final String PREFS_DESIGNER_SUPPRESS_TOOLTIPS = "MdwDesignerSuppressTooltips";

    public static final String PREFS_SWITCH_TO_DESIGNER_PERSPECTIVE = "MdwSwitchToDesignerPerspective";
    public static final String PREFS_DELETE_TEMP_FILES_AFTER_SERVER_CONFIG = "MdwDeleteTempFilesAfterServerConfig";
    public static final String PREFS_SORT_PACKAGE_CONTENTS_A_TO_Z = "MdwSortPackageContentsAtoZ";
    // process explorer: FilterXxx shows by default, ShowXxx does not
    public static final String PREFS_FILTER_PROCESSES_IN_PEX = "FilterProcessesInPEX";
    public static final String PREFS_FILTER_WORKFLOW_ASSETS_IN_PEX = "FilterWorkflowAssetInPEX";
    public static final String PREFS_FILTER_EVENT_HANDLERS_IN_PEX = "FilterEventHandlersInPEX";
    public static final String PREFS_SHOW_ACTIVITY_IMPLEMENTORS_IN_PEX = "ShowImplementorsInPEX";
    public static final String PREFS_FILTER_TASK_TEMPLATES_IN_PEX = "FilterTaskTemplatesInPEX";
    public static final String PREFS_FILTER_ARCHIVED_ITEMS_IN_PEX = "FilterArchivedItemsInPEX";

    public static final String PREFS_SORT_TOOLBOX_A_TO_Z = "MdwSortToolboxAtoZ";

    public static final String PREFS_TEMP_RESOURCE_DIRECTORY = "MdwTempResourceDirectory";
    public static final String PREFS_DEFAULT_TEMP_RESOURCE_DIRECTORY = ".temp";
    public static final String PREFS_PREVIOUS_TEMP_FILE_VERSIONS_TO_KEEP = "MdwPreviousTempFileVersionsToKeep";
    public static final int PREFS_DEFAULT_PREV_TEMP_FILE_VERSIONS = 5;
    public static final String PREFS_LOAD_SCRIPT_LIBS_ON_EDIT = "MdwLoadScriptLibsOnEdit";

    public static final String PREFS_PROCESS_EXPORT_FORMAT = "MdwProcessExportFormat";
    public static final String PREFS_PROCESS_EXPORT_DOCUMENTATION = "MdwProcessExportDocumentation";
    public static final String PREFS_PROCESS_EXPORT_ATTRIBUTES = "MdwProcessExportAttributes";
    public static final String PREFS_PROCESS_EXPORT_VARIABLES = "MdwProcessExportVariables";
    public static final String PREFS_PROCESS_EXPORT_ELEMENT_ORDER = "MdwProcessExportElementOrder";

    // server console pref constants
    public static final String PREFS_SERVER_CONSOLE_BUFFER_SIZE = "MdwServerConsoleBufferSize";
    public static final int PREFS_DEFAULT_SERVER_CONSOLE_BUFFER_SIZE = 80 * 5000; // ~5000
                                                                                  // lines
    public static final String PREFS_SERVER_CONSOLE_FONT = "MdwServerConsoleFont";
    public static final String PREFS_SERVER_CONSOLE_FONT_RED = "MdwServerConsoleFontRed";
    public static final String PREFS_SERVER_CONSOLE_FONT_GREEN = "MdwServerConsoleFontGreen";
    public static final String PREFS_SERVER_CONSOLE_FONT_BLUE = "MdwServerConsoleFontBlue";
    public static final String PREFS_SERVER_CONSOLE_BG_RED = "MdwServerConsoleBackgroundRed";
    public static final String PREFS_SERVER_CONSOLE_BG_GREEN = "MdwServerConsoleBackgroundGreen";
    public static final String PREFS_SERVER_CONSOLE_BG_BLUE = "MdwServerConsoleBackgroundBlue";
    public static final String PREFS_SERVER_CLIENT_SHELL = "MdwServerClientShell";
    public static final String PREFS_SERVER_CLIENT_SHELL_EXE_PATH = "MdwServerClientShellExePath";

    public static final String PREFS_SERVER_RUNNING = "MdwServerRunning";
    public static final String PREFS_SERVER_WF_PROJECT = "MdwServerWorkflowProject";
    public static final String PREFS_RUNNING_SERVER = "MdwRunningServer";

    public static final String PREFS_EXPORT_JSON_FORMAT = "MdwExportJsonFormat";
    // stored as _SUPPRESS_ so that default is true
    public static final String PREFS_SUPPRESS_TASK_TEMPLATES_IN_PKG_EXPORT = "MdwSuppressTaskTemplatesInPackageExport";
    public static final String PREFS_SUPPRESS_INFER_REFERENCED_IMPLS_DURING_EXPORT = "MdwSuppressInferReferencedImplsDuringExport";

    // credentials stored in secure store
    public static final String PREFS_MDW_USER = "com.centurylink.mdw.user";
    public static final String PREFS_MDW_PASSWORD = "com.centurylink.mdw.password";

    // http timeouts
    public static final String PREFS_HTTP_CONNECT_TIMEOUT_MS = "com.centurylink.mdw.http.connect.timeout";
    public static final String PREFS_HTTP_READ_TIMEOUT_MS = "com.centurylink.mdw.http.read.timeout";

}
