---
permalink: /docs/guides/mdw-mobile/
title: MDW Mobile
---

## Installation
MDW Mobile is coming soon to the App Store and Google Play.  In the meantime follow these instructions to install on your device.

### Android
  - Install ADB on your PC or Mac (this is already installed if you have Android Studio):<br/>
    <https://developer.android.com/studio/command-line/adb.html>
  - Download the APK file from here:<br/>
    <https://github.com/CenturyLinkCloud/mdw-mobile/releases/tag/1.0.01>
  - Open a command window in the same directory where the apk is located, and run:
    `adb install mdw-debug.apk`

### iOS
  - Find out your iOS device ID as described in 
    [Apple developer documentation](https://developer.apple.com/library/content/documentation/IDEs/Conceptual/AppDistributionGuide/MaintainingProfiles/MaintainingProfiles.html#//apple_ref/doc/uid/TP40012582-CH30-SW46)
  - Send an email to <mdwcoreteam@centurylink.com> requesting your device be registered.
  - When notified, download mdw.ipa from [Releases](https://github.com/CenturyLinkCloud/mdw-mobile/releases).
  - Install on your device using one of these options:
    - [Apple Configurator 2](https://help.apple.com/xcode/mac/current/#/devade83d1d7?sub=dev87a955931)
      - Follow these instructions to install: https://help.apple.com/xcode/mac/current/#/devade83d1d7?sub=dev87a955931
    - [iTunes](https://www.apple.com/itunes/)
      - Note: If your version of iTunes is 12.7 or later, install Apple's iTunes for business users:
        https://support.apple.com/en-us/HT208079
        (Unfortunately you must delete/rename existing itl files (https://discussions.apple.com/thread/8030764)
      - With your device connected, select `Apps` from the iTunes device dropdown menu (first selecting `Edit Menu` if necessary).
	  - Drag mdw.ipa from Explorer or Finder.
	  - Open the device in iTunes.  Select Summary > Apps, and click the Install button next to mdw.
	  
