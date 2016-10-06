/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.model.value.user;

import java.util.HashMap;
import java.util.Map;

import com.centurylink.mdw.auth.OAuthAccessToken;
import com.centurylink.mdw.common.translator.SelfSerializable;
import com.centurylink.mdw.model.data.task.TaskAction;

/**
 * Contains the information provided by the Portal.
 */
public class AuthenticatedUser extends UserVO implements SelfSerializable
{
  public static final String NOTIFICATION_PREF = "notificationPref";

  public long getUserId()
  {
    return getId().longValue();
  }

  public AuthenticatedUser()
  {
  }

  /**
   * Constructor for an ad-hoc user.
   */
  public AuthenticatedUser(String cuid)
  {
    setId(0L);
    setCuid(cuid);
    setWorkgroups(new UserGroupVO[0]);
    setAllowableActions(new TaskAction[0]);
    setAttributes(new HashMap<String,String>());
  }

  public String getNotificationPreference() { return getAttribute(NOTIFICATION_PREF); }
  public void setNotificationPreference(String s) { setAttribute(NOTIFICATION_PREF, s); }

  public static String NOTIFICATION_OPTION_EMAIL = "E-Mail";
  public static String NOTIFICATION_OPTION_PHONE = "Phone";
  public static String NOTIFICATION_OPTION_TEXT_MESSAGE = "Text Message";
  public static String NOTIFICATION_OPTION_POSTAL_MAIL = "Postal Mail";
  public static String[] NOTIFICATION_OPTIONS = { NOTIFICATION_OPTION_EMAIL, NOTIFICATION_OPTION_PHONE, NOTIFICATION_OPTION_TEXT_MESSAGE, NOTIFICATION_OPTION_POSTAL_MAIL };

  public boolean emailOptIn;
  public boolean isEmailOptIn() { return emailOptIn; }
  public void setEmailOptIn(boolean optIn) { this.emailOptIn = optIn; }

  private TaskAction[] _allowableActions;
  public TaskAction[] getAllowableActions() { return _allowableActions; }
  public void setAllowableActions(TaskAction[] actions) { _allowableActions = actions; }

  public boolean isHasWorkgroups()
  {
    return getWorkgroups() != null && getWorkgroups().length > 0 ;
  }

  public boolean isHasWorkgroupsOtherThanCommon()
  {
    if(getWorkgroups() != null)
    {
      String[] groupNames = getWorkgroupNames();
      if (groupNames.length == 1 && groupNames[0].equals(UserGroupVO.COMMON_GROUP))
      {
        return false;
      }
      return true;
    }
    return false;
  }

  /**
   * Whether the user belongs to the specified MDW role.
   */
  public boolean isInRoleForAnyGroup(String role)
  {
    if (getWorkgroups() == null)
      return false;
    // special handling for "Site Admin" as a role
    if (UserGroupVO.SITE_ADMIN_GROUP.equals(role))
      return isInGroup(UserGroupVO.SITE_ADMIN_GROUP);
    for (UserGroupVO group : getWorkgroups())
    {
      if (group.hasRole(role))
        return true;
    }
    return false;
  }

  public boolean isInRole(String group, String role)
  {
    return super.hasRole(group, role);
    // potential todo - this may need to check for inherited roles
  }

  public boolean isInGroup(String group)
  {
    return super.belongsToGroup(group);
  }

  /**
   * @return the names of all the user's workgroups
   */
  public String[] getWorkgroupNames()
  {
    String[] names = new String[getWorkgroups().length];
    for (int i = 0; i < getWorkgroups().length; i++)
    {
      names[i] = getWorkgroups()[i].getName();
    }
    return names;
  }

  public String[] getGroupNameAndRoles() {
	  String[] nameAndRoles = new String[getWorkgroups().length];
	  for (int i = 0; i < getWorkgroups().length; i++)
	  {
		nameAndRoles[i] = getWorkgroups()[i].getNameAndRolesAsString();
	  }
	  return nameAndRoles;
  }

