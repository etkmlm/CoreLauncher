package com.laeben.corelauncher.api.util;

import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.entity.OS;
import com.laeben.corelauncher.api.entity.Java;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

public class OSUtil {

    private static OS systemOS;

    public static Path getAppFolder(){
        return switch (systemOS){
            case WINDOWS -> Path.of(System.getenv("APPDATA"), ".corelauncher");
            case OSX -> Path.of(System.getProperty("user.home"), "Library", "Application Support", ".corelauncher");
            default -> Path.of(System.getProperty("user.home"), ".corelauncher");
        };
    }

    public static Path getRunningJavaDir(){
        return Path.of(System.getProperty("java.home"));
    }

    public static boolean is64BitJava(Java defaultJava){
        return defaultJava.identify() && defaultJava.arch == 64;
    }

    public static Path getJavaFile(String root, boolean preferWindow){
        if (systemOS == OS.WINDOWS){
            return Path.of(root, "bin", preferWindow ? "javaw.exe" : "java.exe");
        }
        else
            return Path.of(root, "bin", "java");
    }

    public static void openURL(String url) throws IOException {
        var runtime = Runtime.getRuntime();
        if (systemOS == OS.WINDOWS)
            runtime.exec("rundll32 url.dll,FileProtocolHandler " + url);
        else if (systemOS == OS.OSX)
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

    public static void open(File file){
        if (!Desktop.isDesktopSupported())
            return;

        new Thread(() -> {
            try {
                Desktop.getDesktop().open(file);
            } catch (IllegalArgumentException ignored){

            }
            catch (IOException e) {
                Logger.getLogger().log(e);
            }
        }).start();
    }

    public static void mailto(String mail){
        if (!Desktop.isDesktopSupported())
            return;

        new Thread(() -> {
            try {
                Desktop.getDesktop().mail(new URI("mailto:" + mail));
            } catch (IOException | URISyntaxException e) {
                Logger.getLogger().log(e);
            }
        }).start();
    }

    public static void setSystemOS(OS systemOS){
        OSUtil.systemOS = systemOS;
    }
}
