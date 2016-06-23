/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.activity;

import java.io.Serializable;
import java.util.List;

import com.centurylink.mdw.activity.types.AdapterActivity;
import com.centurylink.mdw.activity.types.EvaluatorActivity;
import com.centurylink.mdw.activity.types.EventWaitActivity;
import com.centurylink.mdw.activity.types.FinishActivity;
import com.centurylink.mdw.activity.types.GeneralActivity;
import com.centurylink.mdw.activity.types.InvokeProcessActivity;
import com.centurylink.mdw.activity.types.RuleActivity;
import com.centurylink.mdw.activity.types.ScriptActivity;
import com.centurylink.mdw.activity.types.StartActivity;
import com.centurylink.mdw.activity.types.SynchronizationActivity;
import com.centurylink.mdw.activity.types.TaskActivity;
import com.centurylink.mdw.model.value.variable.VariableVO;


public class ActivityImplementorVO implements Serializable, Comparable<ActivityImplementorVO> {

    public static Class<?>[] baseClasses = {
        GeneralActivity.class,
        AdapterActivity.class,
        EventWaitActivity.class,
        StartActivity.class,
        FinishActivity.class,
        InvokeProcessActivity.class,
        TaskActivity.class,
        SynchronizationActivity.class,
        ScriptActivity.class,
        RuleActivity.class,
        EvaluatorActivity.class
    };

    public static final String oldBaseClassPackage = "com.qwest.mdw.workflow.activity.types";
    public static String[] oldBaseClasses = {
        oldBaseClassPackage + ".ControlledActivity",
        oldBaseClassPackage + ".AdapterActivity",
        oldBaseClassPackage + ".EventWaitActivity",
        oldBaseClassPackage + ".StartActivity",
        oldBaseClassPackage + ".FinishActivity",
        oldBaseClassPackage + ".InvokeProcessActivity",
        oldBaseClassPackage + ".TaskActivity",
        oldBaseClassPackage + ".SynchronizationActivity",
        oldBaseClassPackage + ".ScriptActivity",
        oldBaseClassPackage + ".RuleActivity"
    };

	@Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ActivityImplementorVO)) return false;
        return implementorId.equals(((ActivityImplementorVO)obj).implementorId);
    }

    private Long implementorId;
    private String implementorClassName;
    private Integer implementorType;
    private String attributeDescription;
    private boolean hidden;
    private boolean showInToolbox;
    private String baseClassName;
    private String iconName;
    private String label;
    private String mdwVersion;
    private List<VariableVO> variables;

    public ActivityImplementorVO() {
    }

    public ActivityImplementorVO(Long pId, String pClassName) {
        this.implementorId = pId;
        this.implementorClassName = pClassName;
        this.implementorType = new Integer(1);
    }

    public ActivityImplementorVO(String label, String implClassName, String iconName, String attrdesc) {
        this.implementorId = null;
        this.implementorClassName = implClassName;
        this.implementorType = new Integer(1);
        this.iconName = iconName;
        this.attributeDescription = attrdesc;
        this.label = label;
        this.baseClassName = implClassName;
    }

	public Long getImplementorId() {
		return implementorId;
	}

	public void setImplementorId(Long id) {
		this.implementorId = id;
	}

	public String getImplementorClassName() {
		return implementorClassName;
	}

	public void setImplementorClassName(String name) {
		this.implementorClassName = name;
	}

	public String getSimpleName() {
	    return getImplementorClassNameWithoutPath();
	}

	public String getImplementorClassNameWithoutPath() {
		int k = implementorClassName.lastIndexOf('.');
		if (k>0) return implementorClassName.substring(k+1);
		else return implementorClassName;
	}

	public Integer getImplementorType() {
		return implementorType;
	}

	public void setImplementorType(Integer type) {
		this.implementorType = type;
	}

	public List<VariableVO> getVariables() {
		return variables;
	}

	public void setVariables(List<VariableVO> vars) {
		this.variables = vars;
	}

    public int compareTo(ActivityImplementorVO other) {
        if (this.getLabel() == null)
          return -1;
        return this.getLabel().compareTo(other.getLabel());
    }

    public String getAttributeDescription() {
        return attributeDescription;
    }

    public void setAttributeDescription(String attributeDescription) {
        this.attributeDescription = attributeDescription;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    /**
     * Transient flag for filtering.
     */
    public boolean isShowInToolbox() {
        return showInToolbox;
    }

    public void setShowInToolbox(boolean inToolbox) {
        this.showInToolbox = inToolbox;
    }

    public String getBaseClassName() {
        return baseClassName;
    }

    public void setBaseClassName(String baseClassName) {
        this.baseClassName = baseClassName;
    }

    public String getIconName() {
        return iconName;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getMdwVersion() {
        return mdwVersion;
    }

    public void setMdwVersion(String mdwVersion) {
        this.mdwVersion = mdwVersion;
    }

    public boolean isLoaded() {
    	return this.attributeDescription!=null && this.baseClassName!=null
    			&& this.label!=null && this.iconName!=null;
    }


    /**
     * These methods work for 5.2 and 5.5 base classes.
     */
    public boolean isStart() {
      return getBaseClassName() != null
        && getBaseClassName().endsWith("StartActivity");
    }
    public boolean isFinish() {
        return getBaseClassName() != null
          && getBaseClassName().endsWith("FinishActivity");
    }
    public boolean isManualTask() {
        return getBaseClassName() != null
          && getBaseClassName().endsWith("TaskActivity");
    }
    public boolean isSubProcessInvoke() {
        return getBaseClassName() != null
          && getBaseClassName().endsWith("InvokeProcessActivity");
    }
    public boolean isHeterogeneousSubProcInvoke() {
        return isSubProcessInvoke()
          && getImplementorClassName() != null && getImplementorClassName().contains("Heterogeneous");
    }
    public boolean isSync() {
        return getBaseClassName() != null
          && getBaseClassName().endsWith("SynchronizationActivity");
    }
    public boolean isAdapter() {
        return getBaseClassName() != null
          && getBaseClassName().endsWith("AdapterActivity");
    }
    public boolean isLdapAdapter() {
        return isAdapter()
          && getImplementorClassName() != null && getImplementorClassName().endsWith("LdapAdapter");
    }
    public boolean isEventWait() {
        return getBaseClassName() != null
          && getBaseClassName().endsWith("EventWaitActivity");
    }
    public boolean isScript() {
        return getBaseClassName() != null
          && getBaseClassName().endsWith("ScriptActivity");
    }
    public boolean isScriptExecutor() {
        return isScript()
          && getImplementorClassName() != null && getImplementorClassName().endsWith("ScriptExecuterActivity");
    }
    public boolean isExpressionEval() {
        return getImplementorClassName() != null && getImplementorClassName().endsWith("ScriptEvaluator");
    }
    public boolean isRule() {
        return getBaseClassName() != null
          && getBaseClassName().endsWith("RuleActivity");
    }
    public boolean isTransformActivity() {
        return isScript()
          && getImplementorClassName() != null && getImplementorClassName().endsWith("TransformActivity");
    }
    public boolean isNotification() {
        return getImplementorClassName() != null && getImplementorClassName().endsWith("NotificationActivity");
    }

    /**
     * Currently this is only used for File-based and VCS-based persistence (esp. createActivityImplementor()).
     */
    private String packageName;
    public String getPackageName() {
        return packageName;
    }
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

}
