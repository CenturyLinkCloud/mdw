# MDW Base Package

Many MDW components are delivered in the form of [Assets](https://centurylinkcloud.github.io/mdw/docs/help/assets.html),
which are organized into packages similar to Java packages except that they can contain many other artifacts
besides Java source code.  The package containing this readme (`com.centurylink.mdw.base`) also includes many of the most
fundamental assets required for MDW to function.  It's automatically imported into any new MDW project created by 
[MDW Studio](https://centurylinkcloud.github.io/mdw/docs/guides/mdw-studio) or the 
[CLI](https://centurylinkcloud.github.io/mdw/docs/getting-started/quick-start/).

## Base Package Contents
Contained in this package are the icons and pagelet definitions for many of the most basic [activity implementors](https://centurylinkcloud.github.io/mdw/docs/help/implementor.html)
that MDW provides out of the box.  Let's take the [RestServiceAdapter](https://centurylinkcloud.github.io/mdw/docs/javadoc/com/centurylink/mdw/workflow/adapter/rest/RestServiceAdapter.html)
as an example.  Here's what the @Activity annotation looks like in the activity source code:
```java
@Activity(value="REST Service Adapter", category=AdapterActivity.class, icon="com.centurylink.mdw.base/adapter.png",
        pagelet="com.centurylink.mdw.base/restService.pagelet")
```
The `icon` attribute designates an asset path which, unlike a qualified Java class, contains a slash separating the package
name from the asset.  The restService.pagelet asset is also included in this package and referenced in the same way.

## Package Discovery
Besides this base package, MDW provides quite a number of other packages built for specific purposes.  For example, to
integrate with Microsoft Teams, we provide the `com.centurylink.mdw.msteams` package.  You can find out about available
packages by leveraging MDW's [discovery mechanism](https://centurylinkcloud.github.io/mdw/docs/guides/mdw-studio/#4-discover-and-import-asset-packages).
In fact, you can add your team's Git URLs into the discovery list to facilitate reuse of your assets as well.

Assets imported in MDW Studio are typically version controlled in Git.  A developer who needs an asset package's 
functionality can import the package locally and commit/push to Git so that it'll be available to others working on the project.
In MDWHub the Admin tab > Assets nav link enables you to discover and import assets into a specific environment.  Unlike
in MDW Studio, packages imported through MDWHub are usually not committed to version control.  A use case for importing
in MDWHub is importing test packages so that automated tests can be executed.  For instance you might import certain MDW test
packages to execute as sanity tests in a newly stood-up environment.

## Package Meta Files
MDW package folders are identified by the fact that each contains a subdirectory named `.mdw`.  This directory holds the
package.yaml meta file which describes the package.  An important element in package.yaml is the version.  Like an application
version, an MDW package version should uniquely identify a package's contents for a specific delivery.  So for example
if your project contains a package `com.example.fantastic.workflow`, version 2.1.29 of this package should always be exactly
the same whether it's installed into a dev, test or prod environment.

## Package Dependencies
Asset packages may require other packages in order to work properly.  These dependencies can be declared in your package.yaml
meta file.  Here's an example from version 6.1.31 of the MDW dashboard package:
```yaml
schemaVersion: '6.1'
name: com.centurylink.mdw.dashboard
version: 6.1.31

dependencies:
  - com.centurylink.mdw.base v6.1.31
  - com.centurylink.mdw.node v6.1.31
  - com.centurylink.mdw.react v6.1.31
```
A couple of key points that this package.yaml illustrates:
  - Although MDW uses semantic versioning for individual assets (see [Smart Subprocess Versioning](https://centurylinkcloud.github.io/mdw/docs/help/InvokeSubProcessActivity.html)), 
    package dependencies do not [currently support](https://github.com/CenturyLinkCloud/mdw/issues/796)
    semantic versioning.  You must specify an actual package version. The CLI `dependencies` command, and MDW Studio's 
    dependencies check both consider a dependency to be met if the same or later version of the designated package is installed.
  - Transitive dependencies are [not automatically resolved](https://github.com/CenturyLinkCloud/mdw/issues/797).  So even
    though package `com.centurylink.mdw.react` already depends on `com.centurylink.mdw.node`, and `com.centurylink.mdw.node`
    already depends on `com.centurylink.mdw.base`, all of these must be explicitly declared as dependencies.
    
Package dependencies can be manually checked through MDW Studio (Tools > MDW > Check Dependencies), or by running the CLI
`dependencies` command.  On server startup, package dependencies are checked automatically, and a warning is logged for any
missing dependencies. 
