package com.laeben.corelauncher.api.gpu.entity;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.laeben.corelauncher.api.entity.OS;

import java.lang.reflect.Type;
import java.util.Objects;

@JsonAdapter(GPUType.GPUTypeFactory.class)
public record GPUType(String id) {
    public static class GPUTypeFactory implements JsonSerializer<GPUType>, JsonDeserializer<GPUType> {

        @Override
        public GPUType deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return !json.isJsonNull() ? new GPUType(json.getAsString()) : DEFAULT;
        }

        @Override
        public JsonElement serialize(GPUType src, Type typeOfSrc, JsonSerializationContext context) {
            return src == null || src.equals(DEFAULT) ? JsonNull.INSTANCE : new JsonPrimitive(src.id);
        }
    }

    public static final GPUType DEFAULT = new GPUType(null);

    /**
     * @return os-specific gpu number, default (null) -> not supported
     */
    public String getId(OS os){
        return switch (os){
            case WINDOWS -> {
                if (id.equals("1")) yield id; // primary
                else if (id.equals("2")) yield id; // high performance
                else yield "0"; // auto
            }
            case LINUX -> id;
            default -> DEFAULT.id;
        };
    }

    @Override
    public boolean equals(Object o){
        return o == this || o == null && id == null || o instanceof GPUType t && (Objects.equals(t.id, id));
    }

    @Override
    public int hashCode(){
        return Objects.hash(id);
    }
}
