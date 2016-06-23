/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.constant;

/**
 * This class hold all the constants for work attributes
 */
public class WorkAttributeConstant  {

     public static final String PROCESS_VISIBILITY = "PROCESS_VISIBILITY";
     public static final String EMBEDDED_PROCESS_TYPE = "EMBEDDED_PROCESS_TYPE";
     public static final String PROCESS_START_PAGE = "PROCESS_START_PAGE";
     public static final String PROCESS_START_PAGE_ASSET_VERSION = "PROCESS_START_PAGE_assetVersion";

     /**
      * constant that represents the timer wait
      */
     public static final String TIMER_WAIT = "TIMER_WAIT";

     /**
      * constant that represents the wait event names/etc.
      */
     public static final String WAIT_EVENT_NAMES = "WAIT_EVENT_NAMES";

     /**
      * Constant for node geometry information attribute
      */
     public static final String WORK_DISPLAY_INFO = "WORK_DISPLAY_INFO";
     public static final String SWIMLANE_GEO_INFO = "SWIMLANE_GEO_INFO";

     public static final String SYNCED_ACTIVITIES = "Synced Activities";
     public static final String SYNC_EXPRESSION = "Sync Expression";

     public static final String NOTICE_TYPE = "noticeType";
     public static final String NOTICE_TYPE_EMAIL = "E-Mail";
     public static final String NOTICE_HOST = "host";
     public static final String NOTICE_FROM = "from";
     public static final String NOTICE_SUBJECT = "subject";
     public static final String NOTICE_TEMPLATE = "template";
     public static final String NOTICE_ATTACHMENTS = "attachments";
     public static final String NOTICE_GROUPS = "NoticeGroups";
     public static final String NOTICE_RECIP_EMAILS = "RecipientVar"; // leave attr name for backward compatibility
     public static final String CC_GROUPS = "CCGroups";
     public static final String CC_EMAILS = "CCVar";  // leave attr name for backward compatibility
     public static final String CONTINUE_DESPITE_MESSAGING_EXCEPTION = "ContinueDespiteMessagingException";

     public static final String VARIABLE_NAME = "VARIABLE_NAME";
     public static final String ATTRIBUTE_NAME = "ATTRIBUTE_NAME";
     public static final String CONTAINED_STRING = "CONTAINED_STRING";

     public static final String PROCESS_NAME = "processname";
     public static final String PROCESS_VERSION = "processversion";
     public static final String ASSET_VERSION = "assetVersion";
     public static final String ALIAS_PROCESS_ID = "AliasProcessId";
     public static final String PROCESS_MAP = "processmap";

     // the following attributes are used by designer to remember hidden links
     public static final String START_TRANSITION_ID = "START_TRANSITION_ID";
     public static final String ENTRY_TRANSITION_ID = "ENTRY_TRANSITION_ID";

     // the following attribute is only for embedded processes
     public static final String ENTRY_CODE = "ENTRY_CODE";

     /**
      * attribute name for class name of activity implementor
      */
     public static final String CLASS_NAME = "classname";

     public static final String TRANSITION_WITH_NO_LABEL = "Transition with no Label";

     public static final String FORM = "FORM";

     public static final String SLA = "SLA";
     public static final String SLA_UNIT = "SLA_UNIT";	// this is for old $+SLA, seems
     public static final String SLA_UNITS = "SLAUnits";	// this is new default name, seems

     public static final String STATUS_AFTER_TIMEOUT = "STATUS_AFTER_TIMEOUT";
     public static final String STATUS_AFTER_EVENT = "STATUS_AFTER_EVENT";

     /**
      * Logical ID of activity, transition, subprocess, and text notes
      */
     public static final String LOGICAL_ID = "LOGICAL_ID";
     public static final String REFERENCE_ID = "REFERENCE_ID";

     /**
      * Documentation and reference document links for activities
      */
     public static final String DESCRIPTION = "DESCRIPTION";
     public static final String DOCUMENTATION = "Documentation";
     public static final String REFERENCES = "References";

     /**
      * performance level of processes
      */
     public static final String PERFORMANCE_LEVEL = "PerformanceLevel";

