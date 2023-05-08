package com.cdev.corelauncher.data.entities;

import com.cdev.corelauncher.minecraft.curseforge.entities.Mod;
import com.cdev.corelauncher.minecraft.entities.Version;
import com.cdev.corelauncher.utils.GsonUtils;
import com.cdev.corelauncher.utils.entities.Path;
import javafx.fxml.FXML;
import javafx.scene.image.ImageView;

import java.util.ArrayList;
import java.util.List;

public class Profile {
    private transient boolean isEmpty;
    private String name;
    private String versionId;
    private Account customUser;
    private String icon;
    private List<Mod> mods;
    private Path path;

    private Profile(){

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

    public static Profile empty(){
        var p = new Profile();
        p.isEmpty = true;
        return p;
    }

    public Profile save() {
        if (isEmpty)
            return null;

        String json = GsonUtils.DEFAULT_GSON.toJson(this);

        path.to("profile.json").write(json);

        return this;
    }

    private Profile setProfilePath(Path path){
        this.path = path;
        this.name = path.getName();
        return this;
    }

    public static Profile get(Path profilePath) {
        var file = profilePath.to("profile.json");
        return (file.exists() ? GsonUtils.DEFAULT_GSON.fromJson(file.read(), Profile.class) : new Profile())
                .setProfilePath(profilePath).save();
    }

}
