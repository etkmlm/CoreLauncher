package com.cdev.corelauncher.utils;

import com.cdev.corelauncher.CoreLauncher;
import com.cdev.corelauncher.utils.entities.OS;

import java.nio.file.Path;

public class OSUtils {
    public static Path getAppFolder(){
        var os = CoreLauncher.SYSTEM_OS;

        if (os == OS.WINDOWS){
            return Path.of(System.getenv("APPDATA"), "corelauncher");
        }
        else if (os == OS.OSX){
            return Path.of(System.getProperty("user.home"), "Library", "Application Support", "corelauncher");
        }
        else
            return Path.of(System.getProperty("user.home"), "corelauncher");
    }

    public static Path getRunningJavaFile(){
        return getJavaFile(System.getProperty("java.home"));
    }

    public static Path getJavaFile(String root){
        var os = CoreLauncher.SYSTEM_OS;

        if (os == OS.WINDOWS){
            return Path.of(root, "bin", "java.exe");
        }
        else
            return Path.of(root, "bin", "java");
    }
}
