package com.laeben.corelauncher.minecraft.wrappers.forge.entities;

import java.util.Arrays;

public class FArtifact {
    public enum Type{
        CHANGELOG("Changelog"), MDK("Mdk"), SOURCE("Src"), INSTALLER("Installer"), CLIENT("Client"), SERVER("Server"), UNIVERSAL("Universal"), UNKNOWN("");

        private final String title;
        Type(String title){
            this.title = title;
        }

        public static Type fromTitle(String title){
            return Arrays.stream(Type.class.getEnumConstants()).filter(x -> x.title.equals(title)).findFirst().orElse(Type.UNKNOWN);
        }
    }
    public Type type;
    private String url;

    public FArtifact(){

    }

    public String getUrl(){
        return url.contains("adfoc.us") ? url.split("url=")[1] : url;
    }

    public FArtifact(String title, String url){
        this.type = Type.fromTitle(title);
        this.url = url;
    }
}
