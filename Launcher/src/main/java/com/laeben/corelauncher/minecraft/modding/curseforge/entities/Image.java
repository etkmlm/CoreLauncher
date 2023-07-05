package com.laeben.corelauncher.minecraft.modding.curseforge.entities;

import com.google.gson.*;

import java.lang.reflect.Type;

public class Image {
    public static class ImageFactory implements JsonDeserializer<Image>, JsonSerializer<Image> {

        @Override
        public Image deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            return new Image(jsonElement.getAsString());
        }

        @Override
        public JsonElement serialize(Image image, Type type, JsonSerializationContext jsonSerializationContext) {
            return new JsonPrimitive(image.url);
        }
    }
    public int id;
    public int modId;
    public String title;
    public String description;
    public String thumbnailUrl;
    public String url;

    public Image(){

    }

    public Image(String url){
        this.url = url;
    }
}
