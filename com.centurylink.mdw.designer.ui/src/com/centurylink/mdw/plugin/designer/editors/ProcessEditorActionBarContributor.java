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
package com.centurylink.mdw.plugin.designer.editors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;

import com.centurylink.mdw.designer.display.Node;
import com.centurylink.mdw.plugin.MdwPlugin;
import com.centurylink.mdw.plugin.preferences.model.PreferenceConstants;

/**
 * Actions for the process editor.
 */
public class ProcessEditorActionBarContributor implements IEditorActionBarContributor {
    private ProcessEditor editor;

    private IAction lockAction;

    public IAction getLockAction() {
        return lockAction;
    }

    private IAction runAction;
    private IAction debugAction;
    private IAction instancesAction;
    private IMenuCreator linkTypeMenuCreator;
    private IAction linkTypeAction;
    private IMenuCreator iconShapeMenuCreator;
    private IAction iconShapeAction;
    private IAction processDefinitionAction;
    private ZoomLevelMenuCreator zoomLevelMenuCreator;
    private IAction zoomLevelAction;
    private IAction exportAsAction;
    private IAction refreshAction;
    private IMenuCreator displayPrefsMenuCreator;
    private IAction displayPrefsAction;
    private IAction recordChangesAction;
    private IAction commitChangesAction;

    private static String linkStyle = "Elbow";

    public static String getLinkStyle() {
        return linkStyle;
    }

    public void init(IActionBars bars, IWorkbenchPage page) {
        lockAction = createLockAction();
        runAction = createRunAction();
        debugAction = createDebugAction();
        instancesAction = createInstancesAction();
        linkTypeMenuCreator = new LinkTypeMenuCreator();
        linkTypeAction = createLinkTypeAction();
        iconShapeMenuCreator = new IconShapeMenuCreator();
        iconShapeAction = createIconShapeAction();
        processDefinitionAction = createProcessDefinitionAction();
        zoomLevelMenuCreator = new ZoomLevelMenuCreator();
        zoomLevelAction = createZoomLevelAction();
        exportAsAction = createExportAsAction();
        refreshAction = createRefreshAction();
        displayPrefsMenuCreator = new DisplayPrefsMenuCreator();
        displayPrefsAction = createDisplayPrefsAction();
        recordChangesAction = createRecordChangesAction();
        commitChangesAction = createCommitChangesAction();

        IToolBarManager toolbar = bars.getToolBarManager();
        toolbar.add(new GroupMarker("mdw.process.instance.group"));
        toolbar.add(lockAction);
        toolbar.add(runAction);
        toolbar.add(debugAction);
        toolbar.add(instancesAction);
        toolbar.add(linkTypeAction);
        toolbar.add(iconShapeAction);
        toolbar.add(processDefinitionAction);
        toolbar.add(zoomLevelAction);
        toolbar.add(displayPrefsAction);
        toolbar.add(exportAsAction);
        toolbar.add(refreshAction);
        toolbar.add(recordChangesAction);
        toolbar.add(commitChangesAction);
    }

