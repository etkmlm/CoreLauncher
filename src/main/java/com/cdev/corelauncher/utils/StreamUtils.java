package com.cdev.corelauncher.utils;

import com.cdev.corelauncher.utils.entities.Path;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class StreamUtils {
    public static String inputStreamToString(InputStream s){
        String read = "{}";
        try (var stream = new BufferedInputStream(s);
             var buffer = new ByteArrayOutputStream();
        ){
            int r;
            while ((r = stream.read()) != -1){
                buffer.write(r);
            }

            read = buffer.toString(StandardCharsets.UTF_8);
        }
        catch (IOException e){
            Logger.getLogger().log(e);
        }

        return read;
    }
    public static String urlToString(String url){
        URL u;
        try {
            u = new URL(url);
        } catch (MalformedURLException e) {
            return null;
        }

        try{
            var c = (HttpURLConnection) u.openConnection();
            return inputStreamToString(c.getInputStream());
        }
        catch (IOException e){
            return null;
        }
    }
    public static long getContentLength(String url){
        try {
            var uri = new URL(url);
            var conn = (HttpURLConnection)uri.openConnection();
            return conn.getContentLengthLong();
        }
        catch (IOException e){
            Logger.getLogger().log(e);
            return 0;
        }
    }
    public static Path download(String url, Path destination, boolean useOriginalName, Consumer<Double> onProgress){
        try{
            var uri = new URL(url);
            if (useOriginalName){
                String[] p = uri.getFile().split("/");
                destination = destination.to(p[p.length - 1]);
            }
            destination.prepare();

            var conn = (HttpURLConnection) uri.openConnection();

            long length = conn.getContentLengthLong();
            long progress = 0;

            try(var stream = conn.getInputStream();
                var file = new FileOutputStream(destination.toFile())
            ){
                byte[] buffer = new byte[4096];
                int read;
                while ((read = stream.read(buffer)) != -1){
                    file.write(buffer, 0, read);
                    progress += buffer.length;
                    if (onProgress != null)
                        onProgress.accept(progress * 1.0 / length);
                }
            }
            destination.toFile().setLastModified(conn.getLastModified());
        }
        catch (IOException ex){
            Logger.getLogger().log(ex);
        }
        return destination;
    }
}
