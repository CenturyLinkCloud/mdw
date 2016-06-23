/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.ui.workgroups;

import java.util.ArrayList;
import java.util.List;

import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.lang.StringUtils;
import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.richfaces.component.UITree;
import org.richfaces.component.state.TreeStateAdvisor;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;

import com.centurylink.mdw.common.exception.CachingException;
import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;
import com.centurylink.mdw.model.value.attribute.RuleSetVO;
import com.centurylink.mdw.model.value.task.TaskVO;
import com.centurylink.mdw.model.value.user.AuthenticatedUser;
import com.centurylink.mdw.model.value.user.UserGroupVO;
import com.centurylink.mdw.model.value.user.UserRoleVO;
import com.centurylink.mdw.model.value.user.UserVO;
import com.centurylink.mdw.services.dao.user.cache.UserGroupCache;
import com.centurylink.mdw.taskmgr.ui.layout.ViewUI;
import com.centurylink.mdw.taskmgr.ui.list.ListManager;
import com.centurylink.mdw.taskmgr.ui.roles.RoleItem;
import com.centurylink.mdw.taskmgr.ui.roles.RolesActionController;
import com.centurylink.mdw.taskmgr.ui.tasks.TaskItem;
import com.centurylink.mdw.taskmgr.ui.tasks.template.TaskTemplateActionController;
import com.centurylink.mdw.taskmgr.ui.user.UserActionController;
import com.centurylink.mdw.taskmgr.ui.user.UserItem;
import com.centurylink.mdw.taskmgr.ui.user.Users;
import com.centurylink.mdw.web.jsf.FacesVariableUtil;
import com.centurylink.mdw.web.ui.UIException;

public class WorkgroupTree implements TreeStateAdvisor
{
  public static final String TREE_BEAN = "workgroupTree";
  public static final String TYPE_USER = "User";
  public static final String TYPE_GROUP = "Group";
  public static final String TYPE_ROLE = "Role";
  public static final String TYPE_TASK = "Task";

  private static StandardLogger logger = LoggerUtil.getStandardLogger();

  private TreeNode<Object> rootNode = null;
  private String nodeTitle;
  private TaskVO taskDetailTemplate;

  public static WorkgroupTree getInstance()
  {
    return (WorkgroupTree)FacesVariableUtil.getValue(TREE_BEAN);
  }

  public UserGroupVO getGroup(String groupName)
  {
    if (Workgroups.getActiveGroups() != null)
    {
      for (UserGroupVO group : Workgroups.getActiveGroups())
      {
        if (group.getName().equals(groupName))
          return group;
      }
    }
    return null;
  }

  public String refresh()
  {
    clearSelection();
    clearDisplayList();
    UserGroupCache.clear();
    loadTree();
    return null;
  }

  public void clearSelection()
  {
    scopedUser = null;
    user = null;
    FacesVariableUtil.removeValue(UserItem.ITEM_BEAN);
    group = null;
    FacesVariableUtil.removeValue(WorkgroupItem.ITEM_BEAN);
    role = null;
    FacesVariableUtil.removeValue(RoleItem.ITEM_BEAN);
    task = null;
    FacesVariableUtil.removeValue(TaskItem.ITEM_BEAN);
    asset = null;
    FacesVariableUtil.removeValue(AssetsItem.ITEM_BEAN);

    addMode = false;

    deletePending = false;
    showImport  = false;
    showExport  = false;

    ListManager.getInstance().clearCurrentRows();
  }

  public boolean isShowCommonGroup()
  {
    try
    {
      return ViewUI.getInstance().isShowCommonGroup();
    }
    catch(UIException ex)
    {
      logger.severeException(ex.getMessage(), ex);
      return false;
    }
  }

  public void loadTree()
  {
    rootNode = new TreeNodeImpl<Object>();

    try
    {
      if (isShowCommonGroup())
      {
        // add the common group
        UserGroupVO commonGroup = new UserGroupVO(UserGroupVO.COMMON_GROUP_ID, UserGroupVO.COMMON_GROUP, "All users belong to this group");
        TreeNodeImpl<Object> commonGroupNode = new TreeNodeImpl<Object>();
        commonGroupNode.setData(commonGroup);
        rootNode.addChild(commonGroup.getId(), commonGroupNode);
        addChildren(commonGroupNode);
      }

      AuthenticatedUser authUser = FacesVariableUtil.getCurrentUser();

      for (UserGroupVO group : Workgroups.getActiveGroups())
      {
        if (authUser.isInGroup(UserGroupVO.SITE_ADMIN_GROUP) || authUser.isInGroup(group.getName()))
        {
          TreeNodeImpl<Object> groupNode = new TreeNodeImpl<Object>();
          groupNode.setData(group);
          if (group.getParentGroup() == null)
          {
            rootNode.addChild(group.getId(), groupNode);
            addChildren(groupNode);
          }
        }
      }
    }
    catch (CachingException ex)
    {
      logger.severeException(ex.getMessage(), ex);
    }

    addMode = false;
  }

