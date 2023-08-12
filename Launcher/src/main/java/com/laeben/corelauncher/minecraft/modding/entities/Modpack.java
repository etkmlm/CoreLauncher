package com.laeben.corelauncher.minecraft.modding.entities;

import com.laeben.corelauncher.minecraft.Wrapper;
import com.laeben.corelauncher.minecraft.modding.curseforge.entities.ResourceForge;
import com.laeben.corelauncher.minecraft.modding.modrinth.entities.ResourceRinth;
import com.laeben.corelauncher.minecraft.modding.modrinth.entities.Version;

import java.util.List;

public class Modpack extends CResource {
    public transient Wrapper wr;
    public transient String wrId;
    public transient List<Mod> mods;
    public transient List<Resourcepack> resources;
    public transient List<Shader> shaders;

    public Modpack(SourceType type){
        super(type);
    }

    public static Modpack fromForgeResource(String vId, String loader, ResourceForge r){
        return fromForgeResource(new Modpack(SourceType.CURSEFORGE), vId, loader, r);
    }

    public static Modpack fromRinthResource(ResourceRinth r, Version v){
        return fromRinthResource(new Modpack(SourceType.MODRINTH), r, v);
    }
}
