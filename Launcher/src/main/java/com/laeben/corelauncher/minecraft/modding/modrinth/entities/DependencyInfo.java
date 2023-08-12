package com.laeben.corelauncher.minecraft.modding.modrinth.entities;

public record DependencyInfo(String versionId, String loader, boolean includeDependencies) {
    public static DependencyInfo noDependencies(){
        return new DependencyInfo(null, null, false);
    }

    public static DependencyInfo includeDependencies(String vId, String loader){
        return new DependencyInfo(vId, loader, true);
    }
}