    public void setActiveEditor(IEditorPart targetEditor) {
        if (targetEditor instanceof ProcessEditor) {
            this.editor = (ProcessEditor) targetEditor;
            lockAction.setEnabled(!editor.isForProcessInstance() && editor.isLockAllowed());
            runAction.setEnabled(!editor.isForProcessInstance() && editor.isLaunchAllowed());
            debugAction.setEnabled(!editor.isForProcessInstance() && editor.isDebugAllowed());
            instancesAction.setEnabled(!editor.isForProcessInstance());
            linkTypeAction.setEnabled(!editor.isForProcessInstance() && !editor.isReadOnly());
            iconShapeAction.setEnabled(!editor.isForProcessInstance() && !editor.isReadOnly());
            processDefinitionAction.setEnabled(editor.isForProcessInstance());
            displayPrefsAction.setEnabled(!editor.isForProcessInstance());
            recordChangesAction.setEnabled(!editor.isForProcessInstance() && !editor.isReadOnly());
            commitChangesAction.setEnabled(!editor.isForProcessInstance() && !editor.isReadOnly());

            if (editor.isForProcessInstance()) {
                lockAction.setText("");
                runAction.setText("");
                debugAction.setText("");
                instancesAction.setText("");
                linkTypeAction.setText("");
                iconShapeAction.setText("");
                processDefinitionAction.setText("Process Definition");
                refreshAction.setText("Refresh Process Instance");
                displayPrefsAction.setText("");
                recordChangesAction.setText("");
                commitChangesAction.setText("");
            }
            else {
                if (editor.isLaunchAllowed())
                    runAction.setText("Launch Process");
                else
                    runAction.setText("");

                if (editor.isDebugAllowed())
                    debugAction.setText("Debug Process");
                else
                    debugAction.setText("");

                if (editor.isLockAllowed())
                    lockAction.setText("Lock/Unlock Process");
                else
                    lockAction.setText("");
                lockAction.setChecked(editor.getProcess().isLockedToUser());
                instancesAction.setText("Process Instances");
                linkTypeAction.setText("Link Style");
                processDefinitionAction.setText("");
                iconShapeAction.setText("Icon Shape");
                refreshAction.setText("Refresh Process");

                displayPrefsAction.setText("Display Prefs");
                if (editor.isRecordChanges())
                    recordChangesAction.setText("Stop Recording Changes");
                else
                    recordChangesAction.setText("Record Changes");
                recordChangesAction.setChecked(editor.isRecordChanges());
                commitChangesAction.setText("Commit Changes");
            }

            exportAsAction.setText("Export As...");
            zoomLevelAction.setText("Zoom Level");
            zoomLevelMenuCreator.setZoomable(editor);
        }
    }

    private IAction createLockAction() {
        IAction action = new Action("", IAction.AS_CHECK_BOX) {
            public void run() {
                editor.toggleProcessLock(isChecked());
            }
        };
        ImageDescriptor descriptor = MdwPlugin.getImageDescriptor("icons/lock.gif");
        action.setImageDescriptor(descriptor);
        return action;
    }

    private IAction createRecordChangesAction() {
        IAction action = new Action("", IAction.AS_CHECK_BOX) {
            public void run() {
                editor.setRecordChanges(isChecked());
            }
        };
        ImageDescriptor descriptor = MdwPlugin.getImageDescriptor("icons/record.gif");
        action.setImageDescriptor(descriptor);
        return action;
    }

    private IAction createCommitChangesAction() {
        IAction action = new Action() {
            public void run() {
                editor.commitChanges();
            }
        };
        action.setText("");
        ImageDescriptor descriptor = MdwPlugin.getImageDescriptor("icons/commit.gif");
        action.setImageDescriptor(descriptor);
        return action;
    }

    private IAction createRunAction() {
        IAction action = new Action() {
            public void run() {
                editor.launchProcess(false);
            }
        };
        action.setText("");
        ImageDescriptor descriptor = MdwPlugin.getImageDescriptor("icons/run.gif");
        action.setImageDescriptor(descriptor);
        return action;
    }

    private IAction createDebugAction() {
        IAction action = new Action() {
            public void run() {
                editor.launchProcess(true);
            }
        };
        action.setText("");
        ImageDescriptor descriptor = MdwPlugin.getImageDescriptor("icons/debug.gif");
        action.setImageDescriptor(descriptor);
        return action;
    }

    private IAction createInstancesAction() {
        IAction action = new Action() {
            public void run() {
                editor.showInstances();
            }
        };
        action.setText("");
        ImageDescriptor descriptor = MdwPlugin.getImageDescriptor("icons/list.gif");
        action.setImageDescriptor(descriptor);
        return action;
    }

    private IAction createLinkTypeAction() {
        IAction action = new Action(null, IAction.AS_DROP_DOWN_MENU) {
            public IMenuCreator getMenuCreator() {
                return linkTypeMenuCreator;
            }
        };
        action.setText("Link Style");
        ImageDescriptor descriptor = MdwPlugin.getImageDescriptor("icons/link.gif");
        action.setImageDescriptor(descriptor);
        return action;
    }

    private IAction createIconShapeAction() {
        IAction action = new Action(null, IAction.AS_DROP_DOWN_MENU) {
            public IMenuCreator getMenuCreator() {
                return iconShapeMenuCreator;
            }
        };
        action.setText("Icon Shape");
        ImageDescriptor descriptor = MdwPlugin.getImageDescriptor("icons/shape.gif");
        action.setImageDescriptor(descriptor);
        return action;
    }

    private IAction createProcessDefinitionAction() {
        IAction action = new Action() {
            public void run() {
                editor.openProcessDefinition();
            }
        };
        action.setText("");
        ImageDescriptor descriptor = MdwPlugin.getImageDescriptor("icons/process.gif");
        action.setImageDescriptor(descriptor);
        return action;
    }

