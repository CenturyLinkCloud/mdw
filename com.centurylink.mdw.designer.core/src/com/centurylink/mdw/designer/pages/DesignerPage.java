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
package com.centurylink.mdw.designer.pages;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JToolBar;

import com.centurylink.mdw.common.constant.WorkAttributeConstant;
import com.centurylink.mdw.common.exception.DataAccessException;
import com.centurylink.mdw.dataaccess.DataAccess;
import com.centurylink.mdw.designer.MainFrame;
import com.centurylink.mdw.designer.display.DesignerDataModel;
import com.centurylink.mdw.designer.display.Graph;
import com.centurylink.mdw.designer.display.Node;
import com.centurylink.mdw.designer.display.SubGraph;
import com.centurylink.mdw.designer.icons.IconFactory;
import com.centurylink.mdw.designer.utils.Constants;
import com.centurylink.mdw.designer.utils.ProcessWorker;
import com.centurylink.mdw.designer.utils.Server;
import com.centurylink.mdw.designer.utils.ValidationException;
import com.centurylink.mdw.model.value.activity.ActivityVO;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.process.ProcessVO;
import com.centurylink.mdw.model.value.user.UserGroupVO;

public abstract class DesignerPage extends JPanel implements ActionListener {

    protected static final String ACTION_TESTING = "Testing";
    protected static final String ACTION_SCRIPT = "ACTION_SCRIPT";

    public static enum PersistType {
        CREATE, UPDATE, NEW_VERSION, CANCEL, SAVE
    }

	protected JMenuBar menubar;
	public MainFrame frame;
	protected JToolBar toolbar;
	protected DesignerDataModel model;

	protected IconFactory getIconFactory() {
	    return frame.getIconFactory();
	}
	protected Icon getIcon(String name) {
	    return getIconFactory().getIcon(name);
	}

	public DesignerPage(MainFrame frame) {
		super();
		this.frame = frame;
		model = frame.getDataModel();
		setLayout(new BorderLayout());
		createMenuBar();
		createToolBar();
	}

	public JMenuBar getMenuBar(boolean isAdditionalMainFrame) {
		return menubar;
	}

	public abstract void createMenuBar();

	public abstract void createToolBar();

	public JButton createToolButton(String name, String iconname,
			String tip, String action, ActionListener listener) {
		JButton button;
		if (iconname!=null) {
		    button = new JButton(new ImageIcon(this.getClass().getClassLoader().getResource("images/" + iconname)));
		    //button.setMinimumSize(new Dimension(36,36));
		    //button.setSize(new Dimension(36,36));
		    //button.setPreferredSize(new Dimension(36,36));
		} else button = new JButton(name);
		if (tip!=null) button.setToolTipText(tip);
		button.setActionCommand(action);
		button.addActionListener(listener);
        if(name != null) {
            if(name.equalsIgnoreCase("Delete")) {
                button.setEnabled(true);
            }
        }
		toolbar.add(button);
		return button;
	}

