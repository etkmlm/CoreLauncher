package com.laeben.corelauncher.minecraft.modding.entity;

import com.laeben.corelauncher.minecraft.Wrapper;
import com.laeben.corelauncher.minecraft.modding.curseforge.entity.ForgeFile;
import com.laeben.corelauncher.minecraft.modding.curseforge.entity.ResourceForge;
import com.laeben.corelauncher.minecraft.modding.modrinth.entity.ResourceRinth;
import com.laeben.corelauncher.minecraft.modding.modrinth.entity.Version;

import java.util.List;

public class Modpack extends CResource {
    public transient Wrapper wr;
    public transient String wrId;
    public transient List<Mod> mods;
    public transient List<Resourcepack> resources;
    public transient List<Shader> shaders;

    public Modpack(ModSource.Type type){
        super(type);
    }

    public static Modpack fromForgeResource(String vId, String loader, ResourceForge r, ForgeFile f){
        return fromForgeResource(new Modpack(ModSource.Type.CURSEFORGE), vId, loader, r, f);
    }

    public static Modpack fromRinthResource(ResourceRinth r, Version v){
        return fromRinthResource(new Modpack(ModSource.Type.MODRINTH), r, v);
    }

    @Override
    public ResourceType getType(){
        return ResourceType.MODPACK;
    }
}