  private void addChildren(TreeNodeImpl<Object> groupNode) throws CachingException
  {
    UserGroupVO thisGroup = (UserGroupVO) groupNode.getData();
    if (!thisGroup.getId().equals(UserGroupVO.COMMON_GROUP_ID))
    {
      for (UserGroupVO group : Workgroups.getActiveGroups())
      {
        if (thisGroup.getName().equals(group.getParentGroup()))
        {
          TreeNodeImpl<Object> childGroupNode = new TreeNodeImpl<Object>();
          childGroupNode.setData(group);
          groupNode.addChild(group.getId(), childGroupNode);
          addChildren(childGroupNode);
        }
      }
    }
    // add users
    UserVO[] users = Users.getUsersInWorkgroup(thisGroup.getName());
    for (UserVO user : users)
    {
      TreeNodeImpl<Object> userNode = new TreeNodeImpl<Object>();
      userNode.setData(new ScopedUser(user, thisGroup));
      groupNode.addChild(user.getId(), userNode);
    }
  }

  private UserGroupVO group;
  public UserGroupVO getGroup() { return group; }
  public void setGroup(UserGroupVO group) throws UIException
  {
    clearSelection();
    clearDisplayList();
    this.group = group;
    FacesVariableUtil.setValue(WorkgroupItem.ITEM_BEAN, new WorkgroupItem(this.group));
  }

  private ScopedUser scopedUser;
  public ScopedUser getScopedUser() { return scopedUser; }
  public void setScopedUser(ScopedUser scopedUser) throws UIException
  {
    clearSelection();
    clearDisplayList();
    this.scopedUser = scopedUser;
    UserItem userItem = new UserItem(scopedUser.userVO);
    userItem.setParentGroup(scopedUser.parentGroup);
    FacesVariableUtil.setValue(UserItem.ITEM_BEAN, userItem);
  }

  // user selection (not in tree)
  private UserVO user;
  public UserVO getUser() { return user; }
  public void setUser(UserVO user)
  {
    clearSelection();
    clearDisplayList();
    this.user = user;
    FacesVariableUtil.setValue(UserItem.ITEM_BEAN, new UserItem(user));
  }

  // role selection (not in tree)
  private UserRoleVO role;
  public UserRoleVO getRole() { return role; }
  public void setRole(UserRoleVO role)
  {
    clearSelection();
    clearDisplayList();
    this.role = role;
    FacesVariableUtil.setValue(RoleItem.ITEM_BEAN, new RoleItem(role));
  }

	// task selection (not in tree)
	private TaskVO task;

	public TaskVO getTask() {
		if (taskDetailTemplate != null){
			setTask(taskDetailTemplate);
			taskDetailTemplate = null;
		}
		return task;
	}

	public void setTask(TaskVO task) {
		clearSelection();
		clearDisplayList();
		this.task = task;
		FacesVariableUtil.setValue(TaskItem.ITEM_BEAN, new TaskItem(task));
	}

	public RuleSetVO asset;

    public RuleSetVO getAsset() {
        if (taskDetailTemplate != null){
            setTask(taskDetailTemplate);
            taskDetailTemplate = null;
        }
        return asset;
    }

    public void setAsset(RuleSetVO asset) {
        clearSelection();
        clearDisplayList();
        this.asset = asset;
        FacesVariableUtil.setValue(AssetsItem.ITEM_BEAN, new AssetsItem(asset));
    }


	public TaskVO getTaskDetailTemplate() {
		return taskDetailTemplate;
	}

	/**
	 * Set this taskVO from taskDetail page when Task Template link is clicked
	 * @param taskDetailTemplate
	 */
	public void setTaskDetailTemplate(TaskVO taskDetailTemplate) {
		this.taskDetailTemplate = taskDetailTemplate;
	}

