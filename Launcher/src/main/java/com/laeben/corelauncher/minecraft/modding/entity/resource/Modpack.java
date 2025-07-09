package com.laeben.corelauncher.minecraft.modding.entity.resource;

import com.laeben.corelauncher.minecraft.Loader;
import com.laeben.corelauncher.minecraft.modding.curseforge.entity.CurseForgeFile;
import com.laeben.corelauncher.minecraft.modding.curseforge.entity.CurseForgeResource;
import com.laeben.corelauncher.minecraft.modding.entity.ModSource;
import com.laeben.corelauncher.minecraft.modding.entity.ResourceType;
import com.laeben.corelauncher.minecraft.modding.modrinth.entity.ModrinthResource;
import com.laeben.corelauncher.minecraft.modding.modrinth.entity.Version;

import java.util.List;

public class Modpack extends CResource {
    public transient Loader wr;
    public transient String targetVersionId;
    public transient String wrId;
    public transient List<Mod> mods;
    public transient List<Resourcepack> resources;
    public transient List<Shader> shaders;

    public Modpack(ModSource.Type type){
        super(type);
    }

    public static Modpack fromForgeResource(String vId, String loader, CurseForgeResource r, CurseForgeFile f){
        return fromForgeResource(new Modpack(ModSource.Type.CURSEFORGE), vId, loader, r, f);
    }

    public static Modpack fromRinthResource(ModrinthResource r, Version v){
        return fromRinthResource(new Modpack(ModSource.Type.MODRINTH), r, v);
    }

    @Override
    public ResourceType getType(){
        return ResourceType.MODPACK;
    }
}
