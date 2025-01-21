package com.laeben.corelauncher.api.entity;

import com.laeben.core.entity.Path;
import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.laeben.corelauncher.api.util.OSUtil;
import com.laeben.corelauncher.util.entity.LogType;

import java.io.IOException;
import java.lang.reflect.Type;

public class Java {
    public static class JavaFactory implements JsonSerializer<Java>, JsonDeserializer<Java>{

        @Override
        public Java deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            String path;
            String name = null;
            if (jsonElement.isJsonObject()){
                var obj = jsonElement.getAsJsonObject();
                if (!obj.has("path"))
                    return null;

                path = obj.get("path").getAsString();

                if (obj.has("name"))
                    name = obj.get("name").getAsString();
            }
            else
                path = jsonElement.getAsString();

            return new Java(name, Path.begin(java.nio.file.Path.of(path)));
        }

        @Override
        public JsonElement serialize(Java java, Type type, JsonSerializationContext jsonSerializationContext) {
            var obj = new JsonObject();
            if (java.getName() != null)
                obj.addProperty("name", java.getName());
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

        identify();
    }

    public Java(Path path){
        this.path = path;

        identify();
    }

    /**
     * Identifies the Java from the path.
     * @return is process successfull
     */
    public boolean identify() {
        if (path == null)
            return loaded = false;

        String version = null;
        String arch;

        try{
            var process = new ProcessBuilder()
                    .command(getExecutable().toString(), "-version")
                    .start();

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
        catch (NumberFormatException e){
            Logger.getLogger().log(LogType.ERROR, "Error while trying to identify Java version: " + version + "\nPath: " + path);
            return loaded = false;
        }
        catch (IOException e){
            return loaded = false;
        }
    }

    /**
     * @return minecraft code name of the Java
     */
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

    public Path getWindowExecutable(){
        return Path.begin(OSUtil.getJavaFile(path.toString(), true));
    }

    public Path getExecutable(){
        return Path.begin(OSUtil.getJavaFile(path.toString(), false));
    }

    @Override
    public boolean equals(Object obj){
        return obj instanceof Java j && path != null && path.equals(j.path);
    }
}
