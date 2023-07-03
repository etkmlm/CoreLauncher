package com.cdev.corelauncher.ui.entities;

import com.cdev.corelauncher.data.Profiler;
import com.cdev.corelauncher.data.entities.Profile;
import com.cdev.corelauncher.minecraft.modding.curseforge.entities.Resource;
import com.cdev.corelauncher.minecraft.modding.entities.CResource;

import java.util.function.Consumer;

public class LMod {
    private final CResource res;
    private final Profile profile;
    private Consumer<LMod> onAction;
    public LMod(CResource res, Profile profile){
        this.res = res;
        this.profile = profile;
    }

    public CResource get(){
        return res;
    }
    public Profile getProfile(){
        return profile;
    }

    public Consumer<LMod> onAction(){
        return onAction;
    }

    public LMod setAction(Consumer<LMod> action){
        this.onAction = action;

        return this;
    }
}
