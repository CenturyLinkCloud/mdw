/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin;

public class User
{
  public User(String username)
  {
    this.username = username;
  }

  public User(String username, String password)
  {
    this.username = username;
    this.password = password;
  }

  private String username;
  public String getUsername() { return username; }

  // password only for authenticated projects (non-VCS or remote)
  private String password;
  public String getPassword() { return password; }
}
