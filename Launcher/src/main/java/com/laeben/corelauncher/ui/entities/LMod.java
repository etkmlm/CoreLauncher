package com.laeben.corelauncher.ui.entities;

import com.laeben.corelauncher.data.entities.Profile;
import com.laeben.corelauncher.minecraft.modding.entities.CResource;

import java.util.function.Consumer;

public class LMod {
    private final CResource res;
    private final Profile profile;
    private boolean isLocal;
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

    public boolean isLocal() {
        return isLocal;
    }

    public LMod setLocal(boolean a){
        isLocal = a;

        return this;
    }

    public LMod setAction(Consumer<LMod> action){
        this.onAction = action;

        return this;
    }
}
