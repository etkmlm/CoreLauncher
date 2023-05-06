package com.cdev.corelauncher.data.entities;

import com.cdev.corelauncher.data.Translator;

public class Translate {
    private final String key;
    private final Language language;
    private  final String value;

    public Translate(String key, Language lang, String value){
        this.key = key;
        this.language = lang;
        this.value = value;
    }

    public static Translate empty(){
        return new Translate(null, null, "");
    }

    public boolean check(Language l, String key){
        return this.key.equals(key) && this.language == l;
    }

    public String getValue(){
        return value;
    }
}
