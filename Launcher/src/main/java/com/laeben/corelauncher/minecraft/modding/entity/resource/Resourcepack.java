package com.laeben.corelauncher.minecraft.modding.entity.resource;

import com.laeben.corelauncher.minecraft.modding.curseforge.entity.CurseForgeFile;
import com.laeben.corelauncher.minecraft.modding.curseforge.entity.CurseForgeResource;
import com.laeben.corelauncher.minecraft.modding.entity.ModpackContent;
import com.laeben.corelauncher.minecraft.modding.entity.ResourceType;
import com.laeben.corelauncher.minecraft.modding.modrinth.entity.ModrinthResource;
import com.laeben.corelauncher.minecraft.modding.modrinth.entity.Version;

public class Resourcepack extends CResource implements ModpackContent {
    private Object mpId;

    @Override
    public Object getModpackId() {
        return mpId;
    }

    @Override
    public void setModpackId(Object id) {
        mpId = id;
    }

    public static Resourcepack fromForgeResource(String vId, String loader, CurseForgeResource r, CurseForgeFile f){
        return fromForgeResource(new Resourcepack(), vId, loader, r, f);
    }

    public static Resourcepack fromRinthResource(ModrinthResource r, Version v){
        return fromRinthResource(new Resourcepack(), r, v);
    }

    @Override
    public ResourceType getType(){
        return ResourceType.RESOURCEPACK;
    }
}
