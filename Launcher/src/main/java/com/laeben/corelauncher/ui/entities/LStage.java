package com.laeben.corelauncher.ui.entities;

import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.data.Translator;
import javafx.fxml.FXMLLoader;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;
import java.util.Objects;

public class LStage extends Stage {

    public static final Image LOGO16;
    public static final Image LOGO32;
    public static final Image LOGO64;

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

    public static FXMLLoader getDefaultLoader(URL url){
        FXMLLoader loader = new FXMLLoader(url);

        loader.setResources(Translator.getTranslator().getBundle());

        return loader;
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

    public LStage setStageScene(LScene s){
        setScene(s.setStage(this));
        scene = s;
        return this;
    }

    public LStage setStageTitle(String title){
        setTitle(title);
        return this;
    }

    public LScene getLScene(){
        return scene;
    }
}
