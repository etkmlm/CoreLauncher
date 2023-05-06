package com.cdev.corelauncher.data.entities;

import com.google.gson.annotations.SerializedName;

public class Language {
    private String key;
    @SerializedName("local")
    private String localizedName;

    public Language(String key, String localizedName){
        this.key = key;
        this.localizedName = localizedName;
    }

    private Language(String key){
        this.key = key;
        this.localizedName = null;
    }

    public String getKey(){
        return key;
    }

    public String getLocalizedName(){
        return localizedName;
    }

    public static Language fromKey(String key){
        return new Language(key);
    }
}
