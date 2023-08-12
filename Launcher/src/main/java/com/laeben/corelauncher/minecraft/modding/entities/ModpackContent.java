package com.laeben.corelauncher.minecraft.modding.entities;

public interface ModpackContent {
    Object getModpackId();
    void setModpackId(Object id);

    default boolean belongs(Modpack mp){
        return getModpackId() != null && getModpackId().equals(mp.id);
    }
}
