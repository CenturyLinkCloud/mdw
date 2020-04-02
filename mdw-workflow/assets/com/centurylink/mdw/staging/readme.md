# Asset Staging Package

Users with Asset Design role can edit assets online through MDWHub.  The purpose of asset staging is to control the 
introduction of changes made this way.  The staging mechanism enables users to save incremental changes without applying
them until they're ready.  One or many staged assets can be promoted (applied live) and rolled back (withdrawn) through
the MDWHub interface.

## Dependencies
  - [com.centurylink.mdw.base](https://github.com/CenturyLinkCloud/mdw/blob/master/mdw-workflow/assets/com/centurylink/mdw/base/readme.md)
  - [com.centurylink.mdw.node](https://github.com/CenturyLinkCloud/mdw/blob/master/mdw-workflow/assets/com/centurylink/mdw/node/readme.md)
  - [com.centurylink.mdw.react](https://github.com/CenturyLinkCloud/mdw/blob/master/mdw-workflow/assets/com/centurylink/mdw/react/readme.md)

## Staging Area  
If you intend to modify assets online, you'll start by initializing your dedicated staging area through MDWHub's Admin tab.
After clicking on the Staging navigation link for the first time, you'll be presented with a button as depicted below.

<img src="https://raw.githubusercontent.com/CenturyLinkCloud/mdw/master/mdw-workflow/assets/com/centurylink/mdw/staging/staging_btn.png" alt="Staging Button" width="95%"/>

Clicking the Create Staging Area button will begin preparation of your personal staging area, a one-time process which
can take a few minutes and may appear stalled at times.  During preparation you can visit other areas of MDWHub and
return to check on its progress.

<img src="https://raw.githubusercontent.com/CenturyLinkCloud/mdw/master/mdw-workflow/assets/com/centurylink/mdw/staging/staging_prep.png" alt="Staging Prep" width="95%"/>

Certain activities, such as an application redeployment, can require that your staging area be reinitialized.  This may 
be a nuisance, but be assured you'll not lose any of your work and your staged assets will reappear once preparation 
is complete. 

To stage an asset, search for it in the staging area Finder textbox, or browse by clicking the Assets nav link, and then 
click the Edit button.  Once an asset is staged to your area, it's available to edit in MDWHub.  Your changes are saved
but not applied live until you promote them.  If you unstage an asset your changes are erased and the asset is removed
from your staging area.

If you attempt to stage an asset that's already staged by someone else, you'll get an error message and the asset won't
be staged.

In your staging area you can also create brand new assets by clicking the Plus button.  Newly created assets are automatically
added to your staging area.

## Promotion

Promoting staged assets makes your changes immediately live.  Since staged assets may have interdependencies, all assets
are promoted together.  If you want to exclude an asset from promotion, you must unstage it before clicking the Promote 
button.  Once you confirm asset promotion all changes are saved and applied, and the MDW server cache is refreshed to 
reflect your changes.  Promoted assets are also removed from your staging area.

## Rollback

Promoted assets can be restored to a previous version by browsing to them through the Admin tab's Assets nav link.
Clicking on the History nav link for an asset shows its previous versions.   

<img src="https://raw.githubusercontent.com/CenturyLinkCloud/mdw/master/mdw-workflow/assets/com/centurylink/mdw/staging/asset_history.png" alt="Asset History" width="95%"/>

By clicking the rewind button next to a previous version, you'll add a change to your staging area to revert to that
specific version.  You can roll back multiple assets to accumulate the changes in your staging area until you're ready 
to promote.  Note that the reversion does not take place until the roll backed versions are promoted.  

## Technical Details

A user staging area is actually a Git branch named "staging_*userId*_*runtimeEnv*".
Preparation of a user staging area involves cloning to the MDW temp directory under git/staging.

Since server-side resources under MDW temp can be deleted and recreated at any time, changes saved to a staged asset
are immediately committed and pushed to the user stage branch.  This enables user staging areas to be recreated at any
time by cloning from Git remote.  Saving a staged asset also increments its version in .mdw/versions and commits/pushes
the versions file as well.

Because the temp/git/staging can be deleted at any time (especially due to redeployment), the following logic pertains 
when a user clicks on the Admin tab's Staging nav link:
  - Check Git remote for user staging branch.
    - If staging branch does not exist, display Create Staging Area button
    - Otherwise, if staging branch exists in Git remote, check for user staging directory under temp/git/staging
      - If local staging directory does not exist, clone from Git and display live progress info
      - If/when local staging area exists, pull from remote to refresh, and display any staged assets

To avoid requiring Git accounts for all staging users, commits are performed as git.user from mdw.yaml.  The commit message
for a staged asset save includes the user ID who made the change.  Unstaging commits a new change to the staging branch to undo
any previously staged change(s).

Promoting staged assets performs the following actions:
  - User staging branch is merged into the active branch (git.branch in mdw.yaml)
  - Asset import is performed on the server followed by cache refresh
  - Staged assets are unstaged (removed from user staging area)

## Configuration
In order to programmatically create and merge branches, MDW needs to be authenticated.  Therefore in 
[mdw.yaml](https://centurylinkcloud.github.io/mdw/docs/guides/configuration/#mdwyaml) you'll need to specify
git.user and git.password with an app account that has write permissions on the repository.

