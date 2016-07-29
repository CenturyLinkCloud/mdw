/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.workflow.provider;

import com.centurylink.mdw.activity.types.GeneralActivity;
import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.provider.PackageAwareProvider;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.process.PackageVO;

/**
 * Activity implementor provider that operates based on a workflow package tag.
 * Requires the Spring property packageTagStartsWith.
 */
public class PackageAwareActivityProvider extends ActivityProviderBean implements PackageAwareProvider<GeneralActivity> {

    private static StandardLogger logger = LoggerUtil.getStandardLogger();

    private String packageTagStartsWith;
    public String getPackageTagStartsWith() { return packageTagStartsWith; }
    public void setPackageTagStartsWith(String startsWith) { this.packageTagStartsWith = startsWith; }

    /**
     * Checks whether workflow package tag starts with the correct value.
     */
    @Override
    public GeneralActivity getInstance(PackageVO workflowPackage, String type)
    throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        String tags = workflowPackage.getAttribute(WorkAttributeConstant.VERSION_TAG);
        if (tags != null) {
            for (String tag : tags.split(",")) {
                if (tag.startsWith(packageTagStartsWith)) {
                    GeneralActivity activity = super.getInstance(type);
                    if (activity != null && logger.isMdwDebugEnabled())
                        logger.mdwDebug("Activity " + type + " provided by " + getClass().getClassLoader() + " due to matching workflow package " + workflowPackage.getLabel());
                    return activity;
                }
            }
        }
        if (logger.isMdwDebugEnabled())
            logger.mdwDebug("Version tag not matched: packageTagStartsWith=" + packageTagStartsWith + ", tags=" + tags);

        return null;
    }
}