    private IAction createZoomLevelAction() {
        IAction action = new Action(null, IAction.AS_DROP_DOWN_MENU) {
            public IMenuCreator getMenuCreator() {
                return zoomLevelMenuCreator;
            }
        };
        action.setText("Zoom Level");
        ImageDescriptor descriptor = MdwPlugin.getImageDescriptor("icons/zoom.gif");
        action.setImageDescriptor(descriptor);
        return action;
    }

    private IAction createExportAsAction() {
        IAction action = new Action() {
            public void run() {
                editor.exportAs();
            }
        };
        action.setText("");
        ImageDescriptor descriptor = MdwPlugin.getImageDescriptor("icons/export_as.gif");
        action.setImageDescriptor(descriptor);
        return action;
    }

    private IAction createRefreshAction() {
        IAction action = new Action() {
            public void run() {
                editor.refresh();
            }
        };
        action.setText("Refresh");
        ImageDescriptor descriptor = MdwPlugin.getImageDescriptor("icons/refresh.gif");
        action.setImageDescriptor(descriptor);
        return action;
    }

    private IAction createDisplayPrefsAction() {
        IAction action = new Action(null, IAction.AS_DROP_DOWN_MENU) {
            public IMenuCreator getMenuCreator() {
                return displayPrefsMenuCreator;
            }
        };
        action.setText("Display Prefs");
        ImageDescriptor descriptor = MdwPlugin.getImageDescriptor("icons/display_prefs.gif");
        action.setImageDescriptor(descriptor);
        return action;
    }

    public void dispose() {
        if (linkTypeMenuCreator != null)
            linkTypeMenuCreator.dispose();
        if (iconShapeMenuCreator != null)
            iconShapeMenuCreator.dispose();
        if (displayPrefsMenuCreator != null)
            displayPrefsMenuCreator.dispose();
    }

    class LinkTypeMenuCreator implements IMenuCreator {
        Menu menu;
        MenuItem straightItem;
        MenuItem elbow1Item;
        MenuItem elbow2Item;
        MenuItem elbow3Item;
        MenuItem curve1Item;

        public Menu getMenu(Control parent) {
            menu = createMenu(parent);
            return menu;
        }

