package com.cdev.corelauncher.ui.utils;

import com.cdev.corelauncher.CoreLauncherFX;
import com.cdev.corelauncher.ui.controls.CMsgBox;
import com.cdev.corelauncher.ui.entities.LStage;
import com.cdev.corelauncher.utils.Logger;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class FXManager {

    private static FXManager instance;

    private final List<LStage> openStages;

    public FXManager(){
        openStages = new ArrayList<>();

        instance = this;
    }

    public LStage applyStage(String name, String title, int w, int h){
        var stage = LStage.open(name, w, h).setStageTitle(title);
        openStages.add(stage);
        stage.onCloseRequestProperty().addListener((a) -> openStages.remove(stage));
        return stage;
    }

    public LStage applyStage(String name, String title){
        var stage = LStage.open(name).setStageTitle(title);
        openStages.add(stage);
        stage.onCloseRequestProperty().addListener((a) -> openStages.remove(stage));
        return stage;
    }

    public Node open(Object controller, URL url){
        var manager = LStage.getDefaultLoader(url);
        manager.setController(controller);
        try{
            return manager.load();
        }
        catch (IOException e){
            Logger.getLogger().log(e);
            return null;
        }
    }

    public static FXManager getManager(){
        return instance;
    }

    public List<LStage> getOpenStages(){
        return openStages;
    }

    public void restart(){
        openStages.forEach(Stage::close);
        applyStage("main", "Main").showStage();
    }
}
