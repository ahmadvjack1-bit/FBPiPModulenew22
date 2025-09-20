FB PiP LSPosed Module - README

Contents:
- settings.gradle
- app/build.gradle
- app/src/main/AndroidManifest.xml
- app/src/main/assets/xposed_init
- app/src/main/java/com/example/fbpip/FacebookPipModule.java

Quick instructions:
1) Install Android Studio (or use ./gradlew) and Java JDK 11+.
2) Put an xposed-api jar (api-82.jar) into app/libs/ if you want compile-time references, or remove the compileOnly line.
   You can find api-82.jar releases on GitHub (MALTF/XposedBridgeAPI) or Maven repos.
3) Open the project in Android Studio, build -> Build APK(s).
   Or run from project root: ./gradlew assembleDebug
4) Install the generated APK on your rooted device:
   adb install -r app/build/outputs/apk/debug/app-debug.apk
5) In LSPosed app on your device: enable the module, select target apps (Facebook), then reboot.
6) Test: open Facebook (com.facebook.katana), play a video, press Home. Check logcat:
   adb logcat | grep FBPiP

Notes and cautions:
- This module is a simple generic hook. Facebook may use obfuscated classes or block PiP inside its Activities.
- If the hook doesn't work, use jadx to inspect the Facebook APK and target specific classes/methods (e.g., VideoPlayer.pause).
- Always make a full device backup before experimenting with system-level modules.


Additional notes:
- This version includes expanded hooks targeting MediaPlayer, WebView and ExoPlayer methods to improve chances of keeping playback alive on Facebook v530.x.
- To build automatically, push this repository to GitHub and enable Actions; the workflow will produce the debug APK as an artifact.
- If the module doesn't work for your exact FB APK, upload the Facebook APK (com.facebook.katana v530.0.0.48.74) or provide it here and I will analyze it and adapt the hooks accordingly.