  public TreeNode<Object> getTreeNode()
  {
    if (rootNode == null)
    {
      loadTree();
    }

    return rootNode;
  }

  public String getNodeTitle()
  {
    return nodeTitle;
  }

  public void setNodeTitle(String nodeTitle)
  {
    this.nodeTitle = nodeTitle;
  }


  @Override
  public Boolean adviseNodeOpened(UITree uiTree)
  {
    if (uiTree.getModelTreeNode().getData() instanceof UserGroupVO)
    {
      UserGroupVO groupVO = (UserGroupVO) uiTree.getModelTreeNode().getData();
      if (scopedUser != null)
      {
        UserGroupVO parentGroup = scopedUser.parentGroup;
        while (parentGroup != null)
        {
          if (parentGroup.getName().equals(groupVO.getName()))
            return true;
          parentGroup = getGroup(parentGroup.getParentGroup());
        }
      }
    }
    return null;
  }

  @Override
  public Boolean adviseNodeSelected(UITree uiTree)
  {
    if (uiTree.getModelTreeNode().getData() instanceof ScopedUser)
    {
      ScopedUser sUser = (ScopedUser) uiTree.getModelTreeNode().getData();
      if (sUser.userVO.getCuid().equals((String)FacesVariableUtil.getRequestParamValue("mdwUser")))
      {
        try
        {
          setScopedUser(sUser);
          return true;
        }
        catch (Exception ex)
        {
          logger.severeException(ex.getMessage(), ex);
        }
      }
      if (scopedUser != null && scopedUser.userVO.getCuid().equals(sUser.userVO.getCuid()))
      {
        UserGroupVO parentGroup = scopedUser.parentGroup;
        UserGroupVO groupVO = (UserGroupVO) uiTree.getModelTreeNode().getParent().getData();
        if (parentGroup != null && parentGroup.getName().equals(groupVO.getName()))
          return true;
      }
    }
    if (uiTree.getModelTreeNode().getData() instanceof UserGroupVO)
    {
      UserGroupVO groupVO = (UserGroupVO) uiTree.getModelTreeNode().getData();
      if (groupVO.getName().equals((String)FacesVariableUtil.getRequestParamValue("mdwGroup")))
      {
        try
        {
          setGroup(groupVO);
          return true;
        }
        catch (UIException ex)
        {
          logger.severeException(ex.getMessage(), ex);
        }
      }
      if (group != null && groupVO.getName().equals(group.getName()))
        return true;
    }
    return null;
  }

  public boolean isHasSelection()
  {
    return scopedUser != null || group != null || user != null || role != null || task != null;
  }

  public boolean isUserAllowedToEditSelection()
  {
    if (!isHasSelection())
      return false;
    AuthenticatedUser authUser = FacesVariableUtil.getCurrentUser();
    if (authUser.isInRole(UserGroupVO.SITE_ADMIN_GROUP, UserRoleVO.USER_ADMIN))
      return true;

    if (scopedUser != null)
      return authUser.isInRole(scopedUser.getParentGroup().getName(), UserRoleVO.USER_ADMIN);
    else if (group != null)
      return authUser.isInRole(UserGroupVO.SITE_ADMIN_GROUP, UserRoleVO.USER_ADMIN);

    return false;
  }

  public Object getSelection()
  {
    if (scopedUser != null)
      return scopedUser;
    else if (group != null)
      return group;
    else if (user != null)
      return user;
    else if (role != null)
      return role;
    else if (task != null)
        return task;
    else if (asset != null)
      return asset;
    else
      return null;
  }

  public String getSelectionTitle()
  {
    if (getSelection() instanceof ScopedUser)
      return "User";
    else if (getSelection() instanceof UserVO)
      return "User";
    else if (getSelection() instanceof UserGroupVO)
      return "Group";
    else if (getSelection() instanceof UserRoleVO)
      return "Role";
    else if (getSelection() instanceof TaskVO)
        return "Task";
    else if (getSelection() instanceof RuleSetVO)
      return "Asset";

    else
      return null;
  }

  public String getSelectedTitle()
  {
    if (displayList != null)
      return displayList;
    else if (getSelectionTitle() == null)
      return null;
    else
      return "Selected " + getSelectionTitle();
  }