  /**
   * Returns a string concatenation of the user's workgroups.
   * @return
   */
  public String getWorkgroupsAsString()
  {
    StringBuffer sb = new StringBuffer(512);
    for (int i = 0; i < getWorkgroups().length; i++)
    {
      UserGroupVO group = getWorkgroups()[i];
      if (!UserGroupVO.COMMON_GROUP.equals(group.getName()))
      {
        sb.append(group.getName());
        if (i != getWorkgroups().length - 1)
          sb.append(", ");
      }
    }
    return sb.toString();
  }

  public boolean isUserBelongsToAdminGrp()
  {
    return super.belongsToGroup(UserGroupVO.SITE_ADMIN_GROUP);
  }

  public String toString()
  {
    String ret = "cuid: " + getCuid() + ",\n"
      + "firstName: " + getFirstName() + ",\n"
      + "lastName: " + getLastName() + ",\n"
      + "emailAddress: " + getEmail() + ",\n"
      + "phoneNumber: " + getPhone() + ",\n"
      + "notificationPrefrence: " + getNotificationPreference() + ",\n";

    ret += "allowable actions:\n";
    if (_allowableActions != null)
    {
      for (int i = 0; i < _allowableActions.length; i++)
        ret += "  " + _allowableActions[i].getTaskActionName() + ",\n";
    }

    ret += "workgroups\n";
    if (getWorkgroups() != null)
    {
      for (int i = 0; i < getWorkgroups().length; i++)
        ret += "  " + getWorkgroups()[i].getName() + ",\n";
    }
    return ret;
  }

  /**
   * Convenience method for checking user roles in expression language statements.
   * @return a Map whose key values are the roles a user belongs to.
   *
   * Note: checks whether a user has the specified role for ANY groups.
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public Map getRoles()
  {
    Map roles = new HashMap();

    if (getWorkgroups() != null) {
    	for (UserGroupVO group : getWorkgroups()) {
    		if (group.getRoles()!=null) {
    			for (String role : group.getRoles()) {
    		        roles.put(role, new Boolean(true));
    			}
    		}
    		// special treatment of Site Admin like a role
    		if (group.getName().equals(UserGroupVO.SITE_ADMIN_GROUP))
    		  roles.put(UserGroupVO.SITE_ADMIN_GROUP, new Boolean(true));
    	}
    }
    return roles;
  }

  public boolean isLoaded()
  {
    return getCuid() != null && getName() != null;
  }

  public boolean equals(Object other)
  {
    if (!(other instanceof AuthenticatedUser))
      return false;

    AuthenticatedUser otherUser = (AuthenticatedUser) other;
    if (otherUser.getCuid() == null)
        return getCuid() == null;
    return otherUser.getCuid().equals(getCuid());
  }

  	/**
  	 * Currently this method is only used for testing SelfSerializable.
  	 * Need to complete it if it is really needed
  	 */
	@Override
	public void fromString(String str) {
		String[] fields = str.split(",[\\n\\r\\s]*");
		for (String field : fields) {
			int k = field.indexOf(":");
			if (k>0) {
				String name = field.substring(0,k).trim();
				String value = field.substring(k+1).trim();
				if (name.equals("cuid")) {
					setCuid(value);
				}
			}
		}

	}

	// these are only used for the samples demo
    private String _firstName;
    public String getFirstName() { return _firstName; }
    public void setFirstName(String s) { _firstName = s; }
    private String _lastName;
    public String getLastName() { return _lastName; }
    public void setLastName(String s) { _lastName = s; }


    private OAuthAccessToken oAuthAccessToken;
    public OAuthAccessToken getOAuthAccessToken() { return oAuthAccessToken; }
    public void setOAuthAccessToken(OAuthAccessToken token) { this.oAuthAccessToken = token; }
}
