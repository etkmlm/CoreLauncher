package com.laeben.corelauncher.minecraft.modding.modrinth;

import com.laeben.core.entity.RequestParameter;
import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.util.RequesterFactory;
import com.laeben.corelauncher.minecraft.modding.curseforge.entities.ClassType;
import com.laeben.corelauncher.minecraft.modding.entities.Mod;
import com.laeben.corelauncher.utils.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;

import java.util.ArrayList;
import java.util.List;

public class Modrinth {
    //private static final String API_KEY = "$2a$10$fdQjum78EUUUcJIw2a6gb.m1DNZCQzwvf0EBcfm.YgwIrmFX/1K3m";
    private static final String BASE_URL = "https://api.modrinth.com";
    private static Modrinth instance;
    private final Gson gson;
    private final RequesterFactory factory;
    private static final String SODIUM_ID = "AANobbMI";


    public Modrinth(){
        gson = GsonUtils.empty();

        factory = new RequesterFactory(BASE_URL);

        instance = this;
    }

    public static Modrinth getModrinth(){
        return instance;
    }

    public String getMod(String name, String fileName) throws NoConnectionException, HttpException {
        String str = factory.create()
                .to("/v2/project/" + name + "/version")
                .getString();

        var g = gson.fromJson(str, JsonArray.class);

        if (g == null)
            return null;

        for (var s : g){
            var obj = s.getAsJsonObject();
            for (var h : obj.get("files").getAsJsonArray()){
                var jgb = h.getAsJsonObject();
                if (jgb.get("filename").getAsString().equals(fileName))
                    return jgb.get("url").getAsString();
            }
        }

        return null;
    }


    public List<Mod> searchSodium(String loader, String vId) throws NoConnectionException, HttpException {
        String str = factory.create()
                .to("/v2/project/" + SODIUM_ID + "/version")
                .withParam(new RequestParameter("game_versions", "%5b%22" + vId + "%22%5d"))
                .withParam(new RequestParameter("loaders", "%5b%22" + loader + "%22%5d"))
                .getString();

        if (str == null)
            return List.of();

        var g = gson.fromJson(str, JsonArray.class);

        if (g == null)
            return List.of();

        var mods = new ArrayList<Mod>();

        for (var s : g){
            var obj = s.getAsJsonObject();

            var mod = new Mod();

            var f = obj.get("files").getAsJsonArray().get(0).getAsJsonObject();
            String fileName = f.get("filename").getAsString();
            String url = f.get("url").getAsString();

            String name = obj.get("name").getAsString();

            mod.classId = ClassType.MOD.getId();
            mod.name = name;
            mod.fileName = fileName;
            mod.fileUrl = url;
            mod.logoUrl = null;
            mods.add(mod);
        }

        return mods;

    }
}
