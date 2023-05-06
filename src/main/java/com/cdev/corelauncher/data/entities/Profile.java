package com.cdev.corelauncher.data.entities;

import com.cdev.corelauncher.minecraft.entities.Version;
import com.cdev.corelauncher.utils.entities.Path;
import javafx.fxml.FXML;
import javafx.scene.image.ImageView;

public class Profile {
    private String name;
    private String versionId;
    private String image;
    private final Path folder;
    private final Path profileInfo;

    public Profile(Path folder){
        this.folder = folder;
        name = folder.getName();
        profileInfo = folder.to("profileInfo.json");
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

}
