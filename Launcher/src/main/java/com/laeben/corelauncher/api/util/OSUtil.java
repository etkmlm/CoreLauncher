package com.laeben.corelauncher.api.util;

import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.entity.OS;
import com.laeben.corelauncher.api.entity.Java;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.file.Path;
import java.util.HashMap;

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

    public static byte[] getLocalIP(){
        byte[] ip = new byte[4];
        if (systemOS != OS.OSX){
            try(final DatagramSocket socket = new DatagramSocket()){
                socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
                ip = socket.getLocalAddress().getAddress();
            } catch (SocketException e) {
                Logger.getLogger().log(e);
            } catch (UnknownHostException ignored) {

            }
        }
        else{
            try(final Socket socket = new Socket()){
                socket.connect(new InetSocketAddress("google.com", 80));
                ip = socket.getLocalAddress().getAddress();
            } catch (UnknownHostException ignored) {

            } catch (IOException e) {
                Logger.getLogger().log(e);
            }
        }

        return ip;
    }

    public static String getHostName(){
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return null;
        }
    }

    public static void setClipboard(String text){
        Clipboard.getSystemClipboard().setContent(new HashMap<>(){{ put(DataFormat.PLAIN_TEXT, text); }});
    }
}
