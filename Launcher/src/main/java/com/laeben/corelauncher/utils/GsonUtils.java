package com.laeben.corelauncher.utils;

import com.laeben.corelauncher.data.entities.Account;
import com.laeben.corelauncher.minecraft.Wrapper;
import com.laeben.corelauncher.minecraft.modding.curseforge.entities.Image;
import com.laeben.corelauncher.minecraft.wrappers.Custom;
import com.laeben.corelauncher.minecraft.wrappers.Vanilla;
import com.laeben.corelauncher.minecraft.wrappers.fabric.Fabric;
import com.laeben.corelauncher.minecraft.wrappers.forge.Forge;
import com.laeben.corelauncher.minecraft.wrappers.optifine.OptiFine;
import com.laeben.corelauncher.minecraft.wrappers.quilt.Quilt;
import com.laeben.corelauncher.utils.entities.Java;
import com.laeben.core.entity.Path;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.laeben.corelauncher.utils.entities.PathFactory;

import java.lang.reflect.Type;

public class GsonUtils {
    private static final Gson EMPTY = new Gson();

    public static final Gson DEFAULT_GSON = new GsonBuilder()
            .registerTypeAdapter(Path.class, new PathFactory())
            .registerTypeAdapter(Account.class, new Account.AccountFactory())
            .registerTypeAdapter(Java.class, new Java.JavaFactory())
            .registerTypeAdapter(Wrapper.class, new Wrapper.WrapperFactory())
            .registerTypeAdapter(Forge.class, new Wrapper.WrapperFactory())
            .registerTypeAdapter(Vanilla.class, new Wrapper.WrapperFactory())
            .registerTypeAdapter(OptiFine.class, new Wrapper.WrapperFactory())
            .registerTypeAdapter(Fabric.class, new Wrapper.WrapperFactory())
            .registerTypeAdapter(Quilt.class, new Wrapper.WrapperFactory())
            .registerTypeAdapter(Custom.class, new Wrapper.WrapperFactory())
            .registerTypeAdapter(Image.class, new Image.ImageFactory())
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
