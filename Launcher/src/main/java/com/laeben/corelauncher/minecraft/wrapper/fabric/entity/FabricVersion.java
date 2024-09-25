package com.laeben.corelauncher.minecraft.wrapper.fabric.entity;

public class FabricVersion extends BaseFabricVersion {

    public FabricVersion(){

    }

    public FabricVersion(String id, String wrId){
        super(id, wrId);
    }

    @Override
    public String getForkName() {
        return "fabric";
    }
}
