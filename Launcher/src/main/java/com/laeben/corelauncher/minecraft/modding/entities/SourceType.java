package com.laeben.corelauncher.minecraft.modding.entities;

import java.util.Locale;

public enum SourceType {
    CURSEFORGE, MODRINTH;

    public String getId(){
        return this.name().toLowerCase(Locale.US);
    }
}
