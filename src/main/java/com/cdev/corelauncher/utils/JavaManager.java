package com.cdev.corelauncher.utils;

import com.cdev.corelauncher.CoreLauncher;
import com.cdev.corelauncher.utils.entities.Java;
import com.cdev.corelauncher.utils.entities.Path;
import com.google.gson.Gson;
import com.google.gson.JsonArray;

import java.util.List;
import java.util.function.Consumer;

public class JavaManager {
    private static final String ADOPTIUM = "https://api.adoptium.net/v3/assets/latest/";

    private static JavaManager instance;

    private Path javaDir;

    public JavaManager(Path javaDir){
        this.javaDir = javaDir;
        instance = this;
    }

    public void setJavaDir(Path javaDir){
        this.javaDir = javaDir;
    }

    public static JavaManager getManager(){
        return instance;
    }

    public List<Java> getAll(){
        var files = javaDir.getFiles();
        return files.stream().map(x -> new Java(Path.begin(OSUtils.getJavaFile(x.toString())))).toList();
    }

    public static Java getDefault(){
        return new Java(Path.begin(OSUtils.getRunningJavaFile()));
    }

    private static String getJavaURL(Java j, boolean is64Bit){
        var os = CoreLauncher.SYSTEM_OS;
        String url = ADOPTIUM + j.majorVersion + "/hotspot?os=" + os.getName() + "&image_type=jdk&architecture=" + (is64Bit ? "x64" : "x86");
        var object = new Gson().fromJson(StreamUtils.urlToString(url), JsonArray.class).get(0);
        if (object == null)
            return null;
        return object.getAsJsonObject().getAsJsonObject("binary").getAsJsonObject("package").get("link").getAsString();
    }

    public Java tryGet(Java j){
        return getAll().stream().filter(x -> x.majorVersion == j.majorVersion).findFirst().orElse(null);
    }

    public void download(Java java, Consumer<Double> onProgress){
        if (java.majorVersion == 0)
            return;

        String url = getJavaURL(java, true);

        if (url == null)
            return;

        var file = StreamUtils.download(url, javaDir, true, onProgress);
        file.extract(null, null);
        file.delete();
    }
}
