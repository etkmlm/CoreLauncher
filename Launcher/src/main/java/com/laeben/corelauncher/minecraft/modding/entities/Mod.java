package com.laeben.corelauncher.minecraft.modding.entities;

import com.laeben.corelauncher.minecraft.modding.curseforge.entities.Resource;

public class Mod extends CResource {
    public int mpId;

    public static Mod fromResource(String vId, String loader, Resource r){
        var mod = fromResource(new Mod(), vId, loader, r);

        return mod;
    }

}
