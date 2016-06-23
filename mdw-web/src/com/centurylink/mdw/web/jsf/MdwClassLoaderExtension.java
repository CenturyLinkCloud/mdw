/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.jsf;

import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;

import org.apache.myfaces.shared_impl.util.ClassLoaderExtension;

import com.centurylink.mdw.common.cache.impl.PackageVOCache;
import com.centurylink.mdw.common.cache.impl.RuleSetCache;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.java.CompiledJavaCache;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.process.PackageVO;
import com.centurylink.mdw.web.ui.model.MDWProcessInstance;

public class MdwClassLoaderExtension extends ClassLoaderExtension
{
  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  @Override
  public Class<?> forName(String name)
  {
    if (managedBeanClasses != null && managedBeanClasses.contains(name))
    {
      if (logger.isMdwDebugEnabled())
        logger.mdwDebug("TaskManager: Loading class for managed bean: " + name);

      try
      {
        int lastDot = name.lastIndexOf('.');
        PackageVO javaAssetPackage = null;
        if (lastDot == -1)
        {
          // default package
          javaAssetPackage = PackageVOCache.getDefaultPackageVO();
        }
        else
        {
          String packageName = name.substring(0, lastDot);
          javaAssetPackage = PackageVOCache.getPackageVO(packageName);
        }

        RuleSetVO javaRuleSet = RuleSetCache.getRuleSet(name, RuleSetVO.JAVA);
        if (javaRuleSet == null)
          throw new ClassNotFoundException(name);

        PackageVO runtimePackage = javaAssetPackage;
        // runtime classloader package: try current process's package (is already main for embedded subprocs)
        if (!FacesContext.getCurrentInstance().getExternalContext().getClass().getSimpleName().equals("StartupServletExternalContextImpl"))
        {
          // this can happen for application-scoped managed beans with eager=true; at startup stage process bean is not relevant
          MDWProcessInstance process = (MDWProcessInstance) FacesVariableUtil.getValue("process");
          if (process != null)
          {
            PackageVO processPackage = PackageVOCache.getProcessPackage(process.getProcessId());
            if (processPackage.getBundleSpec() != null)
              runtimePackage = processPackage;
          }
        }

        return CompiledJavaCache.getClass(getClass().getClassLoader(), runtimePackage, name, javaRuleSet.getRuleSet());
      }
      catch (Throwable th)
      {
        logger.severeException(th.getMessage(), th);
        return null;
      }
    }
    else
    {
      return super.forName(name);
    }
  }

  private List<String> managedBeanClasses;

  public void addManagedBeanClass(String className)
  {
    if (managedBeanClasses == null)
      managedBeanClasses = new ArrayList<String>();
    managedBeanClasses.add(className);
  }
}
