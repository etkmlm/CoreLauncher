package com.laeben.corelauncher.minecraft.modding.entity.resource;

import com.laeben.corelauncher.minecraft.modding.curseforge.entity.CurseForgeFile;
import com.laeben.corelauncher.minecraft.modding.curseforge.entity.CurseForgeResource;
import com.laeben.corelauncher.minecraft.modding.entity.ModpackContent;
import com.laeben.corelauncher.minecraft.modding.entity.ResourceType;
import com.laeben.corelauncher.minecraft.modding.modrinth.entity.ModrinthResource;
import com.laeben.corelauncher.minecraft.modding.modrinth.entity.Version;

public class Shader extends CResource implements ModpackContent {

    private Object mpId;

    public static Shader fromForgeResource(String vId, String loader, CurseForgeResource r, CurseForgeFile f){
        return fromForgeResource(new Shader(), vId, loader, r, f);
    }

    public static Shader fromRinthResource(ModrinthResource r, Version v){
        return fromRinthResource(new Shader(), r, v);
    }

    @Override
    public Object getModpackId() {
        return mpId;
    }

    @Override
    public void setModpackId(Object id) {
        mpId = id;
    }

    @Override
    public ResourceType getType(){
        return ResourceType.SHADER;
    }
}
