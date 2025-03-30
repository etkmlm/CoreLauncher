package com.laeben.corelauncher.minecraft.modding.entity;

import com.laeben.corelauncher.minecraft.modding.entity.resource.Modpack;

public interface ModpackContent {
    Object getModpackId();
    void setModpackId(Object id);

    default boolean belongs(Modpack mp){
        return getModpackId() != null && getModpackId().equals(mp.id);
    }
}
