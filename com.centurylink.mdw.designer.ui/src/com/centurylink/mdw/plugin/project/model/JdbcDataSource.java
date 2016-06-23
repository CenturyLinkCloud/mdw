/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.plugin.project.model;

import java.security.GeneralSecurityException;

import org.apache.commons.codec.binary.Base64;

import com.centurylink.mdw.plugin.PluginMessages;
import com.centurylink.mdw.common.utilities.CryptUtil;

/**
 * Represents a JDBC DataSource.
 */
public class JdbcDataSource
{
  public static final String DATASOURCE_NAME = "MDWDataSource";
  public static final String DEFAULT_DRIVER = "oracle.jdbc.OracleDriver";
  public static final String DEFAULT_JDBC_URL = "jdbc:oracle:thin:@mdwdevdb.dev.qintra.com:1594:mdwdev";
  public static final String DEFAULT_DB_USER = "mdwdemo";
  public static final String DEFAULT_DB_USER_OLD = "mdw";
  public static final String DEFAULT_DB_PASSWORD = "mdwdemo";
  public static final String DEFAULT_DB_PASSWORD_OLD = "mdw";

  /**
   * TODO: mariadb
   */
  public static final String[] JDBC_DRIVERS
    = { "oracle.jdbc.OracleDriver", "oracle.jdbc.xa.client.OracleXADataSource", "com.mysql.jdbc.Driver", "org.mariadb.jdbc.Driver" };

  private String name = DATASOURCE_NAME;
  public String getName() { return name; }
  public void setName(String s) { name = s; }

  private String driver = DEFAULT_DRIVER;
  public String getDriver() { return driver; }
  public void setDriver(String s) { driver = s; }

  private String jdbcUrl;
  public String getJdbcUrl() { return jdbcUrl; }
  public void setJdbcUrl(String url) { jdbcUrl = url; }
  public String getJdbcUrlWithCredentials()
  {
    if (isOracle())
      return getJdbcUrlWithCredentialsOracle();
    else if (isMySql() || isMariaDb())
      return getJdbcUrlWithCredentialsMySql();
    else
      return null;
  }

  public String getMariaDbUrlAsMySql()
  {
    if (jdbcUrl == null)
      return null;
    return "jdbc:mysql" + jdbcUrl.substring("jdbc:mariadb".length());
  }

  public boolean isMySql()
  {
    if (jdbcUrl == null)
      return false;
    return jdbcUrl.startsWith("jdbc:mysql");
  }
  public boolean isMariaDb()
  {
    if (jdbcUrl == null)
      return false;
    return jdbcUrl.startsWith("jdbc:mariadb");
  }
  public boolean isOracle()
  {
    if (jdbcUrl == null)
      return false;
    return jdbcUrl.startsWith("jdbc:oracle");
  }

  public String getJdbcUrlWithCredentialsMySql()
  {
    return jdbcUrl + "?user=" + dbUser + "&password=" + dbPassword;
  }

  public String getJdbcUrlWithCredentialsOracle()
  {
    int atIdx = jdbcUrl.indexOf('@');
    return jdbcUrl.substring(0, atIdx) + dbUser + "/" + dbPassword + jdbcUrl.substring(atIdx);
  }

  public boolean setJdbcUrlWithCredentials(String jdbcUrlWithCreds)
  {
    if (jdbcUrlWithCreds.startsWith("jdbc:oracle"))
      return setJdbcUrlWithCredentialsOracle(jdbcUrlWithCreds);
    else if (jdbcUrlWithCreds.startsWith("jdbc:mysql"))
      return setJdbcUrlWithCredentialsMySql(jdbcUrlWithCreds);
    else if (jdbcUrlWithCreds.startsWith("jdbc:mariadb"))
      return setJdbcUrlWithCredentialsMySql(jdbcUrlWithCreds);
    else
      return false;
  }

  public boolean setJdbcUrlWithCredentialsMySql(String jdbcUrlWithCreds)
  {
    // jdbc:mysql://localhost:3306/mdw55?user=mdw55&password=mdw55
    try
    {
      int qIdx = jdbcUrlWithCreds.indexOf('?');
      jdbcUrl = jdbcUrlWithCreds.substring(0, qIdx);
      String[] params = jdbcUrlWithCreds.substring(qIdx + 1).split("&");
      boolean foundUser = false, foundPassword = false;
      for (String param : params)
      {
        if (param.startsWith("user="))
        {
          dbUser = param.substring(5);
          foundUser = true;
        }
        else if (param.startsWith("password"))
        {
          String newDbPassword = param.substring(9);
          if (newDbPassword.length() == 32)
          {
            // encrypted password
            try
            {
              dbPassword = CryptUtil.decrypt(newDbPassword);
            }
            catch (GeneralSecurityException ex)
            {
              PluginMessages.uiError(ex, "Security Exception");
            }
          }
          else
          {
            boolean allStars = true;
            for (int i = 0; i < newDbPassword.length(); i++)
            {
              if (newDbPassword.charAt(i) != '*')
              {
                allStars = false;
                break;
              }
            }
            if (!allStars)
              dbPassword = newDbPassword;
          }
          foundPassword = true;
        }
      }
      return foundUser && foundPassword;
    }
    catch (StringIndexOutOfBoundsException ex)
    {
      return false;
    }
  }

