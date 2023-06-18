package com.cdev.corelauncher.utils;

import com.cdev.corelauncher.CoreLauncher;
import com.cdev.corelauncher.utils.entities.OS;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;

public class OSUtils {
    public static Path getAppFolder(){
        var os = CoreLauncher.SYSTEM_OS;
        return switch (os){
            case WINDOWS -> Path.of(System.getenv("APPDATA"), "corelauncher");
            case OSX -> Path.of(System.getProperty("user.home"), "Library", "Application Support", "corelauncher");
            default -> Path.of(System.getProperty("user.home"), "corelauncher");
        };
    }

    public static Path getRunningJavaDir(){
        return Path.of(System.getProperty("java.home"));
    }

    public static boolean is64BitOS(){
        var j = JavaMan.getDefault();
        return j.retrieveInfo() && j.arch == 64;
    }

    public static Path getJavaFile(String root){
        var os = CoreLauncher.SYSTEM_OS;

        if (os == OS.WINDOWS){
            return Path.of(root, "bin", "java.exe");
        }
        else
            return Path.of(root, "bin", "java");
    }

    public static void openURL(String url){
        var os = CoreLauncher.SYSTEM_OS;

        try{
            var runtime = Runtime.getRuntime();
            if (os == OS.WINDOWS)
                runtime.exec("rundll32 url.dll,FileProtocolHandler " + url);
            else if (os == OS.OSX)
                runtime.exec("open " + url);
            else{
                String[] browsers = { "google-chrome", "firefox", "mozilla", "epiphany", "konqueror",
                        "netscape", "opera", "links", "lynx" };

                var cmd = new StringBuilder();
                for (int i = 0; i < browsers.length; i++)
                    if(i == 0)
                        cmd.append(String.format(    "%s \"%s\"", browsers[i], url));
                    else
                        cmd.append(String.format(" || %s \"%s\"", browsers[i], url));

                runtime.exec(new String[] { "sh", "-c", cmd.toString() });
            }
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }
    }

    public static void openFolder(Path path){
        try {
            Desktop.getDesktop().open(path.toFile());
        } catch (IOException e) {
            Logger.getLogger().log(e);
        }
    }
}
