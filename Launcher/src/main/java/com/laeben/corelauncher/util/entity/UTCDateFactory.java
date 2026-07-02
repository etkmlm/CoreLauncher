package com.laeben.corelauncher.util.entity;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;

public class UTCDateFactory implements JsonSerializer<Date>, JsonDeserializer<Date> {
    @Override
    public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return Date.from(Instant.parse(json.getAsString()));
    }

    @Override
    public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
        return src == null ? null : new JsonPrimitive(src.toInstant().atZone(ZoneOffset.UTC).toString());
    }
}
