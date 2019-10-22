# Asset Staging

Users with Asset Design role can edit assets online through MDWHub.  The purpose of asset staging is to control the 
introduction of changes made this way.  The staging mechanism enables users to save incremental changes without applying
them until they're ready.  One or many staged assets can be promoted (applied live) and rolled back (withdrawn) through
the MDWHub interface.

## Staging Area  
If you intend to modify assets online, you'll start by initializing your dedicated staging area through MDWHub's Admin tab.
After clicking on the Staging navigation link for the first time, you'll be presented with a button as depicted below.

<img src="https://raw.githubusercontent.com/CenturyLinkCloud/mdw/master/mdw-workflow/assets/com/centurylink/mdw/staging/staging_btn.png" alt="Staging Button" width="95%"/>

Clicking the Creating Staging Area button will begin preparation of your personal staging area, a one-time process which
can take a few minutes and may appear stalled at times.  During preparation you can visit other areas of MDWHub and
return to check on it's progress.

<img src="https://raw.githubusercontent.com/CenturyLinkCloud/mdw/master/mdw-workflow/assets/com/centurylink/mdw/staging/staging_prep.png" alt="Staging Prep" width="95%"/>

Certain activities, such as an application redeployment, can require that your staging area be reinitialized.  This may 
be a nuisance, but be assured you'll not lose any of your work and your staged assets will reappear once preparation 
is complete. 

To stage an asset, search for it in the staging area Finder textbox, or browse by clicking the Assets nav link, and then 
click the Edit button.  Once an asset is staged to your area, it's available to edit in MDWHub.  Your changes are saved
but not applied live until you promote them.  If you unstage an asset your changes are erased and the asset is removed
from your staging area.

In your staging area you can also create brand new assets by clicking the Plus button.  Newly created assets are automatically
added to your staging area.

## Promotion

TODO

## Rollback

TODO

## Technical Details

MDW assets are versioned in Git.  A user staging area is actually a Git branch named "staging_*userId*_*runtimeEnv*".
Preparation of a user staging area involves cloning to the MDW temp directory under git/staging.

Since server-side resources under MDW temp can be deleted and recreated at any time, changes saved to a staged asset
are immediately committed and pushed to the user stage branch.  This enables user staging areas to be recreated at any
time by cloning from Git remote.

To avoid requiring Git accounts for all staging users, commits are performed as git.user from mdw.yaml.  The commit message
for a staged asset save includes the user ID who made the change.  Unstaging commits a new change to the staging branch to undo
any previously staged change(s).