  public String getSelectionName()
  {
    if (getSelection() instanceof ScopedUser)
      return ((ScopedUser)getSelection()).getName();
    else if (getSelection() instanceof UserVO)
      return ((UserVO)getSelection()).getName();
    else if (getSelection() instanceof UserGroupVO)
      return ((UserGroupVO)getSelection()).getName();
    else if (getSelection() instanceof UserRoleVO)
      return ((UserRoleVO)getSelection()).getName();
    else if (getSelection() instanceof TaskVO)
        return ((TaskVO)getSelection()).getTaskName();
    else if (getSelection() instanceof RuleSetVO)
      return ((RuleSetVO)getSelection()).getName();
    else
      return null;
  }

  private String addType = TYPE_USER;
  public String getAddType() { return addType; }
  public void setAddType(String type)
  {
    this.addType = type;
  }

  public List<SelectItem> getAddTypeSelectItems()
  {
    List<SelectItem> selectItems = new ArrayList<SelectItem>();
    AuthenticatedUser authUser = FacesVariableUtil.getCurrentUser();
    if (authUser.isInRoleForAnyGroup(UserRoleVO.USER_ADMIN))
      selectItems.add(new SelectItem(TYPE_USER));
    if (authUser.isInRole(UserGroupVO.SITE_ADMIN_GROUP, UserRoleVO.USER_ADMIN))
    {
      selectItems.add(new SelectItem(TYPE_GROUP));
      selectItems.add(new SelectItem(TYPE_ROLE));
      selectItems.add(new SelectItem(TYPE_TASK));
    }
    return selectItems;
  }

  // action method
  public String setUserMode() throws UIException
  {
    if (scopedUser != null)
    {
      user = scopedUser.userVO;
      scopedUser = null;
    }
    return null;
  }

  private boolean showImport;
  public boolean isShowImport() { return showImport; }
  public void setShowImport(boolean showImport) {
    this.showImport=showImport;
    }
  public void setShowImport(Object toDelete){
    String saveDisplayList = displayList;
    setAsset((RuleSetVO)toDelete);
    displayList = saveDisplayList;
    this.showImport=true;
  }

  private boolean showExport;
  public boolean isShowExport() { return showExport; }
  public void setShowExport(boolean showExport) {
    this.showExport=showExport;
    }
  public void setShowExport(Object toDelete){
    String saveDisplayList = displayList;
    setAsset((RuleSetVO)toDelete);
    displayList = saveDisplayList;
    this.showExport=true;
  }




  private UploadedFile _importFile;
  public UploadedFile getImportFile() { return _importFile; }
  public void setImportFile(UploadedFile importFile)
  {
    this._importFile = importFile;
  }

  private String comments;
  public String getComments() { return comments; }
  public void setComments(String comments)
  {
    this.comments = comments;
  }

  private boolean deletePending;
  public boolean isDeletePending() { return deletePending; }
  public void setDeletePending(Object toDelete) throws UIException
  {
    String saveDisplayList = displayList;

    if (toDelete instanceof UserVO)
      setUser((UserVO)toDelete);
    else if (toDelete instanceof UserGroupVO)
      setGroup((UserGroupVO)toDelete);
    else if (toDelete instanceof UserRoleVO)
      setRole((UserRoleVO)toDelete);
    else if (toDelete instanceof TaskVO)
      setTask((TaskVO)toDelete);

    displayList = saveDisplayList;
    deletePending = true;
  }

  private boolean addMode = false;
  public boolean isAddMode() { return addMode; }

  // action method
  public String setAddMode() throws UIException
  {
    clearSelection();
    clearDisplayList();
    addMode = true;
    if (addType.equals(TYPE_USER))
    {
      user = new UserVO();
      user.setGroups(new ArrayList<UserGroupVO>());
      FacesVariableUtil.setValue(UserItem.ITEM_BEAN, new UserItem(user));
    }
    else if (addType.equals(TYPE_GROUP))
    {
      group = new UserGroupVO(null, null, null);
      FacesVariableUtil.setValue(WorkgroupItem.ITEM_BEAN, new WorkgroupItem(group));
    }
    else if (addType.equals(TYPE_ROLE))
    {
      role = new UserRoleVO();
      FacesVariableUtil.setValue(RoleItem.ITEM_BEAN, new RoleItem(role));
    }
    else if (addType.equals(TYPE_TASK))
    {
      task = new TaskVO();
      FacesVariableUtil.setValue(TaskItem.ITEM_BEAN, new TaskItem(task));
    }

    return null;
  }