	public JMenuItem createMenuItem(JMenu menu, String label, String action, ActionListener listener) {
		JMenuItem menuitem = new JMenuItem(label);
		menuitem.addActionListener(listener);
		menu.add(menuitem);
		menuitem.setActionCommand(action);
		return menuitem;
	}

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
    }

	public void createToolRadio(ButtonGroup group, String label, String iconname,
			boolean selected, String action, ActionListener listener) {
		JRadioButton button;
        if (iconname!=null) {
            button = new JRadioButton(new ImageIcon(this.getClass().getClassLoader().getResource("images/" + iconname)),
                    	selected);
            button.setSelectedIcon(new ImageIcon(this.getClass().getClassLoader().getResource("images/selected_" + iconname)));
        } else button = new JRadioButton(label, selected);
		button.setActionCommand(action);
		button.addActionListener(listener);
		toolbar.add(button);
		group.add(button);
	}

	public JComboBox createToolDropdown(String[] choices,
			String action, ActionListener listener, int selected, int width) {
		JComboBox jcb = new JComboBox(choices);
		jcb.setSelectedIndex(selected);
		jcb.setActionCommand(action);
		jcb.addActionListener(listener);
        if (width>0) {
            Dimension d = new Dimension(width, 30);
//          jcb.setSize(d);           // this does not help
//          jcb.setPreferredSize(d);  // this does not help either
            jcb.setMaximumSize(d);
        }
        toolbar.addSeparator();
		toolbar.add(jcb);
		return jcb;
	}

	public JComboBox createToolDropdown(String[] choices, String[] images, String tip,
			String action, ActionListener listener, int selected, int width) {
		JComboBox jcb = new JComboBox();
		ImageIcon icon;
		for (int i=0; i<images.length; i++) {
			icon = new ImageIcon(this.getClass().
					getClassLoader().getResource("images/" + images[i]));
			jcb.addItem(icon);
			icon.setDescription(choices[i]);
		}
		jcb.setSelectedIndex(selected);
		jcb.setActionCommand(action);
		jcb.addActionListener(listener);
		if (width>0) {
		    Dimension d = new Dimension(width, 30);
//		    jcb.setSize(d);           // this does not help
//		    jcb.setPreferredSize(d);  // this does not help either
            jcb.setMaximumSize(d);
		}
		if (tip!=null) jcb.setToolTipText(tip);
        toolbar.addSeparator();
		toolbar.add(jcb);
		return jcb;
	}

	public JCheckBox createToolCheckBox(String label, boolean selected, String action, ActionListener listener) {
        JCheckBox button;
        button = new JCheckBox(label, selected);
        button.setActionCommand(action);
        button.addActionListener(listener);
        toolbar.add(button);
        return button;
    }

	public boolean getConfirmation(String message) {
        return frame.getOptionPane().confirm(this, message, true);
	}

	public void showMessage(String message) {
	    frame.getOptionPane().showMessage(this, message);
	}

	public void showError(String message) {
	    frame.getOptionPane().showError(this, message);
	}

	public void showError(Component parent, String message) {
	    frame.getOptionPane().showError(parent, message);
	}

	private void collectErrorMessages(Throwable e, List<String> msgs, int level) {
	    String msg = e.getMessage();
	    if (msg==null) msg = e.getClass().getName();
	    if (msg!=null && msg.length()>256) msg = msg.substring(0,256);
	    if (!msgs.contains(msg)) msgs.add(msg);
	    if (level<4 && e.getCause()!=null) collectErrorMessages(e.getCause(), msgs, level+1);
	}

	public void showError(String msg, Throwable e) {
        showError(this, msg, e);
	}

	public void showError(Component parent, String msg, Throwable e) {
	    List<String> msgs = new ArrayList<String>(6);
        if (msg!=null) msgs.add(msg);
        collectErrorMessages(e, msgs, 0);
        StringBuffer sb = new StringBuffer();
        for (int i=0; i<msgs.size(); i++) sb.append(msgs.get(i)).append('\n');
        frame.getOptionPane().showError(parent, sb.toString());
	}

    public void showErrorDelayed(String msg) {
        frame.errmsg = msg;
    }

    /**
     *
     * @param graph
     * @return process ID if saving is successful; null o/w
     */
	private String save_process(Graph graph, PersistType persistType, int version, boolean lock) throws ValidationException {
        String processId = null;
		ProcessVO process = graph.getProcessVO();
		int currentVersion = process.getVersion();
		try {
			frame.log("Saving process " + process.getProcessName() + " ...");
			new ProcessWorker().convert_from_designer(process, model.getNodeMetaInfo());
			if (persistType==PersistType.CREATE) {
				processId = frame.dao.createProcess(process);
            } else {
                processId = frame.dao.updateProcess(process, version, lock);
                process.setVersion(currentVersion);
                // keep the version number
            }
			graph.setDirtyLevel(Graph.CLEAN);
			graph.setReadonly(true);
			graph.getProcessVO().clearDeletedTransitions();
		} catch (Exception e) {
		    // TODO better not to convert back - keep the original
		    new ProcessWorker().convert_to_designer(process);
		    process.setVersion(currentVersion);
			e.printStackTrace();
			throw new ValidationException(e.getCause() == null? e.getMessage() : e.getCause().getMessage());
		}
        return processId;
	}

	public String getTitle() {
		return "MDW Designer";
	}

	private void setProcessVersion(Node node, boolean includeId) {
		String procname = node.getAttribute(WorkAttributeConstant.PROCESS_NAME);
        if (procname!=null) {
            ProcessVO procdef = findProcessDefinition(procname, 0);
            if (procdef!=null) {
                node.setAttribute(WorkAttributeConstant.PROCESS_VERSION, Integer.toString(procdef.getVersion()));
                if (includeId)
                  node.setAttribute("process_id", procdef.getProcessId().toString());
                node.setAttribute(WorkAttributeConstant.ALIAS_PROCESS_ID, null);
            }
        }
	}

	private void setSynchronizationActivity(Node node) {
        List<ActivityVO> as = node.graph.getProcessVO().
        	getUpstreamActivities(node.getId());
        StringBuffer sb = new StringBuffer();
        for (ActivityVO a : as) {
        	if (sb.length()>0) sb.append("#");
        	sb.append(a.getActivityName());
        }
        node.setAttribute(WorkAttributeConstant.SYNCED_ACTIVITIES, sb.toString());
    }

	public void setProcessVersions(Graph process, boolean includeIds) {
	    for (Node node : process.nodes) {
	        if (node.isSubProcessActivity()) {
	        	setProcessVersion(node, includeIds);
			} else if (node.isSynchronizationActivity()) {
				setSynchronizationActivity(node);
	        }
	    }
	    if (process.subgraphs!=null) {
	    	for (SubGraph subgraph : process.subgraphs) {
	    		for (Node node : subgraph.nodes) {
	    			if (node.isSubProcessActivity()) {
	    				setProcessVersion(node, includeIds);
	    			} else if (node.isSynchronizationActivity()) {
	    				setSynchronizationActivity(node);
	    			}
	    	    }
	    	}
	    }
	}

	/**
	 * Loads a process for use by Designer.
	 */
	public ProcessVO loadProcess(ProcessVO procdef) throws RemoteException,DataAccessException {
	    ProcessVO process = new ProcessVO(frame.dao.getProcess(procdef.getProcessId(), procdef));
	    // these don't get set by the clone
	    process.setId(procdef.getId());
        process.setNextVersion(procdef.getNextVersion());
        process.setPrevVersion(procdef.getPrevVersion());
	    new ProcessWorker().convert_to_designer(process);
	    return process;
	}

    /**
     * Saves and then reload the process graph
     */
    public Graph saveProcess(Graph process, MainFrame frame, PersistType persistType, int version, boolean lock)
    throws ValidationException, DataAccessException, RemoteException {
        String processId = save_process(process, persistType, version, lock);
        if(processId==null)
            throw new ValidationException("Error in saving process");
        // load process again
        ProcessVO procdef = process.getProcessVO();
        if (persistType==PersistType.CREATE) {
            procdef.setProcessId(new Long(processId));
            procdef = loadProcess(procdef);
        } else if (persistType==PersistType.NEW_VERSION) {
            procdef = frame.dao.getProcess(new Long(processId), procdef);
            // procdef is overridden with the new version here
            new ProcessWorker().convert_to_designer(procdef);
            model.addProcess(procdef);
        } else {
            procdef = loadProcess(procdef);
        }
        process.reinit(procdef);
        return process;
    }

    /**
     * find out process ID of a given process name through
     * the process list. If there are more than one version
     * of the process, return the ID of the latest version.
     */
    private ProcessVO findProcessDefinition(String processName, int version) {
        ProcessVO procdef = model.findProcessDefinition(processName, version);
        if (procdef!=null) return procdef;
        try {
            procdef = frame.dao.getProcessDefinition(processName, version);
            model.addPrivateProcess(procdef);
        } catch (Exception e) {
            frame.dao.renewServer();
            procdef = null;
        }
        return procdef;
    }

    public ProcessVO findProcessDefinition(Long procid, String server) {
        ProcessVO procdef = model.findProcessDefinition(procid, server);
        if (procdef!=null) return procdef;
        try {
            procdef = frame.dao.getProcessDefinition(procid);
            model.addPrivateProcess(procdef);
        } catch (Exception e) {
            procdef = null;
        }
        return procdef;
    }

    public void show_process(Graph process) {
        FlowchartPage flowchart = (FlowchartPage)frame.getPage(FlowchartPage.class);
        flowchart.setProcess(process);
        frame.setPage(flowchart);
    }

    public void actionPerformed(ActionEvent event) {
        String cmd = event.getActionCommand();
        if (cmd.equals(Constants.ACTION_REFRESH_CACHE)) {
            try {
            	if (serverIsRunning(false)) {
	                frame.dao.refreshServerCaches(null);
            		showMessage("Please wait for 15 sec for the server to refresh all\n"
            				+ "Check for server log for possible errors");
            	}
            } catch (Exception e) {
                showError(e.getMessage());
            }
        } else {
            showError("The function is not implemented");
        }
    }

    /**
     * Loads a process for use by Designer.
     */
    public Graph loadProcess(Long processId, String server) {
        Graph process = model.findProcessGraph(processId, server);
        if (process != null)
          return process;
        ProcessVO procdef = findProcessDefinition(processId, server);
        try {
            procdef = loadProcess(procdef);
            process = new Graph(procdef, model.getNodeMetaInfo(), frame.getIconFactory());
            model.addProcessGraph(process);
        }
        catch (Exception ex) {
          ex.printStackTrace();
        }
        return process;
    }

    public DesignerDataModel getDataModel() {
        return model;
    }

    protected void addCommonToolButtons(String[] actions) {
    	for (String action : actions) {
    		if (action.equals(Constants.ACTION_PROCLIST)) {
    			createToolButton(null,"open.gif", "Process List", Constants.ACTION_PROCLIST, this);
    		} else if (action.equals(Constants.ACTION_PACKAGE)) {
    			if (model.getDatabaseSchemaVersion()>=DataAccess.schemaVersion4
    					&& !frame.dao.noDatabase())
    	            createToolButton(null,"package.gif","Packages",Constants.ACTION_PACKAGE,this);
    		} else if (action.equals(ACTION_TESTING)) {
    			if (model.getDatabaseSchemaVersion()>=DataAccess.schemaVersion4) {
    	            createToolButton(null,"testing.gif","Regression test cases",ACTION_TESTING,this);
    	        }
    		} else if (action.equals(ACTION_SCRIPT)) {
    			if (model.getDatabaseSchemaVersion()>=DataAccess.schemaVersion4)
    				createToolButton(null, "customsetup.jpg", "Resource List", ACTION_SCRIPT, this);
    		} else if (action.equals(Constants.ACTION_START)) {
    			if (model.getDatabaseSchemaVersion()>=DataAccess.schemaVersion4)
    	            createToolButton(null, "notice.gif", "Send external message to engine", Constants.ACTION_START,this);
    		} else if (action.equals(Constants.ACTION_NEW_ACTIVITY_IMPL)) {
    			createToolButton(null, "new-activity.gif", "Activity Implementor", Constants.ACTION_NEW_ACTIVITY_IMPL, this);
    		} else if (action.equals(Constants.EXTERNAL_EVENT_BUTTON)) {
    			createToolButton(null,"event.gif","External Event Handlers",Constants.EXTERNAL_EVENT_BUTTON,this);
    		}
    	}
    }

	public boolean lock_unlock_ruleset(RuleSetVO ruleset, boolean lock) {
		try {
	    	if (ruleset.getNextVersion()!=null) {
	    		showError("You can only edit the latest version");
	    		return false;
	    	}
			boolean changed;
			if (!lock) {
				if (getConfirmation("You will lose your changes by unlock; are you sure?")) {
					String errmsg = frame.dao.lockUnlockRuleSet(ruleset.getId(), frame.getCuid(), false);
					if (errmsg!=null) {
						changed = false;
						this.showError(errmsg);
					} else changed = true;
				} else changed = false;
			} else {
				String errmsg = frame.dao.lockUnlockRuleSet(ruleset.getId(), frame.getCuid(), true);
				if (errmsg!=null) {
					changed = false;
					this.showError(errmsg);
				} else changed = true;
    		}
			if (changed) {
				RuleSetVO loaded = frame.dao.getRuleSet(ruleset.getId());
                ruleset.setRuleSet(loaded.getRuleSet());
                ruleset.setModifyingUser(loaded.getModifyingUser());
                ruleset.setModifyDate(loaded.getModifyDate());
			}
			return changed;
		} catch (Exception e) {
			this.showError("Failed to edit/unedit the resource", e);
			e.printStackTrace();
			frame.dao.renewServer();
			return false;
		}
	}

	/**
	 *
	 * @param ruleset
	 * @param originalName
	 * @return 0 - saved, -1 - cancelled or erred, >0 - new version ID
	 */
	public long save_resource(RuleSetVO ruleset, String newName) {
		boolean isNew = ruleset.getId()<=0;
		boolean nameChanged = !isNew && !ruleset.getName().equals(newName);
		if (isNew || nameChanged) {
			if (model.findRuleSet(newName,ruleset.getLanguage())!=null) {
	    		showError(isNew?"The name already exists":"The new name is already used");
	    		return -1L;
			}
			ruleset.setName(newName);
		}
		boolean keeplock;
		boolean asNewVersion;
		boolean cancel;
		if (isNew) {
			String[] choices = {"Save", "Cancel", "Save & Edit"};
			int option = frame.getOptionPane().choose(this, "Save Resource", choices);
			keeplock = option==2;
			asNewVersion = false;
			cancel = option==1;
		} else if (nameChanged) {
			String[] choices = {"Save as New Version", "Cancel"};
			int option = frame.getOptionPane().choose(this, "Save Resource", choices);
			keeplock = false;
			asNewVersion = false;
			cancel = option==1;
		} else {
			String[] choices = {"Save", "Cancel", "Save & Edit", "Save as New Version"};
			int option = frame.getOptionPane().choose(this, "Save Resource", choices);
			keeplock = option==2;
			asNewVersion = option==3;
			cancel = option==1;
		}
		if (cancel) return -1L;
		if (isNew||asNewVersion) {
			// why below???
		}
		Long id = save_resource_sub(ruleset, isNew, asNewVersion, keeplock);
		if (id!=null) {
			return (asNewVersion||isNew)?ruleset.getId().longValue():0L;
		} else {
			return -1L;
        }
	}

	public Long save_resource_sub(RuleSetVO ruleset,
			boolean isNew, boolean asNewVersion, boolean keeplock) {
        try {
        	if (asNewVersion) {
        		RuleSetVO newRuleSet = new RuleSetVO();
        		newRuleSet.setId(-1L);
        		newRuleSet.setLanguage(ruleset.getLanguage());
        		newRuleSet.setVersion(ruleset.getVersion()+1);
        		newRuleSet.setName(ruleset.getName());
        		newRuleSet.setRuleSet(ruleset.getRuleSet());
	    		// set old ruleset as if it is not loaded
	    		ruleset.setRuleSet(null);
	    		ruleset.setModifyingUser(null); // not saved to database, but should be ok
	    		ruleset = newRuleSet;
        	}
    		ruleset.setModifyingUser(keeplock?frame.getCuid():null);
			Long id = frame.dao.saveRuleSet(ruleset);
			if (ruleset.getId()<=0L) ruleset.setId(id);
			if (serverIsRunning(true)) frame.dao.refreshServerCaches("RuleSetCache");
			return id;
		} catch (Exception ex) {
			ex.printStackTrace();
            showError(this, "Save resource failed", ex);
            return null;
		}
	}

	public boolean canleave() {
		return true;
	}

    public boolean serverIsRunning(boolean checkonly) {
    	boolean live;
    	Server current_server = frame.dao.getCurrentServer();
    	if (current_server.getServerUrl()!=null) {
    		live = frame.dao.ping();
    	} else if (!checkonly) {
    		String server_url = frame.getOptionPane().getInput(this,
    				"No server url is known.\nPlease specify an engine URL (iiop)\n"
    				+ "or MDW Web URL (http)");
    		if (server_url!=null) {
    			current_server.setServerUrl(server_url);
    			live = frame.dao.ping();
    		} else live = false;
    	} else live = false;
		if (!live && !checkonly) showError("This requires server running but it is not");
		return live;
    }

    public boolean verifyRoleForSite(String role) {
    	if (model.getDatabaseSchemaVersion()>=DataAccess.schemaVersion52) {
    		if (!model.userHasRole(UserGroupVO.SITE_ADMIN_GROUP,role)) {
    			showError("You need to be in " +  UserGroupVO.SITE_ADMIN_GROUP
        				+ " with " + role + " role to perform this function");
    			return false;
    		} else return true;
    	} else {
    		if (!model.userHasRole(UserGroupVO.COMMON_GROUP,role)) {
    			showError("You need to have " + role
        				+ " role to perform this function");
    			return false;
    		} else return true;
    	}
    }

}
