package com.cdev.corelauncher.ui.utils;

import com.cdev.corelauncher.CoreLauncherFX;
import com.cdev.corelauncher.ui.entities.LStage;
import com.cdev.corelauncher.utils.Logger;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FXManager {

    private static FXManager instance;

    private final List<LStage> openStages;

    public FXManager(){
        openStages = new ArrayList<>();

        instance = this;
    }


    public LStage openScene(String name, String title, int w, int h){
        return LStage.open(name, w, h).setStageTitle(title).showStage();
    }

    public static void load(URL u){
        FXMLLoader loader = new FXMLLoader(u);
        try{
            loader.load();
        }
        catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    public static FXManager getManager(){
        return instance;
    }

    public List<LStage> getOpenStages(){
        return openStages;
    }
}
