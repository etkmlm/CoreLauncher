package com.cdev.corelauncher.data.entities;

import com.cdev.corelauncher.minecraft.curseforge.entities.Mod;
import com.cdev.corelauncher.minecraft.entities.Version;
import com.cdev.corelauncher.utils.entities.Path;
import javafx.fxml.FXML;
import javafx.scene.image.ImageView;

import java.util.ArrayList;
import java.util.List;

public class Profile {
    private String name;
    private String versionId;
    private Account customUser;
    private String icon;
    private final List<Mod> mods;
    private final Path folder;
    private final Path profileInfo;

    private Profile(){
        
    }

    public Profile(Path folder){
        this.folder = folder;
        name = folder.getName();
        profileInfo = folder.to("profileInfo.json");
        mods = new ArrayList<>();
        versionId = "0.0.0";
    }

    public Profile reload(){

        return this;
    }

    public String getName() {
        return name;
    }

    public String getVersionId() {
        return versionId;
    }

    public Profile setVersionId(String versionId){
        this.versionId = versionId;
        return this;
    }

//----------------------------------------------


    public Profile writeToJson() {
        String json = GsonUtils.DEFAULT_GSON.toJson(this);

        profileInfo.write(json);

        return this;
    }

    public static Profile get(Path profilePath) {

        GsonUtils.DEFAULT_GSON.fromJson()
    }



}
