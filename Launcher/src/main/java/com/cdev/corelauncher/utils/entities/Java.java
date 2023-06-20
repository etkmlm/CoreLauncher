package com.cdev.corelauncher.utils.entities;

import com.cdev.corelauncher.utils.Logger;
import com.cdev.corelauncher.utils.OSUtils;
import com.google.gson.*;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.lang.reflect.Type;

public class Java {
    public static class JavaFactory implements JsonSerializer<Java>, JsonDeserializer<Java>{

        @Override
        public Java deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            var obj = jsonElement.getAsJsonObject();
            if (!obj.has("path"))
                return null;
            String name = null;
            if (obj.has("name"))
                name = obj.get("name").getAsString();
            return new Java(name == null ? "null" : name, Path.begin(java.nio.file.Path.of(obj.get("path").getAsString())));
        }

        @Override
        public JsonElement serialize(Java java, Type type, JsonSerializationContext jsonSerializationContext) {
            var obj = new JsonObject();
            obj.add("name", new JsonPrimitive(java.name == null ? "" : java.name));
            obj.add("path", new JsonPrimitive(java.path != null ? java.path.toString() : String.valueOf(java.majorVersion)));

            return obj;
        }
    }
    @SerializedName("component")
    private String codeName;
    public int majorVersion;
    public int arch;
    public String version;
    private String name;
    private Path path;
    private boolean loaded;

    public Java(){
        path = null;

        loaded = false;
    }

    public Java(int majorVersion){
        this.majorVersion = majorVersion;
        path = null;

        loaded = false;
    }

    public Java(String name){
        this.name = name;
        path = null;
        loaded = false;
    }

    public Java(String name, Path path){
        this.name = name;
        this.path = path;

        retrieveInfo();
    }

    public Java(Path path){
        this.path = path;

        retrieveInfo();
    }

    public boolean retrieveInfo(){
        if (path == null)
            return loaded = false;

        try{
            var process = new ProcessBuilder().command(getExecutable().toString(), "-version").start();

            String version;
            String arch;
            try(var reader = process.errorReader()){
                version = reader.readLine();
                reader.readLine();
                arch = reader.readLine();
            }

            String[] a = version.split(" ");
            this.version = a[2].replace("\"", "");
            String[] b = this.version.split("\\.");
            majorVersion = Integer.parseInt(b[0].equals("1") ? b[1] : b[0]);
            this.arch = arch.contains("64-Bit") ? 64 : 32;

            return loaded = true;
        }
        catch (IOException e){
            Logger.getLogger().log(e);

            return loaded = false;
        }
    }

    public String getCodeName(){
        if (codeName != null)
            return codeName;

        if (majorVersion == 17)
            codeName = "java-runtime-alpha";
        else if (majorVersion == 16)
            codeName = "java-runtime-gamma";
        else
            codeName = "jre-legacy";

        return codeName;
    }

    public static Java fromVersion(int majorVersion){
        return new Java(majorVersion);
    }

    public static Java fromCodeName(String codeName){
        if (codeName.contains("alpha") || codeName.contains("beta"))
            return new Java(17);
        else if (codeName.equals("gamma"))
            return new Java(16);
        else
            return new Java(8);
    }

    public boolean isLoaded(){
        return loaded;
    }

    public Java setPath(Path path){
        this.path = path;

        return this;
    }

    public Path getPath(){
        return path;
    }

    public boolean isInitial(){
        return name == null;
    }

    public String getName(){
        return name == null ? "JDK " + majorVersion : name;
    }

    public Java setName(String name){
        this.name = name;

        return this;
    }

    public boolean isEmpty(){
        return path == null;
    }

    public String toIdentifier(){
        return getName() + " - " + majorVersion;
    }

    public Path getExecutable(){
        return Path.begin(OSUtils.getJavaFile(path.toString()));
    }

    @Override
    public boolean equals(Object obj){
        return obj instanceof Java j && path != null && path.equals(j.path);
    }
}