     /**
      * variable name mapping
      */
     public static final String VARIABLE_MAPPING = "Variable Mapping";

	 //Attributes for setting pre-script and post-script in adapter activities
     public static final String PRE_SCRIPT = "PreScript";
     public static final String PRE_SCRIPT_LANGUAGE = "PreScriptLang";
     public static final String POST_SCRIPT = "PostScript";
     public static final String POST_SCRIPT_LANGUAGE = "PostScriptLang";

     public static final String OSGI_BSN = "OsgiBundleSymbolicName";
     public static final String OSGI_BUNDLE_VERSION = "OsgiBundleVersion";

     public static final String VERSION_TAG = "VersionTag";

     public static final String RENDERING_ENGINE = "Rendering"; // for custom pages

     public static final String HTML5_RENDERING = "HTML5";
     public static final String COMPATIBILITY_RENDERING = "Compatibility";

     // OSGi Service Adapter
     public static final String SERVICE_INTERFACE = "ServiceInterface" ;
     public static final String SERVICE_METHOD = "ServiceMethod";
     public static final String SERVICE_PARAMETERS = "ServiceParameters";
     public static final String SERVICE_RESULT = "ServiceResult";


     // Override Attributes
     public static final String SIMULATION_ATTR_PREFIX = "SIMUL";
     public static final String SIMULATION_STUB_MODE = SIMULATION_ATTR_PREFIX + "@STUB_MODE";
     public static final String SIMULATION_RESPONSE = SIMULATION_ATTR_PREFIX + "@RESPONSE";
     public static final String MILESTONE_NAME = "MILESTONE@NAME";

     public static final String BAM_ATTR_PREFIX = "BAM";
     public static final String BAM_START_MSGDEF = BAM_ATTR_PREFIX + "@START_MSGDEF";
     public static final String BAM_FINISH_MSGDEF = BAM_ATTR_PREFIX + "@FINISH_MSGDEF";

     public static boolean isOverrideAttribute(String attrName) {
         return attrName != null && attrName.indexOf('@') >= 0;
     }

     /**
      * Attribute override for processes stored in ruleset
      */
     public static final String OVERRIDE_QUALIFIER = "OVERRIDE_";
     public static final String OVERRIDE_ACTIVITY = OVERRIDE_QUALIFIER;
     public static final String OVERRIDE_TRANSITION = OVERRIDE_QUALIFIER + "TRANSITION_";
     public static final String OVERRIDE_SUBPROC = OVERRIDE_QUALIFIER + "SUBPROC_";

     /**
     * Returns raw name if subtype is null.
     */
    public static String getOverrideAttributeName(String rawName, String subType, String subId) {
         if (OwnerType.ACTIVITY.equals(subType))
             return OVERRIDE_ACTIVITY + subId + ":" + rawName;
         else if (OwnerType.WORK_TRANSITION.equals(subType))
             return OVERRIDE_TRANSITION + subId + ":" + rawName;
         else if (OwnerType.PROCESS.equals(subType))
             return OVERRIDE_SUBPROC + subId + ":" + rawName;
         else
             return rawName;
     }

     /**
     * Returns top-level (process-level) match if subType is null.
     */
    public static boolean isFullAttrNameFor(String fullName, String subType, String subId, String prefix) {
         if (OwnerType.ACTIVITY.equals(subType))
             return fullName.startsWith(OVERRIDE_ACTIVITY + subId + ":" + prefix + "@");
         else if (OwnerType.WORK_TRANSITION.equals(subType))
             return fullName.startsWith(OVERRIDE_TRANSITION + subId + ":" + prefix + "@");
         else if (OwnerType.PROCESS.equals(subType))
             return fullName.startsWith(OVERRIDE_SUBPROC + subId + ":" + prefix + "@");
         else
             return fullName.startsWith(prefix + "@"); // top level
    }

    public static boolean isAttrNameFor(String attrName, String prefix) {
        return attrName.startsWith(prefix + "@") || attrName.indexOf(":" + prefix + "@") > 0;
    }

    public static String getOverrideAttributePrefix(String fullName) {
        int at = fullName.indexOf('@');
        if (at > 0)
            return fullName.substring(0, at);
        else
            return null;
    }
}
