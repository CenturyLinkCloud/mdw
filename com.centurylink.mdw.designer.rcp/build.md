Steps for Building and Publishing a New MDW RCP Release:
--------------------------------------------------------

Build the latest framework code:
 - (In MDW 5.5) Run the mdw-framework/buildAll Gradle task.

In com.centurylink.mdw.designer:
 - Make sure the following in gradle.properties match your environment and desired build version:
    mdwOutputDir
    eclipseDir
    mdwDesignerVersion
 - Run gradle task buildFeature to build the plugin locally 

In com.centurylink.mdw.designer.rcp:
 - Update MDWDesignerRCP.product (in text editor):
     product version
     feature version
     aboutText
     (if upgrading Eclipse version): org.eclipse.rcp and org.eclipse.equinox.p2.user.ui features
 - Update product_build.xml:
     eclipse.home and deltapack properties (reflect current runtime)
 - If you don't already have the matching Eclipse deltapack locally:
     A. Copy from \\eldnp1515dm4.ad.qintra.com\union_station\IT\MDW\eclipse\deltapack
       OR..
     B. Can be built through Ant:
        - update buildId and buildRepo properties in create_deltapack.xml to match current release
        - run Ant build target in create_deltapack.xml (in same JRE as workspace) -> output is in rcp.deltapack/featureTemp2
 - Update build.properties to reflect your local environment:
     JavaSE-1.7
     JavaSE-1.8
     x86.jre
     x64.jre

Build the Workspace (ctrl-b).

Run the "build" target in product_build.xml
(make sure Ant launch config is set to run in same JRE as workspace)

Make sure the exploded build output under tmp is launchable for both x86 and x64.
Publish the zips from build/zip to \\eldnp1515dm4.ad.qintra.com\union_station\IT\MDW\RCP

Upload the following files to the /prod/ecom2/local/apps/MdwRcp directory on lxdenvmtc143.dev.qintra.com:
   - com.centurylink.mdw.designer.feature/site.xml
   - buildDirectory/buildRepo/artifacts.xml
   - buildDirectory/buildRepo/content.xml
   - buildDirectory/buildRepo/features/com.centurylink.mdw.designer.feature_X.X.X.jar
   - buildDirectory/buildRepo/plugins/com.centurylink.mdw.designer.core_X.X.X.jar
   - buildDirectory/buildRepo/plugins/com.centurylink.mdw.designer.ui_X.X.X.jar
   - buildDirectory/buildRepo/plugins/com.centurylink.mdw.designer.rcp_X.X.X.jar
   - buildDirectory/buildRepo/binary/com.centurylink.mdw.designer.rcp_root.win32.win32.x86_X.X.X
   - buildDirectory/buildRepo/binary/com.centurylink.mdw.designer.rcp_root.win32.win32.x86_64_X.X.X
   - buildDirectory/buildRepo/binary/com.centurylink.mdw.designer.rcp.root.feature_root_X.X.X
   (plus any changed dependencies under features, plugins or binary).

Log into the server (as your CUID) and chmod -R a+rwx /prod/ecom2/local/apps/MdwRcp/*

Test updating a previous RCP installation to the new build.
Update Site URL: http://lxdenvmtc143.dev.qintra.com:6101/MdwRcp


**** TROUBLESHOOTING ****

Errors when Testing:
--------------------
If MDWDesignerRCP.product cannot be launched for debug due to missing dependencies,
edit the launch configuration to "Add Required Plug-ins".

Dependencies:
-------------
If the build fails during [p2.director] due to "No repository found containing: xxx"
 - Add the necessary plugins on the Plug-ins tab of the feature.xml editor.
 
If the exported RCP application won't run due to missing dependencies, make
sure to select the required dependencies in the GUI screens for both plugin.xml.

Startup Errors:
---------------
(in config.ini) Make sure to use simple configurator: 
 - osgi.bundles=reference\:file\:org.eclipse.equinox.simpleconfigurator_1.0.101.R35x_v20090807-1100.jar@1\:start
 
Remote Debugging:
-----------------
Do not try to add debug args to mdw.ini.  Instead, launch with the following command line:
mdw.exe -vmargs -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8017

If LDAP Auth has Problems:
--------------------------
Users can revert to CT auth by adding the following to mdw.ini:
-Dmdw.authentication.provider=cleartrust