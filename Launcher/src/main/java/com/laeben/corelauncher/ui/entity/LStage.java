package com.laeben.corelauncher.ui.entity;

import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.api.ui.entity.Frame;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Objects;

public class LStage extends Stage {

    public static final Image LOGO16;
    public static final Image LOGO32;
    public static final Image LOGO64;

    private Frame frame;

    static {
        LOGO16 = new Image(Objects.requireNonNull(CoreLauncherFX.class.getResourceAsStream("/com/laeben/corelauncher/logo16x16.png")));
        LOGO32 = new Image(Objects.requireNonNull(CoreLauncherFX.class.getResourceAsStream("/com/laeben/corelauncher/logo32x32.png")));
        LOGO64 = new Image(Objects.requireNonNull(CoreLauncherFX.class.getResourceAsStream("/com/laeben/corelauncher/logo64x64.png")));
    }

    private LScene scene;
    private String name;

    public LStage(){
        getIcons().add(LOGO16);
        getIcons().add(LOGO32);
        getIcons().add(LOGO64);
    }

    public LStage setName(String name){
        this.name = name;
        return this;
    }

    public String getName(){
        return name;
    }

    public LStage setStyle(StageStyle style){
        initStyle(style);
        return this;
    }

    public LStage setFrame(Frame f){
        frame = f;
        return this;
    }

    public Frame getFrame(){
        return frame;
    }

    public LStage setStageScene(LScene s){
        setScene(s.setStage(this));
        scene = s;
        return this;
    }

    public LStage setStageTitle(String title){
        setTitle(title);
        if (frame != null)
            frame.setTitle(title);
        return this;
    }

    public LScene getLScene(){
        return scene;
    }
}
