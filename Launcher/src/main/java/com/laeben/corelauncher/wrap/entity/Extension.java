package com.laeben.corelauncher.wrap.entity;

import com.laeben.corelauncher.LauncherConfig;
import com.laeben.corelauncher.wrap.exception.InvalidExtensionPropertiesException;
import javafx.scene.image.Image;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Extension {
    private String name;
    private String mainPackage;
    private String description;
    private String author;
    private String version;
    private String mainClass;
    private String target;


    private final transient List listeners;
    private transient URLClassLoader loader;
    private transient Image icon;

    private Extension(){
        listeners = new ArrayList();
    }

    public static Extension fromURLClassLoader(URLClassLoader loader) throws IllegalAccessException, IOException, InvalidExtensionPropertiesException {
        var extension = new Extension();

        var icon = loader.getResourceAsStream("icon.png");
        var stream = loader.getResourceAsStream("info.properties");
        var p = new Properties();
        p.load(stream);

        for (var f : Extension.class.getDeclaredFields()){
            if ((f.getModifiers() & Modifier.TRANSIENT) == Modifier.TRANSIENT)
                continue;
            if (!p.containsKey(f.getName()))
                return null;

            f.set(extension, p.get(f.getName()));
        }

        extension.loader = loader;
        extension.icon = icon == null ? null : new Image(icon);
        return extension;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public String getAuthor() {
        return author;
    }

    public String getVersion() {
        return version;
    }

    public String getPackage() {
        return mainPackage;
    }

    public String getMainClass() {
        return mainPackage + "." + mainClass;
    }

    public List<Object> getListeners() {
        return listeners;
    }

    public void registerListener(Object listener) {
        listeners.add(listener);
    }

    public String getClassIdentifier(String path) {
        return mainPackage + "." + path;
    }

    public URLClassLoader getClassLoader() {
        return loader;
    }

    public Image getIcon() {
        return icon;
    }

    public String getTarget(){
        return target;
    }

    public boolean isCompatible(){
        double launcherVer = LauncherConfig.VERSION;
        try{
            if (target.startsWith(">="))
                return launcherVer >= Double.parseDouble(target.substring(2));
            else if (target.startsWith(">"))
                return launcherVer > Double.parseDouble(target.substring(1));
            else if (target.startsWith("<="))
                return launcherVer <= Double.parseDouble(target.substring(2));
            else if (target.startsWith("<"))
                return launcherVer < Double.parseDouble(target.substring(1));
            else
                return launcherVer == Double.parseDouble(target);
        }
        catch (NumberFormatException ignored){
            return false;
        }
    }

}
