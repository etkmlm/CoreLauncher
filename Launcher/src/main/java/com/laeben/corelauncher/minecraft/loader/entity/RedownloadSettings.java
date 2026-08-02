package com.laeben.corelauncher.minecraft.loader.entity;

public class RedownloadSettings {
    private static final RedownloadSettings NONE = new RedownloadSettings();
    private static final RedownloadSettings ALL = new RedownloadSettings()
            .client()
            .libraries()
            .assets()
            .mods()
            .resourcePacks()
            .shaders();

    private boolean client;
    private boolean libraries;
    private boolean assets;
    private boolean mods;
    private boolean resourcePacks;
    private boolean shaders;

    public static RedownloadSettings all(){
        return ALL;
    }

    public static RedownloadSettings none(){
        return NONE;
    }

    public RedownloadSettings client(){
        this.client = true;
        return this;
    }
    public RedownloadSettings libraries(){
        this.libraries = true;
        return this;
    }
    public RedownloadSettings assets(){
        this.assets = true;
        return this;
    }
    public RedownloadSettings mods(){
        this.mods = true;
        return this;
    }
    public RedownloadSettings resourcePacks(){
        this.resourcePacks = true;
        return this;
    }
    public RedownloadSettings shaders(){
        this.shaders = true;
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
