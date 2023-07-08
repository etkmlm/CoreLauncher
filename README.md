<img src="logo.png" width="100" height="100"/>

# CoreLauncher
A minimalized, deeply customizable launcher for Minecraft.

## Why should I use CoreLauncher?
Because,
- It is fast.
- It is small.
- It is modular.
- It is open source.
- It contains the CurseForge and the Modrinth APIs.
- It built with useful and minimalized UI.
- It includes the most used modding APIs like Forge and Fabric.
- It supports CLI.
- It downloads the requiring Java itself.
- You can download multiple modpacks in one profile.
- You can see your worlds (including seeds, game mode and world's spawnpoint) in one, and resources (mods, resourcepacks, modpacks, and worlds) in one page.
- You can easily backup, import and share your profiles and worlds.
- Also, it supports the offline and online authentication.

Create profile, select you loader (wrapper), customize your resources, and play!

## How can I download it?
CoreLauncher works with Java 17, so first you have to download and add to the system path Java 17.

### Downloading Java 17
0) If you have earlier version of Java
   1) Delete it or
   2) Remove it from the system path variable or
   3) Use launch script to start the launcher
1) Go to [Java 17 Download Page](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
2) Download (JDK or JRE) and install it based on your OS
3) Check the path variable after install. (it should be like `.../jre|jdk 17.../bin`)

### Downloading Launcher
You can download the latest version of the launcher from it's website.
[Click Me]()!

### Launching Launcher
You have two options to launch.
1) Using a launch script. (Look below)
3) Clicking to the JAR twice. (Yup, like a normal app.)

#### Creating a Launch Script

`java -jar CoreLauncher.jar`

You can replace 'java' with custom Java 17 path. (`.../jre|jdk 17.../bin/java.exe`)

Create a launch script (for Unix .sh, for MacOS .command, for Windows .bat) and write this code into it.

In Unix, maybe you need to add 
```
#!/bin/sh
cd "$(dirname "$(readlink -fn "$0")")"
```
to the beggining of the launch script.

In MacOS, maybe you need to add 
```
#!/bin/bash
cd "$(dirname "$0")"
```
to the beggining of the launch script.

If you not changed the JAR path, the launch script and launcher must be in the same directory.


If you listen to my advice, you should create a new folder for the launcher, because the launcher creates a config.json and all of your settings are stored in this file. You don't want to lose it :)

## How can I use it?

### Main Page

