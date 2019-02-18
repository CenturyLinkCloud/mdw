## MDW React components
To use any third-party React component, create a package (such as com.example.node) with an npm package.json file in it.
After you run "npm install" for your components, zip the node_modules directory into an asset called node_modules.zip.
Also add the node_modules folder (not the zip) to .gitignore and .mdwignore files in the package.  
The node_modules.zip asset is committed to Git, whereas the node_modules folder should not be.  
On startup in any environment node_modules.zip will be automatically exploded.  Take a look at the `com.centurylink.mdw.node`
package for a working example of this pattern.