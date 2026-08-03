package com.laeben.corelauncher.minecraft.loader.entity;

public class RedownloadSettings {
    private boolean client;
    private boolean libraries;
    private boolean assets;
    private boolean mods;
    private boolean resourcePacks;
    private boolean shaders;

    public static RedownloadSettings all(){
        return new RedownloadSettings()
                .client(true)
                .libraries(true)
                .assets(true)
                .mods(true)
                .resourcePacks(true)
                .shaders(true);
    }

    public static RedownloadSettings none(){
        return new RedownloadSettings();
    }

    public RedownloadSettings client(boolean value){
        this.client = value;
        return this;
    }
    public RedownloadSettings libraries(boolean value){
        this.libraries = value;
        return this;
    }
    public RedownloadSettings assets(boolean value){
        this.assets = value;
        return this;
    }
    public RedownloadSettings mods(boolean value){
        this.mods = value;
        return this;
    }
    public RedownloadSettings resourcePacks(boolean value){
        this.resourcePacks = value;
        return this;
    }
    public RedownloadSettings shaders(boolean value){
        this.shaders = value;
        return this;
    }

    public boolean hasClient(){
        return client;
    }
    public boolean hasLibraries(){
        return libraries;
    }
    public boolean hasAssets(){
        return assets;
    }
    public boolean hasMods(){
        return mods;
    }
    public boolean hasResourcePacks(){
        return resourcePacks;
    }
    public boolean hasShaders(){
        return shaders;
    }

}
