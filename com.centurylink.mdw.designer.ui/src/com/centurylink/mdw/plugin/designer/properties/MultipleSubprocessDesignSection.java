package com.centurylink.mdw.plugin.designer.properties;

import org.eclipse.jface.viewers.IFilter;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;

import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.model.value.attribute.AssetVersionSpec;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.plugin.designer.model.Activity;
import com.centurylink.mdw.plugin.designer.model.WorkflowProcess;
import com.centurylink.mdw.plugin.designer.properties.editor.AssetLocator;
import com.centurylink.mdw.plugin.designer.properties.editor.PropertyEditor;
import com.centurylink.mdw.plugin.designer.properties.editor.TableEditor.DefaultRowImpl;
import com.centurylink.mdw.plugin.designer.properties.editor.ValueChangeListener;

public class MultipleSubprocessDesignSection extends DesignSection implements IFilter
{
  @Override
  protected void preRender(final PropertyEditor propertyEditor)
  {
    if (WorkAttributeConstant.PROCESS_MAP.equals(propertyEditor.getName()))
    {
      propertyEditor.addValueChangeListener(new ValueChangeListener()
      {
        public void propertyValueChanged(Object newValue)
        {
          DefaultRowImpl row = (DefaultRowImpl) newValue;
          String procName = row.getColumnValues()[2];
          String ver = row.getColumnValues()[3];
          if (procName != null && ver != null) {
            AssetVersionSpec spec = new AssetVersionSpec(procName, ver);
            AssetLocator locator = new AssetLocator(getActivity(), AssetLocator.Type.Process);
            WorkflowProcess subproc = locator.getProcessVersion(spec);
            if (subproc != null)
              openSubprocess(subproc);
          }
        }
      });
    }
  }

  @Override
  public boolean select(Object toTest)
  {
    if (toTest == null || !(toTest instanceof Activity))
      return false;

    Activity activity = (Activity) toTest;

    if (activity.isForProcessInstance())
      return false;

    return activity.isHeterogeneousSubProcInvoke();
  }

  private void openSubprocess(WorkflowProcess subproc)
  {
    IWorkbenchPage page = MdwPlugin.getActivePage();
    try
    {
      page.openEditor(subproc, "mdw.editors.process");
    }
    catch (PartInitException ex)
    {
      PluginMessages.uiError(ex, "Open Process", subproc.getProject());
    }
  }
}
