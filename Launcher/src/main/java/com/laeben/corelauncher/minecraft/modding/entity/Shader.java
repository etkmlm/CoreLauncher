package com.laeben.corelauncher.minecraft.modding.entity;

import com.laeben.corelauncher.minecraft.modding.modrinth.entity.ResourceRinth;
import com.laeben.corelauncher.minecraft.modding.modrinth.entity.Version;

public class Shader extends CResource implements ModpackContent {

    private Object mpId;

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
