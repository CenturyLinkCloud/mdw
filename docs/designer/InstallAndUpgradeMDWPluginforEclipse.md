### Installing and Upgrading the MDW Plugin for Eclipse

#### Set Up Eclipse on Your PC:
- Make sure you have Java Runtime 8 installed on your computer:                                                           
  [http://www.oracle.com/technetwork/java/javase/downloads](http://www.oracle.com/technetwork/java/javase/downloads)
  
- Install Eclipse Neon (4.6.x):                                                                                                         
  [http://www.eclipse.org/downloads](http://www.eclipse.org/downloads)
   
- Launch Eclipse.  Here’s an example command line that includes appropriate JVM memory settings for running and debugging:              
    - `C:\eclipse_4.6.2\eclipse.exe -vm C:\jdk1.8.0_60\bin\javaw.exe -vmargs -Dsun.lang.ClassLoader.allowArraySyntax=true -Xms512m -Xmx1024m -XX:MaxPermSize=256m`
- Install the latest version of the MDW Plugin via Eclipse Software Updates:                                                            
 (Help > Install New Software > Add > http://centurylinkcloud.github.io/mdw/docs/designer/updateSite/plugins> Install).
 
#### Optional Plug-Ins: 
- Buildship Plugin:                                                                                                                     
  [http://download.eclipse.org/buildship/updates/e46/releases/2.x](http://download.eclipse.org/buildship/updates/e46/releases/2.x)
  
- Groovy Eclipse Plug-In (Groovy script syntax highlighting, auto-complete, etc.):
   [http://dist.springsource.org/snapshot/GRECLIPSE/e4.6](http://dist.springsource.org/snapshot/GRECLIPSE/e4.6)

- YAML Editor (YAML syntax highlighting, etc.):                                                                                         
   [http://dadacoalition.org/yedit](http://dadacoalition.org/yedit)
  
- Quantum DB Plug-In (Database querying):                                                                                               
   [http://quantum.sourceforge.net/update-site](http://quantum.sourceforge.net/update-site)

#### Upgrading to a New MDW Plugin Release:
- From the Eclipse menu select Help > Install New Software.
- In the "Work with:" dropdown, find the MDW update site URL.
- Select the latest version and click Next to continue through the install wizard.
