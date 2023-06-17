package com.cdev.corelauncher;

import com.cdev.corelauncher.ui.utils.FXManager;
import javafx.application.Application;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class CoreLauncherFX extends Application {

    @Override
    public void start(Stage stage) {
        stage.close();
        new FXManager();

        Font.loadFont(CoreLauncherFX.class.getResource("/com/cdev/corelauncher/font/Minecraft.ttf").toExternalForm(), 16);


        FXManager.getManager().applyStage("main", "Hoş geldin uşağum!").showStage();
    }

    public static void launchFX(){
        launch();
    }
}