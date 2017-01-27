Steps for Building and Publishing a New MDW RCP Release:
--------------------------------------------------------

Build com.centurylink.mdw.designer according to the instructions in its build_readme.txt.
 - Afterward change the PDE Target Platform to the latest release (Running Platform) 

In com.centurylink.mdw.designer.rcp:
 - Update MDWDesignerRCP.product (in text editor):
     product version
     feature version
     aboutText
     (if upgrading Eclipse version): org.eclipse.rcp and org.eclipse.equinox.p2.user.ui features
 - Update product_build.xml:
     (if upgrading Eclipse version): eclipse.home and deltapack properties
 - Update build.properties to reflect your local environment:
     - JavaSE-1.7, JavaSE-1.8

Build the Workspace (ctrl-b).

deltapack - (if you don't have)
 - Can be built through Ant (create_deltapack.xml -- Run in same JRE as workspace)
   - output is in rcp.deltapack/featureTemp2 for me 
 or...
 - Copy deltapack from following location and extract it 
 	\\eldnp1515dm4.ad.qintra.com\union_station\IT\MDW\eclipse\deltapack
 - set eclipse.home and deltapack properties in product_build.xml

Run the "buildProduct" target in product_build.xml
(make sure Ant launch config is set to run in same JRE as workspace)

(TODO: automate the following)
Explode the zips and modify as follows (TODO automate):
 - Add these two lines to configuration/config.ini:
osgi.splashPath=platform:/base/plugins/com.centurylink.mdw.plugin.rcp
osgi.instance.area.default=@user.home/workspace
 - Copy org.eclipse.ui.win32_3.2.500.v20150423-0822.jar from eclipse_4.6.2/plugins into the plugins directory for in-place editor support
   (TODO try automating by adding to feature.xml)
 - Copy in the appropriate jre directory depending on architecture (jdk_1.8 64 bit or 32 bit) 

DO NOT launch the executable before repackaging.

Zip the mdw directory (not mdw_X.X.X since in-place upgrading would make this dir name out-of-date).
Rename the mdw directory you just zipped up to mdw_x.x.x_xxx and test that it is launchable.
Publish the zips to \\eldnp1515dm4.ad.qintra.com\union_station\IT\MDW\RCP

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

Web Start:
----------
  - No longer supported: see mdw-webstart project readme.txt.
  
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