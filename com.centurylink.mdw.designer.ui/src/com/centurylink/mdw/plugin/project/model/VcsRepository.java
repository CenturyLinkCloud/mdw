package com.centurylink.mdw.plugin.project.model;

import java.security.GeneralSecurityException;
import java.util.List;

import org.apache.commons.codec.binary.Base64;

import com.centurylink.mdw.common.utilities.CryptUtil;
import com.centurylink.mdw.plugin.PluginMessages;

public class VcsRepository
{
  public static final String DEFAULT_REPOSITORY_URL = "";
  public static final String DEFAULT_USER = "";
  public static final String DEFAULT_PASSWORD = "";
  public static final String DEFAULT_BRANCH = "master";
  public static final String DEFAULT_LOCAL_PATH = "assets";

  public static final String PROVIDER_GIT = "Git";

  private String provider;
  public String getProvider() { return provider; }
  public void setProvider(String provider) { this.provider = provider; }

  private String localPath = DEFAULT_LOCAL_PATH;
  public String getLocalPath() { return localPath; }
  public void setLocalPath(String path) { this.localPath = path; }

  private List<String> pkgPrefixes;
  public List<String> getPkgPrefixes() { return pkgPrefixes; }
  public void setPkgPrefixes(List<String> prefixes) { this.pkgPrefixes = prefixes; }

  public boolean hasRemoteRepository()
  {
    return repositoryUrl != null && !repositoryUrl.isEmpty();
  }

  private String repositoryUrl;
  public String getRepositoryUrl() { return repositoryUrl; }
  public void setRepositoryUrl(String url) { repositoryUrl = url; }
  public String getRepositoryUrlWithCredentials()
  {
    if (!hasRemoteRepository())
      return null;
    if (user == null) // public repo
      return repositoryUrl;
    int slashSlash = repositoryUrl.indexOf("//");
    return repositoryUrl.substring(0, slashSlash + 2) + user + ":" + password + "@" + repositoryUrl.substring(slashSlash + 2);
  }

  public boolean setRepositoryUrlWithCredentials(String repoUrlWithCreds)
  {
    // https://mdw:ldap_0123@8.22.8.164/mdw/mdw-workflow.git
    // or https://8.22.8.164/mdw/mdw-workflow.git (if no credentials)
    int at = repoUrlWithCreds.indexOf("@");
    if (at == -1)
    {
      repositoryUrl = repoUrlWithCreds;
    }
    else
    {
      try
      {
        int slashSlash = repoUrlWithCreds.lastIndexOf("//");
        repositoryUrl = repoUrlWithCreds.substring(0, slashSlash + 2) + repoUrlWithCreds.substring(at + 1);
        int colon = repoUrlWithCreds.indexOf(":", slashSlash);
        user = repoUrlWithCreds.substring(slashSlash + 2, colon);
        String newGitPassword = repoUrlWithCreds.substring(colon + 1, at);
        if (newGitPassword.length() == 32)
        {
          // encrypted password
          try
          {
            password = CryptUtil.decrypt(newGitPassword);
          }
          catch (GeneralSecurityException ex)
          {
            PluginMessages.uiError(ex, "Security Exception");
          }
        }
        else
        {
          boolean allStars = true;
          for (int i = 0; i < newGitPassword.length(); i++)
          {
            if (newGitPassword.charAt(i) != '*')
            {
              allStars = false;
              break;
            }
          }
          if (!allStars)
            password = newGitPassword;
        }
      }
      catch (StringIndexOutOfBoundsException ex)
      {
        return false;
      }
    }
    return true;
  }

  public String getRepositoryUrlWithMaskedCredentials()
  {
    if (!hasRemoteRepository())
      return null;

    if (user == null)
      return getRepositoryUrl();

    String maskedPassword = "";
    for (int i = 0; i < password.length(); i++)
      maskedPassword += "*";

    int slashSlash = repositoryUrl.indexOf("//");
    return repositoryUrl.substring(0, slashSlash + 2) + user + ":" + maskedPassword + "@" + repositoryUrl.substring(slashSlash + 2);
  }

  public boolean equalsIgnoreMask(String repoUrl)
  {
    VcsRepository newRepo = new VcsRepository();
    newRepo.setRepositoryUrlWithCredentials(repoUrl);
    if (newRepo.getPassword() == null)
      newRepo.setPassword("temp");  // avoid NPE
    return getRepositoryUrlWithMaskedCredentials().replaceAll("\\*", "").equals(newRepo.getRepositoryUrlWithMaskedCredentials().replaceAll("\\*", ""));
  }

  public String getRepositoryUrlWithEncryptedCredentials()
  {
    if (user == null || password == null)
      return repositoryUrl;
    String encryptedPassword = password;
    try
    {
       encryptedPassword = CryptUtil.encrypt(password);
    }
    catch (GeneralSecurityException ex)
    {
      PluginMessages.log(ex);
    }
    int slashSlash = repositoryUrl.indexOf("//");
    return repositoryUrl.substring(0, slashSlash + 2) + user + ":" + encryptedPassword + "@" + repositoryUrl.substring(slashSlash + 2);
  }

  private String user;
  public String getUser() { return user; }
  public void setUser(String user) { this.user = user; }

  private String password;
  public String getPassword() { return password; }
  public void setPassword(String password) { this.password = password; }
  public String getPasswordBase64()
  {
    return new String(Base64.encodeBase64(password.getBytes()));
  }

  private String branch = "master";
  public String getBranch() { return branch; }
  public void setBranch(String branch) { this.branch = branch; }

  // only for remote projects
  private String commit;
  public String getCommit() { return commit; }
  public void setCommit(String commit) { this.commit = commit; }

  private String entrySource;
  public String getEntrySource() { return entrySource; }
  public void setEntrySource(String source) { this.entrySource = source; }

  private boolean syncAssetArchive;
  public boolean isSyncAssetArchive() { return syncAssetArchive; }
  public void setSyncAssetArchive(boolean sync) { this.syncAssetArchive = sync; }

  private boolean gitProjectSync;
  public boolean isGitProjectSync() { return gitProjectSync; }
  public void setGitProjectSync(boolean sync) { this.gitProjectSync = sync; }

  public void clear()
  {
    provider = null;
    repositoryUrl = null;
    user = null;
    password = null;
    branch = null;
    entrySource = null;
  }

  public String toString()
  {
    String encryptedPassword = "";
    try
    {
       encryptedPassword = getPassword() == null ? null : CryptUtil.encrypt(getPassword());
    }
    catch (Exception ex)
    {
      PluginMessages.log(ex);
    }

    return "GitRepository:\n-------------\n"
      + "provider: " + getProvider() + "\n"
      + "repositoryUrl: " + getRepositoryUrl() + "\n"
      + "user: " + getUser() + "\n"
      + "password: " + encryptedPassword + "\n"
      + "localPath: " + getLocalPath() + "\n"
      + "branch: " + branch;
  }

}
