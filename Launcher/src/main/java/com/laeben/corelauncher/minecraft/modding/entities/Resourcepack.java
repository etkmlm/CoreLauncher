package com.laeben.corelauncher.minecraft.modding.entities;

import com.laeben.corelauncher.minecraft.modding.curseforge.entities.ResourceForge;
import com.laeben.corelauncher.minecraft.modding.modrinth.entities.ResourceRinth;
import com.laeben.corelauncher.minecraft.modding.modrinth.entities.Version;

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

    public static Resourcepack fromForgeResource(String vId, String loader, ResourceForge r){
        return fromForgeResource(new Resourcepack(), vId, loader, r);
    }

    public static Resourcepack fromRinthResource(ResourceRinth r, Version v){
        return fromRinthResource(new Resourcepack(), r, v);
    }
}
