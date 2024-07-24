package com.laeben.corelauncher.minecraft.modding.entity;

import com.laeben.corelauncher.minecraft.modding.curseforge.entity.ForgeFile;
import com.laeben.corelauncher.minecraft.modding.curseforge.entity.ResourceForge;
import com.laeben.corelauncher.minecraft.modding.modrinth.entity.ResourceRinth;
import com.laeben.corelauncher.minecraft.modding.modrinth.entity.Version;

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

    public static Mod fromForgeResource(String vId, String loader, ResourceForge r, ForgeFile f){
        return fromForgeResource(new Mod(), vId, loader, r, f);
    }

    public static Mod fromRinthResource(ResourceRinth r, Version v){
        return fromRinthResource(new Mod(), r, v);
    }
}