  public void cancelAddMode(ActionEvent event)
  {
    clearSelection();
    // clear component values
    FacesVariableUtil.clearFormComponents(event.getComponent());
  }

  public List<SelectItem> getParentGroupSelectItems()
  {
    List<SelectItem> items = new ArrayList<SelectItem>();
    items.add(new SelectItem(""));
    for (UserGroupVO group : Workgroups.getActiveGroups())
    {
      if (!group.getName().equals(this.group.getName()))
        items.add(new SelectItem(group.getName()));
    }
    return items;
  }

  public String deleteSelection()
  {
    String outcome = null;
    try
    {
      if (getSelection() instanceof ScopedUser || getSelection() instanceof UserVO)
      {
        outcome = ((UserActionController)FacesVariableUtil.getValue(UserActionController.CONTROLLER_BEAN)).deleteUser();
      }
      else if (getSelection() instanceof UserGroupVO)
      {
        outcome = ((WorkgroupActionController)FacesVariableUtil.getValue(WorkgroupActionController.CONTROLLER_BEAN)).deleteGroup();
      }
      else if (getSelection() instanceof UserRoleVO)
      {
        outcome = ((RolesActionController)FacesVariableUtil.getValue(RolesActionController.CONTROLLER_BEAN)).deleteRole();
      }
      else if (getSelection() instanceof TaskVO)
      {
        outcome = ((TaskTemplateActionController)FacesVariableUtil.getValue(TaskTemplateActionController.CONTROLLER_BEAN)).deleteTask();
      }

      FacesVariableUtil.addMessage(getSelectionTitle() + " '" + getSelectionName() + "' deleted");
      clearSelection();
      loadTree();
    }
    catch (UIException ex)
    {
      logger.severeException(ex.getMessage(), ex);
      FacesVariableUtil.addMessage(ex);
    }
    return outcome;
  }


  public String doImport(){
    showImport  = false;
    String outcome = null;

    try{
      if(getImportFile()!=null && StringUtils.isNotEmpty(getImportFile().getName())){
       outcome = ((AssetsActionController)FacesVariableUtil.getValue(AssetsActionController.CONTROLLER_BEAN)).updateAsset(getImportFile(),getComments());
       String fileName = getImportFile().getName();

       FacesVariableUtil.addMessage("Asset:" + " '" + fileName.substring(fileName.lastIndexOf("\\")+1) + "' Updated");
      }
    }catch (UIException ex)
    {
      logger.severeException(ex.getMessage(), ex);
      FacesVariableUtil.addMessage(ex);
    }

    return outcome;
  }

  public String createUser()
  {
    String outcome = null;
    try
    {
      outcome = ((UserActionController)FacesVariableUtil.getValue(UserActionController.CONTROLLER_BEAN)).saveUserWithGroups();
      FacesVariableUtil.addMessage("User created");
      loadTree();
      // select user?
    }
    catch (UIException ex)
    {
      logger.severeException(ex.getMessage(), ex);
      FacesVariableUtil.addMessage(ex);
    }
    return outcome;
  }

  public String saveUser()
  {
    String outcome = null;
    try
    {
      UserActionController userController = (UserActionController)FacesVariableUtil.getValue(UserActionController.CONTROLLER_BEAN);
      if (user != null)
        outcome = userController.saveUserWithGroups();
      else if (scopedUser != null)
        outcome = userController.saveUserWithGroupRoles();

      FacesVariableUtil.addMessage("User saved");
      loadTree();
    }
    catch (UIException ex)
    {
      logger.severeException(ex.getMessage(), ex);
      FacesVariableUtil.addMessage(ex);
    }
    return outcome;
  }

  public String createGroup()
  {
    String outcome = null;
    try
    {
      outcome = ((WorkgroupActionController)FacesVariableUtil.getValue(WorkgroupActionController.CONTROLLER_BEAN)).saveGroupWithUsers();
      FacesVariableUtil.addMessage("Group created");
      loadTree();
      group = ((WorkgroupItem)FacesVariableUtil.getValue(WorkgroupItem.ITEM_BEAN)).getUserGroup();
      addType = TYPE_USER; // reset to default
      return outcome;
    }
    catch (UIException ex)
    {
      logger.severeException(ex.getMessage(), ex);
      FacesVariableUtil.addMessage(ex);
    }
    return outcome;
  }