        private Menu createMenu(Control parent) {
            Menu menu = new Menu(parent);

            // elbow
            elbow1Item = new MenuItem(menu, SWT.RADIO);
            elbow1Item.setData("Elbow");
            ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/elbow1.gif");
            elbow1Item.setImage(imageDesc.createImage());
            elbow1Item.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    select(elbow1Item);
                }
            });
            elbow1Item.setSelection(true);

            // straight
            straightItem = new MenuItem(menu, SWT.RADIO);
            straightItem.setData("Straight");
            imageDesc = MdwPlugin.getImageDescriptor("icons/straight.gif");
            straightItem.setImage(imageDesc.createImage());
            straightItem.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    select(straightItem);
                }
            });

            // elbowH
            elbow2Item = new MenuItem(menu, SWT.RADIO);
            elbow2Item.setData("ElbowH");
            imageDesc = MdwPlugin.getImageDescriptor("icons/elbow2.gif");
            elbow2Item.setImage(imageDesc.createImage());
            elbow2Item.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    select(elbow2Item);
                }
            });

            // elbowV
            elbow3Item = new MenuItem(menu, SWT.RADIO);
            elbow3Item.setData("ElbowV");
            imageDesc = MdwPlugin.getImageDescriptor("icons/elbow3.gif");
            elbow3Item.setImage(imageDesc.createImage());
            elbow3Item.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    select(elbow3Item);
                }
            });

            // curved1
            curve1Item = new MenuItem(menu, SWT.RADIO);
            curve1Item.setData("Curve");
            imageDesc = MdwPlugin.getImageDescriptor("icons/curved1.gif");
            curve1Item.setImage(imageDesc.createImage());
            curve1Item.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    select(curve1Item);
                }
            });

            if (linkStyle.equals("Elbow"))
                select(elbow1Item);
            else if (linkStyle.equals("Straight"))
                select(straightItem);
            else if (linkStyle.equals("ElbowH"))
                select(elbow2Item);
            else if (linkStyle.equals("ElbowV"))
                select(elbow3Item);
            else if (linkStyle.equals("Curve"))
                select(curve1Item);

            return menu;
        }

        private void select(MenuItem item) {
            straightItem.setSelection(false);
            elbow1Item.setSelection(false);
            elbow2Item.setSelection(false);
            elbow3Item.setSelection(false);
            curve1Item.setSelection(false);
            item.setSelection(true);

            linkStyle = (String) item.getData();
            editor.setCanvasLinkStyle(linkStyle);
        }

        public Menu getMenu(Menu parent) {
            // not used
            return null;
        }

        public void dispose() {
            if (menu != null)
                menu.dispose();
        }
    }

    class IconShapeMenuCreator implements IMenuCreator {
        Menu menu;
        MenuItem iconItem;
        MenuItem boxItem;
        MenuItem iconBoxItem;

        public Menu getMenu(Control parent) {
            menu = createMenu(parent);
            return menu;
        }

        private Menu createMenu(Control parent) {
            Menu menu = new Menu(parent);

            // iconBox
            iconBoxItem = new MenuItem(menu, SWT.RADIO);
            iconBoxItem.setData("BoxIcon");
            ImageDescriptor imageDesc = MdwPlugin.getImageDescriptor("icons/node-box-icon.gif");
            iconBoxItem.setImage(imageDesc.createImage());
            iconBoxItem.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    select(iconBoxItem);
                }
            });

            // icon
            iconItem = new MenuItem(menu, SWT.RADIO);
            iconItem.setData("Icon");
            imageDesc = MdwPlugin.getImageDescriptor("icons/node-icon.gif");
            iconItem.setImage(imageDesc.createImage());
            iconItem.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    select(iconItem);
                }
            });

            // box
            boxItem = new MenuItem(menu, SWT.RADIO);
            boxItem.setData("Box");
            imageDesc = MdwPlugin.getImageDescriptor("icons/node-box.gif");
            boxItem.setImage(imageDesc.createImage());
            boxItem.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    select(boxItem);
                }
            });

            String nodeStyle = editor.getProcess().getAttribute("NodeStyle");
            if (nodeStyle == null)
                nodeStyle = "Icon";

            if (nodeStyle.equals("BoxIcon"))
                select(iconBoxItem);
            else if (nodeStyle.equals("Icon"))
                select(iconItem);
            else if (nodeStyle.equals("Box"))
                select(boxItem);

            return menu;
        }

        private void select(MenuItem item) {
            iconItem.setSelection(false);
            boxItem.setSelection(false);
            iconBoxItem.setSelection(false);
            item.setSelection(true);

            String nodeShape = (String) item.getData();
            editor.setCanvasNodeStyle(nodeShape);
        }

        public Menu getMenu(Menu parent) {
            // not used
            return null;
        }

        public void dispose() {
            if (menu != null)
                menu.dispose();
        }
    }

    class DisplayPrefsMenuCreator implements IMenuCreator {
        Menu menu;
        MenuItem nodeIdTypeLogicalItem;
        MenuItem nodeIdTypeSequenceItem;
        MenuItem nodeIdTypeReferenceItem;
        MenuItem nodeIdTypeDatabaseItem;
        MenuItem nodeIdTypeNoneItem;
        MenuItem showToolTipsItem;

        public Menu getMenu(Control parent) {
            menu = createMenu(parent);
            return menu;
        }

        private Menu createMenu(Control parent) {
            Menu menu = new Menu(parent);

            MenuItem subMenuItem = new MenuItem(menu, SWT.CASCADE);
            subMenuItem.setText("Node ID Type");

            Menu subMenu = new Menu(subMenuItem);
            subMenuItem.setMenu(subMenu);

            // logical
            nodeIdTypeLogicalItem = new MenuItem(subMenu, SWT.RADIO);
            nodeIdTypeLogicalItem.setData(Node.ID_LOGICAL);
            nodeIdTypeLogicalItem.setText("Logical ID");
            nodeIdTypeLogicalItem.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    selectNodeIdType(nodeIdTypeLogicalItem);
                    MdwPlugin.getDefault().getPreferenceStore().setValue(
                            PreferenceConstants.PREFS_DESIGNER_CANVAS_NODE_ID_TYPE,
                            Node.ID_LOGICAL);
                }
            });

            // sequence
            nodeIdTypeSequenceItem = new MenuItem(subMenu, SWT.RADIO);
            nodeIdTypeSequenceItem.setData(Node.ID_SEQUENCE);
            nodeIdTypeSequenceItem.setText("Sequence Number");
            nodeIdTypeSequenceItem.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    selectNodeIdType(nodeIdTypeSequenceItem);
                    MdwPlugin.getDefault().getPreferenceStore().setValue(
                            PreferenceConstants.PREFS_DESIGNER_CANVAS_NODE_ID_TYPE,
                            Node.ID_SEQUENCE);
                }
            });

            // reference
            nodeIdTypeReferenceItem = new MenuItem(subMenu, SWT.RADIO);
            nodeIdTypeReferenceItem.setData(Node.ID_REFERENCE);
            nodeIdTypeReferenceItem.setText("Reference ID");
            nodeIdTypeReferenceItem.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    selectNodeIdType(nodeIdTypeReferenceItem);
                    MdwPlugin.getDefault().getPreferenceStore().setValue(
                            PreferenceConstants.PREFS_DESIGNER_CANVAS_NODE_ID_TYPE,
                            Node.ID_REFERENCE);
                }
            });

            // database
            nodeIdTypeDatabaseItem = new MenuItem(subMenu, SWT.RADIO);
            nodeIdTypeDatabaseItem.setData(Node.ID_DATABASE);
            nodeIdTypeDatabaseItem.setText("System ID");
            nodeIdTypeDatabaseItem.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    selectNodeIdType(nodeIdTypeDatabaseItem);
                    MdwPlugin.getDefault().getPreferenceStore().setValue(
                            PreferenceConstants.PREFS_DESIGNER_CANVAS_NODE_ID_TYPE,
                            Node.ID_DATABASE);
                }
            });

            // none
            nodeIdTypeNoneItem = new MenuItem(subMenu, SWT.RADIO);
            nodeIdTypeNoneItem.setData(Node.ID_NONE);
            nodeIdTypeNoneItem.setText("None");
            nodeIdTypeNoneItem.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    selectNodeIdType(nodeIdTypeNoneItem);
                    MdwPlugin.getDefault().getPreferenceStore().setValue(
                            PreferenceConstants.PREFS_DESIGNER_CANVAS_NODE_ID_TYPE, Node.ID_NONE);
                }
            });

            String nodeIdType = editor.getNodeIdType();

            if (nodeIdType.equals(Node.ID_DATABASE))
                selectNodeIdType(nodeIdTypeDatabaseItem);
            else if (nodeIdType.equals(Node.ID_LOGICAL))
                selectNodeIdType(nodeIdTypeLogicalItem);
            else if (nodeIdType.equals(Node.ID_SEQUENCE))
                selectNodeIdType(nodeIdTypeSequenceItem);
            else if (nodeIdType.equals(Node.ID_REFERENCE))
                selectNodeIdType(nodeIdTypeReferenceItem);
            else if (nodeIdType.equals(Node.ID_NONE))
                selectNodeIdType(nodeIdTypeNoneItem);

            // showToolTips
            showToolTipsItem = new MenuItem(menu, SWT.CHECK);
            showToolTipsItem.setText("Show Tool Tips");
            showToolTipsItem.setEnabled(!editor.isForProcessInstance());
            showToolTipsItem.addSelectionListener(new SelectionAdapter() {
                public void widgetSelected(SelectionEvent e) {
                    boolean isShowTips = showToolTipsItem.getSelection();
                    editor.setShowToolTips(isShowTips);
                    MdwPlugin.getDefault().getPreferenceStore().setValue(
                            PreferenceConstants.PREFS_DESIGNER_SUPPRESS_TOOLTIPS, !isShowTips);
                }
            });

            showToolTipsItem.setSelection(editor.isShowToolTips());

            return menu;
        }

        private void selectNodeIdType(MenuItem item) {
            nodeIdTypeDatabaseItem.setSelection(false);
            nodeIdTypeLogicalItem.setSelection(false);
            nodeIdTypeSequenceItem.setSelection(false);
            nodeIdTypeReferenceItem.setSelection(false);
            nodeIdTypeNoneItem.setSelection(false);
            item.setSelection(true);

            String nodeIdType = (String) item.getData();
            editor.setNodeIdType(nodeIdType);
        }

        public Menu getMenu(Menu parent) {
            // not used
            return null;
        }

        public void dispose() {
            if (menu != null)
                menu.dispose();
        }
    }
}
