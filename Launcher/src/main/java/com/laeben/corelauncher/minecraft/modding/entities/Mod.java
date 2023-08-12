package com.laeben.corelauncher.minecraft.modding.entities;

import com.laeben.corelauncher.minecraft.modding.curseforge.entities.ResourceForge;
import com.laeben.corelauncher.minecraft.modding.modrinth.entities.ResourceRinth;
import com.laeben.corelauncher.minecraft.modding.modrinth.entities.RinthFile;
import com.laeben.corelauncher.minecraft.modding.modrinth.entities.Version;

public class Mod extends CResource implements ModpackContent {
    private Object mpId;

    @Override
    public Object getModpackId() {
        return mpId;
    }

    @Override
    public void setModpackId(Object id) {
        mpId = id;
    }

    public static Mod fromForgeResource(String vId, String loader, ResourceForge r){
        return fromForgeResource(new Mod(), vId, loader, r);
    }

    public static Mod fromRinthResource(ResourceRinth r, Version v){
        return fromRinthResource(new Mod(), r, v);
    }
}
