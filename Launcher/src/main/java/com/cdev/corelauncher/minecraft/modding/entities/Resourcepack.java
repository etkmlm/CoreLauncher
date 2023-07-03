package com.cdev.corelauncher.minecraft.modding.entities;

import com.cdev.corelauncher.minecraft.modding.curseforge.entities.Resource;

public class Resourcepack extends CResource{
    public int mpId;
    public static Resourcepack fromResource(String vId, String loader, Resource r){
        var pack = fromResource(new Resourcepack(), vId, loader, r);

        return pack;
    }
}
