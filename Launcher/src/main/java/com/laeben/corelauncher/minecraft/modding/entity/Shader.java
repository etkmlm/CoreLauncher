package com.laeben.corelauncher.minecraft.modding.entity;

import com.laeben.corelauncher.minecraft.modding.curseforge.entity.ForgeFile;
import com.laeben.corelauncher.minecraft.modding.curseforge.entity.ResourceForge;
import com.laeben.corelauncher.minecraft.modding.modrinth.entity.ResourceRinth;
import com.laeben.corelauncher.minecraft.modding.modrinth.entity.Version;

public class Shader extends CResource implements ModpackContent {

    private Object mpId;

    public static Shader fromForgeResource(String vId, String loader, ResourceForge r, ForgeFile f){
        return fromForgeResource(new Shader(), vId, loader, r, f);
    }

    public static Shader fromRinthResource(ResourceRinth r, Version v){
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
