 # melonDS_hh version
 This is a version which is forked from https://github.com/rafaelvcaetano/melonDS-android, but add some special features.
 ## Update:
 
 1.0.4
 Add auto save
 An auto save will be stored in a new save slot, and it will be automatic loaded when a game is loaded.
 
 1.0.3
 Add save import function
 You can import save file from other emulators now(from Drastic or Desmume).
 1.0.2
 Merge from original project
 
 1.0.1
 * Add Chinese translation
 * Support usrcheat.dat cheat system
 * * Usage:
 * * * 1. Put usrcheat.dat file into the ROM folder.
 * * * 2. Enable the cheat function in emulator.
 * * * 3. Open the game, press back button, choose "usrcheat.dat" option
 * * Unfinish parts:
 * * * 1. The single choosen folder does not split, it means some cheats will show as mutilple choosen.
 * * * 2. All cheats in root folder will be placed in a folder named "root"
 
## [点击显示中文Readme](https://github.com/huhao1987/melonDS-android/blob/hh/README_CN.md)

## Original melonDS version details
### What is working
*  Device scanning for ROMS
*  Games can boot and run
*  Sound
*  Input
*  Mic input
*  Game saves
*  Save states
*  Rewind
*  AR cheats
*  GBA ROM support
*  DSi support (experimental)
*  Controller support
*  Customizable layouts
*  Settings

### What is missing
*  Wi-Fi
*  OpenGL renderer
*  Customizable button skins
*  More display filters

### Performance
Performance is solid on 64 bit devices with thread rendering and JIT enabled, and should run at full speed on flagship devices. Performance on older devices, specially
32 bit devices, is very poor due to the lack of JIT support.

### Integration with third party frontends
It's possible to launch melonDS from third part frontends. For that, you simply need to call the emulation activity with the absolute path to the ROM file. The parameters are the following:
*  Package name: `me.magnum.melonds`
*  Activity name: `me.magnum.melonds.ui.emulator.EmulatorActivity`
*  Parameters:
    * `PATH` - a string with the absolute path to the NDS ROM (ZIP files are supported)

### Building
To build the project you will need Android SDK, NDK and CMake.

## Build steps:
1.  Clone the project, including submodules with:
    
    `git clone --recurse-submodules https://github.com/rafaelvcaetano/melonDS-android.git`
2.  Install the Android SDK, NDK and CMake
3.  Build with:
    1.  Unix: `./gradlew :app:assembleGitHubRelease`
    2.  Windows: `gradlew.bat :app:assembleGitHubRelease`
4.  The generated APK can be found at `app/gitHub/release`
