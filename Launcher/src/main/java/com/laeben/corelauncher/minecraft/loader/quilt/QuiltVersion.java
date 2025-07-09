package com.laeben.corelauncher.minecraft.loader.quilt;

import com.laeben.corelauncher.minecraft.loader.fabric.entity.BaseFabricVersion;

public class QuiltVersion extends BaseFabricVersion {

    public QuiltVersion(){

    }

    public QuiltVersion(String id, String wrId){
        super(id, wrId);
    }

    @Override
    public String getForkName() {
        return "quilt";
    }
}
