---
permalink: /docs/guides/SetupGuideForCloudFoundry/
title: Setup Guide for CloudFoundry
---

### MDW Cloud Foundry Setup Guide

### Prerequisite
 - Eclipse Neon for JavaEE Developers:  
   http://www.eclipse.org/downloads
 - Required Plugins: 
   `Note: These are Eclipse plug-ins and as a result, they need to be installed and updated in the Eclipse. Please use the following steps to install the latest plug-ins`.
   - MDW Designer:  http://centurylinkcloud.github.io/mdw/designer/updateSite
     - Copy the following url: `http://centurylinkcloud.github.io/mdw/designer/updateSite`
     - Go to `Help > Install New Software` and paste it in the `Work with` and press Enter.
     - Select the `BMP Workflow Designer` and click `Next`
     - Highlight the `MDW Designer` and `Finish`.
     - Click `Yes` on the pop-up window to restart your Eclipse.
   - Buildship Plugin: http://download.eclipse.org/buildship/updates/e46/releases/2.x
     - Follow the same steps as above to install the Buildship Plugin.
       
 - Running MDW Locally:
     - Refer to `Tomcat Container` in this guide 
 
### Running in the Cloud

#### 1. Clone the Demo Project from GitHub

A quick way to get familiar with the layout of an mdw workflow cloud project is to start with the mdw-demo project. This project is available from our GitHub instance of the CenturyLink.
- For your eclipse setup guide, refer to the `readme.md` file (step 1 and step 3 only) in the [https://github.com/CenturyLinkCloud/mdw-demo](https://github.com/CenturyLinkCloud/mdw-demo) project and return to this guide to continue. 

##### MDW Designer Perspective:
-	To best view mdw-demo's workflow assets, switch to Designer Perspective (Window > Perspective > Open Perspective > Other > MDW Designer).
-	In Process Explorer view expand the mdw-demo project to see the included workflow packages.  These packages contain the assets that you will deploy to Cloud Foundry in the following steps.

#### 2. Build and Deploy to Cloud Foundry

##### Ensure Permissions:
-	Edit mdw-demo/config/seed_users.json to add yourself with permissions similar to the other users in the file.  Note: The default configuration uses an embedded MariaDB database for runtime persistence.  For non-dev environments, longer term persistence is required, and this can be accomplished using an external MariaDB or MySQL database configured in your manifest.yml descriptor.

##### Install Cloud Foundry CLI:
-	This tutorial uses the Cloud Foundry Command Line Interface:     
    - Click this link to download: [https://cli.run.pivotal.io/stable?release=windows64&source=github](https://cli.run.pivotal.io/stable?release=windows64&source=github)
    - Once it has been downloaded, double click it to run it. It will be installed on C:\Program Files\Cloud Foundry.

##### Cloud Foundry Development Tools in Eclipse:
-	Eclipse Neon comes with built-in Cloud Foundry support: [(https://projects.eclipse.org/projects/ecd.cft](https://projects.eclipse.org/projects/ecd.cft)    
    - Go to menu: Help -> Eclipse Marketplace
    - Search for "Eclipse Tools for Cloud Foundry"
    - Select "Update" or "Install" in the "Eclipse Tools for Cloud Foundry" 

##### Perform a `cf push` to deploy mdw-demo:
Note: The following examples use the MDW demo deployment in CenturyLink's AppFog cloud environment:
-   See the Cloud Foundry Dev Guide: [https://docs.cloudfoundry.org/devguide](https://docs.cloudfoundry.org/devguide), or type `cf help` for details about the available commands. 
-	A Cloud Foundry deployment is configured primarily through a manifest.yml file in the project root directory.  Take a look at the `manifest.yml` in mdw-demo, and note that it stipulates the MDW buildpack through its Git repository URL.  The settings you'll likely need to configure in this file are as follows:
    - `MDW_VERSION` - Whichever version of the MDW framework you're using (must be available as a buildpack).
    - `mdw.database.ur` - Only if you're using an external database rather than embedded.
    - `mdw.hub.url` - This is the user-access app endpoint URL as reported after a successful push.  
    - `mdw.services.url` - The services endpoint.  In development this is the same as mdw.hub.url.

-	After you've installed the Cloud Foundry CLI, open a command-line window in the mdw-demo root directory.  
-   Use the `cf login` command, then enter your credentials to log in to your cloud space:
    `cf login -a https://api.useast.appfog.ctl.io -o MDWF` (The -a parameter designates the API endpoint, and the -o parameter is the organization -- type `cf login -h` for details).
        
-	Now, to deploy the mdw-demo app simply type cf push: `cf push`
-	In another command-line session, you can tail the application logs by typing: `cf logs mdw-demo`
-	Once the push is complete, you will see the message like this:

   ![alt text](../images/commanLineDeployment.png "commanLineDeployment")
    
-  You can also verify successful deployment by accessing MDWHub in your browser with a URL something like this: 
   [https://mdw-demo.useast.appfog.ctl.io/mdw](https://mdw-demo.useast.appfog.ctl.io/mdw)

##### Make a Change and Push Again:
-	In Eclipse switch to MDW Designer perspective and expand the MyServices.  Open the Employees.java Dynamic Java asset and edit the get() method of this REST service to expect your ID and return your employee information:
-	Save the file, incrementing its version, and type the `cf push` command again.
-	Once the push has completed, access the service in your browser through a URL like the following: 
     - http://mdw-demo.useast.appfog.ctl.io/mdw/Services/MyServices/Employees/your-cuid

### Running Locally
#### 1. Tomcat Container

It can quickly become tedious to build and push to Cloud Foundry every time you want to test a code change.  To deploy mdw-demo locally, you can run on Tomcat.  With a Tomcat server running locally, your changes can be hot-deployed so that pushes and server restarts are not required.

##### Supported Tomcat Containers:
-	Apache Tomcat 8: Please follow the [Setup Guide for Tomcat](../SetupGuideForTomcat/) and return to this guide to continue.                             
 
#### 2. Deploy Locally

##### Run Tomcat:
-	Edit the following properties in `config/mdw.properties':
```
  mdw.asset.location - This is the directory path on your hard-drive where your assets are located.     
  mdw.git.local.path - The root directory of your mdw-demo Git project.     
  mdw.hub.user - You may have noticed the Java system property runtimeEnv in your server config, which is preset to "dev".  This property allows you to bypass authentication locally.  
```
-	Now that you've created the WTP server instance, the Servers view gives you a handy way to start and stop Tomcat.  And output is directed to the Eclipse Console view, where you can click on stack traces to open associated source code (including MDW code and Dynamic Java).  Start your server in debug mode by right-clicking on it and selecting Debug (or use the icon in the Servers view toolbar).
-	The first time you start your server Tomcat explodes the mdw.war file in your deploy/webapps directory and caches the deployable content.  This can sometimes take a minute.  With the server running you should see MDW output in the Eclipse Console view.
Tip: When you upgrade to a new MDW build version in Eclipse, Designer automatically downloads the corresponding mdw.war file into your deploy/webapps directory.  If at any time you want to clean out the MDW deployment and start fresh, you can delete mdw.war and the exploded mdw directory (and for a very thorough cleansing you can even delete the Tomcat cache under deploy/work/Catalina/localhost/mdw).  Then you can deploy from scratch from Package Explorer view by right-clicking on your mdw-workflow or mdw-demo project and selecting MDW Update > Update Framework Libraries.
-	You can confirm that MDW was successfully deployed by accessing MDWHub in your browser:                          
  [http://localhost:8080/mdw](http://localhost:8080/mdw)

##### Make and Test a Change:
-	Make another change to the Employees service.  After saving the asset in Designer, you should be able to access the service right away and see your changes at:                                             
  http://localhost:8080/mdw/Services/MyServices/Employees/your-cuid
-	Once you've implemented and tested a feature, you can deploy to the cloud using `cf push`.

##### Next Steps:
-	Check out some of the other MDW developer docs:       
    - [Microservices Cookbook](../MicroservicesCookbook/)            
    - [SOAP-based Web Service](../SOAPService/)
-	Browse through the online help docs, which are the same as those in Eclipse:                             
 	  [http://centurylinkcloud.github.io/mdw/help](http://centurylinkcloud.github.io/mdw/docs/help)
-	The MDW JavaDocs:                                                     
      [http://centurylinkcloud.github.io/mdw/javadoc/index.html](http://centurylinkcloud.github.io/mdw/docs/javadoc/index.html)
