package com.laeben.corelauncher.utils.entities;

import com.google.gson.*;
import com.laeben.core.entity.Path;

import java.lang.reflect.Type;

public class PathFactory implements JsonSerializer<Path>, JsonDeserializer<Path> {
    @Override
    public JsonElement serialize(Path path, Type type, JsonSerializationContext jsonSerializationContext) {
        return new JsonPrimitive(path.toString());
    }

    @Override
    public Path deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return new Path(java.nio.file.Path.of(jsonElement.getAsString()));
    }
}