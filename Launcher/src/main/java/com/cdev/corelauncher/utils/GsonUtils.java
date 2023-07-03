package com.cdev.corelauncher.utils;

import com.cdev.corelauncher.data.entities.Account;
import com.cdev.corelauncher.minecraft.Wrapper;
import com.cdev.corelauncher.minecraft.modding.curseforge.entities.Image;
import com.cdev.corelauncher.minecraft.wrappers.Custom;
import com.cdev.corelauncher.minecraft.wrappers.Vanilla;
import com.cdev.corelauncher.minecraft.wrappers.fabric.Fabric;
import com.cdev.corelauncher.minecraft.wrappers.forge.Forge;
import com.cdev.corelauncher.minecraft.wrappers.optifine.OptiFine;
import com.cdev.corelauncher.minecraft.wrappers.quilt.Quilt;
import com.cdev.corelauncher.utils.entities.Java;
import com.cdev.corelauncher.utils.entities.Path;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import java.lang.reflect.Type;

public class GsonUtils {
    private static final Gson EMPTY = new Gson();

    public static final Gson DEFAULT_GSON = new GsonBuilder()
            .registerTypeAdapter(Path.class, new Path.PathFactory())
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

    public static <T> T fromJson(JsonElement obj, Type type){
        return empty().fromJson(obj, type);
    }

    public static Gson empty(){
        return EMPTY;
    }
}
