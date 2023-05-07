package com.cdev.corelauncher.data.entities;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;

public class Language {

    public static final class LanguageFactory implements JsonSerializer<Language>, JsonDeserializer<Language> {

        @Override
        public Language deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            return Language.fromKey(jsonElement.getAsString());
        }

        @Override
        public JsonElement serialize(Language languageFactory, Type type, JsonSerializationContext jsonSerializationContext) {
            return new JsonPrimitive(languageFactory.key);
        }
    }

    private final String key;
    @SerializedName("local")
    private final String localizedName;

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
