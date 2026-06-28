package com.laeben.corelauncher.api.ui.entity;

import com.google.gson.*;
import com.laeben.corelauncher.api.entity.ImageEntity;
import javafx.scene.paint.Color;

import java.lang.reflect.Type;

public class UIPreference {
    public static final String DOCK_SELECTION_COLOR = "__dockselcolor";

    public static class ColorFactory implements JsonSerializer<Color>, JsonDeserializer<Color> {

        @Override
        public Color deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonNull()) return null;

            try{
                return Color.web(json.getAsString());
            }
            catch (IllegalArgumentException ignored){}

            return null;
        }

        @Override
        public JsonElement serialize(Color src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive("#" + src.toString().substring(2));
        }
    }
    private String identifier;
    private Color customColor;
    private ImageEntity customImage;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public Color getCustomColor() {
        return customColor;
    }

    public void setCustomColor(Color customColor) {
        this.customColor = customColor;
    }

    public ImageEntity getCustomImage() {
        return customImage;
    }

    public void setCustomImage(ImageEntity customImage) {
        this.customImage = customImage;
    }
}