  public boolean setJdbcUrlWithCredentialsOracle(String jdbcUrlWithCreds)
  {
    // jdbc:oracle:thin:mdw55/mdw55@localhost:1521:xe
    try
    {
      int atIdx = jdbcUrlWithCreds.indexOf('@');
      int colonIdx = jdbcUrlWithCreds.substring(0, atIdx).lastIndexOf(':');
      jdbcUrl = jdbcUrlWithCreds.substring(0, colonIdx + 1) + jdbcUrlWithCreds.substring(atIdx);
      int slashIdx = jdbcUrlWithCreds.indexOf('/');
      dbUser = jdbcUrlWithCreds.substring(colonIdx + 1, slashIdx);
      String newDbPassword = jdbcUrlWithCreds.substring(slashIdx + 1, atIdx);
      if (newDbPassword.length() == 32)
      {
        // encrypted password
        try
        {
          dbPassword = CryptUtil.decrypt(newDbPassword);
        }
        catch (GeneralSecurityException ex)
        {
          PluginMessages.uiError(ex, "Security Exception");
        }
      }
      else
      {
        boolean allStars = true;
        for (int i = 0; i < newDbPassword.length(); i++)
        {
          if (newDbPassword.charAt(i) != '*')
          {
            allStars = false;
            break;
          }
        }
        if (!allStars)
          dbPassword = newDbPassword;
      }
    }
    catch (StringIndexOutOfBoundsException ex)
    {
      return false;
    }
    return true;
  }

  public String getJdbcUrlWithMaskedCredentials()
  {
    if (isOracle())
      return getJdbcUrlWithMaskedCredentialsOracle();
    else if (isMySql() || isMariaDb())
      return getJdbcUrlWithMaskedCredentialsMySql();
    else
      return null;
  }

  public String getJdbcUrlWithMaskedCredentialsMySql()
  {
    String maskedPassword = "";
    for (int i = 0; i < dbPassword.length(); i++)
      maskedPassword += "*";

    return jdbcUrl + "?user=" + dbUser + "&password=" + maskedPassword;
  }

  public String getJdbcUrlWithMaskedCredentialsOracle()
  {
    int atIdx = jdbcUrl.indexOf('@');

    String maskedPassword = "";
    for (int i = 0; i < dbPassword.length(); i++)
      maskedPassword += "*";

    return jdbcUrl.substring(0, atIdx) + dbUser + "/" + maskedPassword + jdbcUrl.substring(atIdx);
  }

  public boolean equalsIgnoreMask(String jdbcUrl)
  {
    JdbcDataSource newDs = new JdbcDataSource();
    newDs.setJdbcUrlWithCredentials(jdbcUrl);
    if (newDs.getDbPassword() == null)
      newDs.setDbPassword("temp");  // avoid NPE
    return getJdbcUrlWithMaskedCredentials().replaceAll("\\*", "").equals(newDs.getJdbcUrlWithMaskedCredentials().replaceAll("\\*", ""));
  }

  public String getJdbcUrlWithEncryptedCredentials()
  {
    if (isOracle())
      return getJdbcUrlWithEncryptedCredentialsOracle();
    else if (isMySql() || isMariaDb())
      return getJdbcUrlWithEncryptedCredentialsMySql();
    else
      return null;
  }

  public String getJdbcUrlWithEncryptedCredentialsMySql()
  {
    String encryptedPassword = dbPassword;
    try
    {
       encryptedPassword = CryptUtil.encrypt(dbPassword);
    }
    catch (GeneralSecurityException ex)
    {
      PluginMessages.log(ex);
    }
    return jdbcUrl + "?user=" + dbUser + "&amp;password=" + encryptedPassword;
  }

  public String getJdbcUrlWithEncryptedCredentialsOracle()
  {
    int atIdx = jdbcUrl.indexOf('@');
    String encryptedPassword = dbPassword;
    try
    {
       encryptedPassword = CryptUtil.encrypt(dbPassword);
    }
    catch (GeneralSecurityException ex)
    {
      PluginMessages.log(ex);
    }
    return jdbcUrl.substring(0, atIdx) + dbUser + "/" + encryptedPassword + jdbcUrl.substring(atIdx);
  }

  private String dbUser;
  public String getDbUser() { return dbUser; }
  public void setDbUser(String s) { dbUser = s; }

  private String dbPassword;
  public String getDbPassword() { return dbPassword; }
  public void setDbPassword(String s) { dbPassword = s; }
  public String getDbPasswordBase64()
  {
    return new String(Base64.encodeBase64(dbPassword.getBytes()));
  }

  private String schemaOwner;
  public String getSchemaOwner() { return schemaOwner; }
  public void setSchemaOwner(String owner) { schemaOwner = owner; }

  public String getDbHost()
  {
    int atIdx = jdbcUrl.indexOf('@');
    return jdbcUrl.substring(atIdx + 1, jdbcUrl.indexOf(':', atIdx));
  }

  public String getDbPort()
  {
    int atIdx = jdbcUrl.indexOf('@');
    int firstColonIdx = jdbcUrl.indexOf(':', atIdx);
    return jdbcUrl.substring(firstColonIdx + 1, jdbcUrl.indexOf(':', firstColonIdx + 1));
  }

  public String getDbName()
  {
    int lastColonIdx = jdbcUrl.lastIndexOf(':');
    return jdbcUrl.substring(lastColonIdx + 1);
  }

  private String entrySource;
  public String getEntrySource() { return entrySource; }
  public void setEntrySource(String source) { this.entrySource = source; }

  public String toString()
  {
    String encryptedPassword = "";
    try
    {
       encryptedPassword = getDbPassword() == null ? null : CryptUtil.encrypt(getDbPassword());
    }
    catch (Exception ex)
    {
      PluginMessages.log(ex);
    }

    return "JdbcDataSource:\n---------------\n"
      + "name: " + getName() + "\n"
      + "driver: " + getDriver() + "\n"
      + "jdbcUrl: " + getJdbcUrl() + "\n"
      + "dbUser: " + getDbUser() + "\n"
      + "dbPassword: " + encryptedPassword + "\n"
      + "schemaOwner: " + getSchemaOwner() + "\n";
  }
}
