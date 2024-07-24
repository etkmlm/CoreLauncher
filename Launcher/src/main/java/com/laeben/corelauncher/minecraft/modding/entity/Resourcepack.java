package com.laeben.corelauncher.minecraft.modding.entity;

import com.laeben.corelauncher.minecraft.modding.curseforge.entity.ForgeFile;
import com.laeben.corelauncher.minecraft.modding.curseforge.entity.ResourceForge;
import com.laeben.corelauncher.minecraft.modding.modrinth.entity.ResourceRinth;
import com.laeben.corelauncher.minecraft.modding.modrinth.entity.Version;

public class Resourcepack extends CResource implements ModpackContent{
    private Object mpId;

    @Override
    public Object getModpackId() {
        return mpId;
    }

    @Override
    public void setModpackId(Object id) {
        mpId = id;
    }

    public static Resourcepack fromForgeResource(String vId, String loader, ResourceForge r, ForgeFile f){
        return fromForgeResource(new Resourcepack(), vId, loader, r, f);
    }

    public static Resourcepack fromRinthResource(ResourceRinth r, Version v){
        return fromRinthResource(new Resourcepack(), r, v);
    }

    @Override
    public ResourceType getType(){
        return ResourceType.RESOURCE;
    }
}
