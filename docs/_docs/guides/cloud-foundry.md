---
permalink: /docs/guides/SetupGuideForCloudFoundry/
title: Cloud Foundry Setup Guide
---

### MDW Cloud Foundry Setup Guide

### Prerequisite
 - Eclipse with the [MDW Designer Plugin](../../getting-started/install-designer)
 - An MDW Project, which can be created by following the [quick-start guide](../../getting-started/quick-start)
 - For running MDW locally in Tomcat, refer to the [Tomcat Setup Guide](../tomcat) 
 
### Running in the Cloud

#### 1. Clone the Demo Project from GitHub

A quick way to get familiar with the layout of an mdw project is to start with the mdw-demo project. This project is available from our GitHub instance of the CenturyLink.
- For your Eclipse setup guide, refer to the `README.md` file (step 1 and step 3 only) in the [mdw-demo](https://github.com/CenturyLinkCloud/mdw-demo) project and return to this guide to continue. 

##### MDW Designer Perspective:
-	To best view mdw-demo's workflow assets, switch to Designer Perspective (Window > Perspective > Open Perspective > Other > MDW Designer).
-	In Process Explorer view expand the mdw-demo project to see the included workflow packages.  These packages contain the assets that you will deploy to Cloud Foundry in the following steps.

#### 2. Build and Deploy to Cloud Foundry

##### Ensure Permissions:
-	Edit `mdw-demo/config/seed_users.json` to add yourself with permissions similar to the other users in the file.  Note: The default configuration uses an embedded MariaDB database for runtime persistence.  For non-dev environments, longer term persistence is required, and this can be accomplished using an external MariaDB or MySQL database configured in your manifest.yml descriptor.

##### Install Cloud Foundry CLI:
-	This tutorial uses the Cloud Foundry Command Line Interface:     
    - Follow [this link](https://cli.run.pivotal.io/stable?release=windows64&source=github) to download Cloud Foundry CLI.
    - Once it has been downloaded, double click it to run it. It will be installed on C:\Program Files\Cloud Foundry.

##### Cloud Foundry Development Tools in Eclipse:
-	Eclipse Neon comes with a built-in Cloud Foundry support: [(https://projects.eclipse.org/projects/ecd.cft](https://projects.eclipse.org/projects/ecd.cft)    
    - Go to menu: Help -> Eclipse Marketplace
    - Search for `Eclipse Tools for Cloud Foundry`
    - Select `Update` or `Install` in the `Eclipse Tools for Cloud Foundry`

##### Perform a `cf push` to deploy mdw-demo:
Note: The following examples use the MDW demo deployment in CenturyLink's AppFog cloud environment:
-   To see the Cloud Foundry Dev Guide, [click here](https://docs.cloudfoundry.org/devguide) or type `cf help` for details about the available commands.

-	A Cloud Foundry deployment is configured primarily through a manifest.yml file in the project root directory.  Take a look at the `manifest.yml` in mdw-demo, and note that it stipulates the MDW buildpack through its Git repository URL.  The settings you'll likely need to configure in this file are as follows:
    - `MDW_VERSION` - Whichever version of the MDW framework you're using (must be available as a buildpack).
    - `mdw.database.ur` - Only if you're using an external database rather than embedded.
    - `mdw.hub.url` - This is the user-access app endpoint URL as reported after a successful push.  
    - `mdw.services.url` - The services endpoint.  In development this is the same as mdw.hub.url.

-	After you've installed the Cloud Foundry CLI, open a command-line window in the `mdw-demo` root directory.  
-   Use the `cf login` command, then enter your credentials to log in to your cloud space:
    `cf login -a https://api.useast.appfog.ctl.io -o MDWF` (The -a parameter designates the API endpoint, and the -o parameter is the organization -- type `cf login -h` for details).
        
-	Now, to deploy the mdw-demo app simply type cf push: `cf push`
-	In another command-line session, you can tail the application logs by typing: `cf logs mdw-demo`
-	Once the push is complete, you will see the message like this:

   ![alt text](../images/commanLineDeployment.png "commanLineDeployment")
    
-  You can also verify successful deployment by accessing MDWHub in your browser with a URL something like this: 
   [https://mdw-demo.useast.appfog.ctl.io/mdw](https://mdw-demo.useast.appfog.ctl.io/mdw)

##### Make a Change and Push Again:
-	In Eclipse switch to MDW Designer perspective and expand the MyServices.  Open the Employees.java asset and edit the get() method of this REST service to expect your ID and return your employee information:
-	Save the file, incrementing its version, and type the `cf push` command again.
-	Once the push has completed, access the service in your browser through a URL like the following: 
     - http://mdw-demo.useast.appfog.ctl.io/mdw/Services/MyServices/Employees/your-cuid

### Running Locally
#### 1. Tomcat Container

It can quickly become tedious to build and push to Cloud Foundry every time you want to test a code change.  To deploy mdw-demo locally, you can run on Tomcat.  With a Tomcat server running locally, your changes can be hot-deployed so that pushes and server restarts are not required.

#### 2. Deploy Locally

##### Make and Test a Change:
-	Make another change to the Employees service.  After saving the asset in Designer, you should be able to access the service right away and see your changes at:                                             
  http://localhost:8080/mdw/Services/MyServices/Employees/your-cuid
-	Once you've implemented and tested a feature, you can deploy to the cloud using `cf push`.

##### Next Steps:
-	Check out some of the other MDW developer docs:   
    - [MDW Cookbook](../mdw-cookbook/)       
    - [SOAP Web Service](../SOAPService/)   
 
-   Browse through the online help docs, which are the same as those in Eclipse:   
     - [MDW Help Topics](http://centurylinkcloud.github.io/mdw/docs/help)    
 	  
-	The MDW JavaDocs:                                                     
    - [JavaDocs](http://centurylinkcloud.github.io/mdw/docs/javadoc/index.html)