  public String saveGroup()
  {
    String outcome = null;
    try
    {
      outcome = ((WorkgroupActionController)FacesVariableUtil.getValue(WorkgroupActionController.CONTROLLER_BEAN)).saveGroupWithUsers();
      FacesVariableUtil.addMessage("Group saved");
      loadTree();
      return outcome;
    }
    catch (UIException ex)
    {
      logger.severeException(ex.getMessage(), ex);
      FacesVariableUtil.addMessage(ex);
    }
    return outcome;
  }

  public String createRole()
  {
    String outcome = null;
    try
    {
      outcome = ((RolesActionController)FacesVariableUtil.getValue(RolesActionController.CONTROLLER_BEAN)).saveRole();
      FacesVariableUtil.addMessage("Role created");
      loadTree();
      role = ((RoleItem)FacesVariableUtil.getValue(RoleItem.ITEM_BEAN)).getRole();
      addType = TYPE_USER; // reset to default
      return outcome;
    }
    catch (UIException ex)
    {
      logger.severeException(ex.getMessage(), ex);
      FacesVariableUtil.addMessage(ex);
    }
    return outcome;
  }

  public String saveRole()
  {
    String outcome = null;
    try
    {
      outcome = ((RolesActionController)FacesVariableUtil.getValue(RolesActionController.CONTROLLER_BEAN)).saveRole();
      FacesVariableUtil.addMessage("Role saved");
      loadTree();
      return outcome;
    }
    catch (UIException ex)
    {
      logger.severeException(ex.getMessage(), ex);
      FacesVariableUtil.addMessage(ex);
    }
    return outcome;
  }

  public String createTask()
  {
    String outcome = null;
    try
    {
      outcome = ((TaskTemplateActionController)FacesVariableUtil.getValue(TaskTemplateActionController.CONTROLLER_BEAN)).saveTask();
      FacesVariableUtil.addMessage("Task created");
      loadTree();
      task = ((TaskItem)FacesVariableUtil.getValue(TaskItem.ITEM_BEAN)).getTask();
      addType = TYPE_USER; // reset to default
      return outcome;
    }
    catch (UIException ex)
    {
      logger.severeException(ex.getMessage(), ex);
      FacesVariableUtil.addMessage(ex);
    }
    return outcome;
  }

  public String saveTask()
  {
    String outcome = null;
    try
    {
      outcome = ((TaskTemplateActionController)FacesVariableUtil.getValue(TaskTemplateActionController.CONTROLLER_BEAN)).saveTask();
      FacesVariableUtil.addMessage("Task saved");
      return outcome;
    }
    catch (UIException ex)
    {
      logger.severeException(ex.getMessage(), ex);
      FacesVariableUtil.addMessage(ex);
    }
    return outcome;
  }

  private static final String DISPLAY_LIST_USERS = "Users";
  private static final String DISPLAY_LIST_ASSETS ="Assets";
  private static final String DISPLAY_LIST_GROUPS = "Groups";
  private static final String DISPLAY_LIST_ROLES = "Roles";
  private static final String DISPLAY_LIST_TASKS = "Tasks";
  private static final String DISPLAY_LIST_HISTORY = "History";

  private String displayList;
  public String getDisplayList() { return displayList; }
  public void clearDisplayList() { displayList = null; }


  public String assetList()
  {
    clearSelection();
    displayList = DISPLAY_LIST_ASSETS;
    return null;
  }

  public String userList()
  {
    clearSelection();
    displayList = DISPLAY_LIST_USERS;
    return null;
  }

  public String groupList()
  {
    clearSelection();
    displayList = DISPLAY_LIST_GROUPS;
    return null;
  }

  public String roleList()
  {
    clearSelection();
    displayList = DISPLAY_LIST_ROLES;
    return null;
  }

  public String taskList()
  {
    clearSelection();
    displayList = DISPLAY_LIST_TASKS;
    return null;
  }

  public String historyList()
  {
    clearSelection();
    displayList = DISPLAY_LIST_HISTORY;
    return null;
  }

  public class ScopedUser
  {
    private UserVO userVO;
    public UserVO getUserVO() { return userVO; }

    private UserGroupVO parentGroup;
    public UserGroupVO getParentGroup() { return parentGroup; }

    ScopedUser(UserVO userVO, UserGroupVO parentGroup)
    {
      this.userVO = userVO;
      this.parentGroup = parentGroup;
    }

    public String getTitle()
    {
      return "User";
    }
    public String getName()
    {
      return userVO.getName();
    }
  }
}
