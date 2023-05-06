package com.cdev.corelauncher.ui.entities;

import com.cdev.corelauncher.CoreLauncherFX;
import com.cdev.corelauncher.utils.Logger;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class LStage extends Stage {

    private static final InputStream LOGO16 = CoreLauncherFX.class.getResourceAsStream("/com/cdev/corelauncher/logo16x16.png");
    private static final InputStream LOGO32 = CoreLauncherFX.class.getResourceAsStream("/com/cdev/corelauncher/logo32x32.png");
    private static final InputStream LOGO64 = CoreLauncherFX.class.getResourceAsStream("/com/cdev/corelauncher/logo64x64.png");

    private final Thread eventHandlerThread;
    private final List<Task<Void>> eventHandleQueue;

    public LStage(){
        eventHandleQueue = new ArrayList<>();
        eventHandlerThread = new Thread(() -> {
            int queue = 0;

        });

        getIcons().add(new Image(LOGO16));
        getIcons().add(new Image(LOGO32));
        getIcons().add(new Image(LOGO64));
    }

    public static LStage open(String fxml, int w, int h){
        LStage stage = new LStage();
        try{
            FXMLLoader fxmlLoader = new FXMLLoader(CoreLauncherFX.class.getResource("layout/" + fxml + ".fxml"));
            Scene scene = new Scene(fxmlLoader.load(), w, h);
            stage.setScene(scene);
        }
        catch (IOException e){
            Logger.getLogger().log(e);
        }

        return stage;
    }

    public LStage setStageScene(Scene s){
        setScene(s);
        return this;
    }

    public LStage setStageTitle(String title){
        setTitle(title);
        return this;
    }

    public LStage showStage(){
        show();
        return this;
    }
}
