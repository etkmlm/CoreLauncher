package com.cdev.corelauncher;

import com.cdev.corelauncher.minecraft.Launcher;
import com.cdev.corelauncher.ui.utils.FXManager;
import com.cdev.corelauncher.utils.JavaManager;
import com.cdev.corelauncher.utils.Logger;
import com.cdev.corelauncher.utils.entities.OS;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class CoreLauncherFX extends Application {

    @Override
    public void start(Stage stage) {
        stage.close();
        new FXManager();
        FXManager.getManager().openScene("main", "Hoş geldin uşağum!", 720, 600);
    }

    public static void launchFX(){
        launch();
    }
}