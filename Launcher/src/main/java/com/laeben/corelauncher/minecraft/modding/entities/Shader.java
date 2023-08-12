package com.laeben.corelauncher.minecraft.modding.entities;

import com.laeben.corelauncher.minecraft.modding.modrinth.entities.ResourceRinth;
import com.laeben.corelauncher.minecraft.modding.modrinth.entities.Version;

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
}
