package com.laeben.corelauncher.util;

import com.google.gson.*;
import com.google.gson.annotations.Expose;
import com.laeben.core.entity.TranslationBundle;
import com.laeben.corelauncher.api.entity.Account;
import com.laeben.corelauncher.minecraft.Wrapper;
import com.laeben.corelauncher.minecraft.modding.curseforge.entity.Image;
import com.laeben.corelauncher.minecraft.modding.entity.resource.CResource;
import com.laeben.corelauncher.minecraft.modding.entity.LoaderType;
import com.laeben.corelauncher.minecraft.modding.entity.ModSource;
import com.laeben.corelauncher.minecraft.modding.entity.ResourceType;
import com.laeben.corelauncher.api.entity.Java;
import com.laeben.core.entity.Path;
import com.laeben.corelauncher.util.entity.PathFactory;

import java.lang.reflect.Type;

public class GsonUtil {
    public static final Gson EMPTY_GSON = new Gson();

    public static final Gson DEFAULT_GSON;

    static {
        var builder =  new GsonBuilder()
                .registerTypeAdapter(Path.class, new PathFactory())
                .registerTypeAdapter(Account.class, new Account.AccountFactory())
                .registerTypeAdapter(Java.class, new Java.JavaFactory())
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
                });

        var wrFact = new Wrapper.WrapperFactory();

        builder.registerTypeAdapter(Wrapper.class, wrFact);

        for (var l : LoaderType.values()){
            if (l.getCls() != null)
                builder.registerTypeAdapter(l.getCls(), wrFact);
        }

        var resFact = new CResource.CResourceFactory();

        builder.registerTypeAdapter(CResource.class, resFact);

        for (var x : ResourceType.values()){
            builder.registerTypeAdapter(x.getEntityClass(), resFact);
        }

        /*.registerTypeAdapter(Wrapper.class, wrFact);
        .registerTypeAdapter(Forge.class, new Wrapper.WrapperFactory())
        .registerTypeAdapter(NeoForge.class, new Wrapper.WrapperFactory())
        .registerTypeAdapter(Vanilla.class, new Wrapper.WrapperFactory())
        .registerTypeAdapter(OptiFine.class, new Wrapper.WrapperFactory())
        .registerTypeAdapter(Fabric.class, new Wrapper.WrapperFactory())
        .registerTypeAdapter(Quilt.class, new Wrapper.WrapperFactory())
        .registerTypeAdapter(Custom.class, new Wrapper.WrapperFactory())*/

        DEFAULT_GSON = builder.create();
    }

    /**
     * Shortcut for gson.fromJson()
     * @param obj target json element
     * @param type type of the deserialization context
     * @param <T> generic of the deserialization
     * @return context
     */
    public static <T> T fromJson(JsonElement obj, Type type){
        return EMPTY_GSON.fromJson(obj, type);
    }
}
