---
permalink: /docs/getting-started/install-designer/
title: Install MDW Designer
---

<script>
function copyToClipboard() {
  console.log("COPY TO CLIPBOARD");
  // var element = document.getElementById('input');
  // element.select();
  // document.execCommand('copy');
  // element.blur();
}
</script>

Install the MDW Designer Eclipse Plugin

### Prerequisites
  - Java 8   
  If you type `java -version` on the command line and see **1.8**, you're all set:
  ```
  java version "1.8.0_60"
  ...
  ```
  - Eclipse Oxygen for JavaEE Developers:<br>
    <https://www.eclipse.org/downloads/packages/eclipse-ide-java-ee-developers/oxygen2>  
  
### Install Designer from Eclipse Marketplace
  - Launch Eclipse and from the menu select Help > Eclipse Marketplace...
  - Type "MDW Designer for Eclipse" in the find text box and do search
  - Click on the install button of the displayed result

### Install Designer from our Update Site
  - Launch Eclipse and from the menu select Help > Install New Software...
  - Add the update site URL: {% include copyToClipboard.html text="http://centurylinkcloud.github.io/mdw/designer/updateSite" %}
  ![install designer plugin](../images/designerPlugin.png "designerPlugin")

### Recommended Plugins
  These plugins are optional (although Groovy is highly recommended).
  They can be installed through Help > Install New Software... the same as above.
  - Groovy: {% include copyToClipboard.html text="http://dist.springsource.org/release/GRECLIPSE/e4.7" %}
  - Yaml Editor: {% include copyToClipboard.html text="http://dadacoalition.org/yedit" %}

### Designer User Guide
  To learn about creating workflows and assets using MDW Designer in Eclipse,
  [check out the Designer User Guide](../../designer/user-guide).
