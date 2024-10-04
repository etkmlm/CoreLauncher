package com.laeben.corelauncher.api;

import java.net.MalformedURLException;
import java.net.URL;

public class Tool {
    /**
     * Get the resource from the class {@link Tool}.
     * @param name name of the resource
     * @return absolute url
     */
    public static URL getResource(String name){
        return Tool.class.getResource(name);
    }

    /**
     * Get the resource from the given class or
     * a new URL directs to the JAR file.
     * @param name name of the resource
     * @param clz source class for the loader
     * @return absolute url
     */
    public static <T> URL getResource(String name, Class<T> clz){
        var first = clz.getResource(name);
        if (first != null)
            return first;

        if (clz.getProtectionDomain() == null)
            return null;

        if (!name.startsWith("/"))
            name = "/" + name;

        try {
            return new URL("jar:file:///" + clz.getProtectionDomain().getCodeSource().getLocation().getPath() + "!" + name);
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
