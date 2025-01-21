package com.laeben.corelauncher.util;

import com.google.gson.*;
import com.google.gson.annotations.Expose;
import com.laeben.core.entity.TranslationBundle;
import com.laeben.corelauncher.api.entity.Account;
import com.laeben.corelauncher.minecraft.Wrapper;
import com.laeben.corelauncher.minecraft.modding.curseforge.entity.Image;
import com.laeben.corelauncher.minecraft.modding.entity.ModSource;
import com.laeben.corelauncher.minecraft.wrapper.Custom;
import com.laeben.corelauncher.minecraft.wrapper.Vanilla;
import com.laeben.corelauncher.minecraft.wrapper.fabric.Fabric;
import com.laeben.corelauncher.minecraft.wrapper.forge.Forge;
import com.laeben.corelauncher.minecraft.wrapper.neoforge.NeoForge;
import com.laeben.corelauncher.minecraft.wrapper.optifine.OptiFine;
import com.laeben.corelauncher.minecraft.wrapper.quilt.Quilt;
import com.laeben.corelauncher.api.entity.Java;
import com.laeben.core.entity.Path;
import com.laeben.corelauncher.util.entity.PathFactory;

import java.lang.reflect.Type;

public class GsonUtil {
    private static final Gson EMPTY = new Gson();

    public static final Gson DEFAULT_GSON = new GsonBuilder()
            .registerTypeAdapter(Path.class, new PathFactory())
            .registerTypeAdapter(Account.class, new Account.AccountFactory())
            .registerTypeAdapter(Java.class, new Java.JavaFactory())
            .registerTypeAdapter(Wrapper.class, new Wrapper.WrapperFactory())
            .registerTypeAdapter(Forge.class, new Wrapper.WrapperFactory())
            .registerTypeAdapter(NeoForge.class, new Wrapper.WrapperFactory())
            .registerTypeAdapter(Vanilla.class, new Wrapper.WrapperFactory())
            .registerTypeAdapter(OptiFine.class, new Wrapper.WrapperFactory())
            .registerTypeAdapter(Fabric.class, new Wrapper.WrapperFactory())
            .registerTypeAdapter(Quilt.class, new Wrapper.WrapperFactory())
            .registerTypeAdapter(Custom.class, new Wrapper.WrapperFactory())
            .registerTypeAdapter(Image.class, new Image.ImageFactory())
            .registerTypeAdapter(TranslationBundle.class, new TranslationBundle.TranslationBundleFactory())
            .registerTypeAdapter(ModSource.TypeFactory.class, new ModSource.TypeFactory())
            .addSerializationExclusionStrategy(new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                    return fieldAttributes.getAnnotation(Expose.class) != null;
                }

                @Override
                public boolean shouldSkipClass(Class<?> aClass) {
                    return false;
                }
            })
            .addDeserializationExclusionStrategy(new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                    return fieldAttributes.getAnnotation(Expose.class) != null;
                }

                @Override
                public boolean shouldSkipClass(Class<?> aClass) {
                    return false;
                }
            })
            .create();

    /**
     * Shortcut for gson.fromJson()
     * @param obj target json element
     * @param type type of the deserialization context
     * @param <T> generic of the deserialization
     * @return context
     */
    public static <T> T fromJson(JsonElement obj, Type type){
        return empty().fromJson(obj, type);
    }

    public static Gson empty(){
        return EMPTY;
    }
}
