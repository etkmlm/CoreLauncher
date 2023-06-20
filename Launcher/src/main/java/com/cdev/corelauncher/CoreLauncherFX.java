package com.cdev.corelauncher;

import com.cdev.corelauncher.ui.utils.FXManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.Objects;

public class CoreLauncherFX extends Application {

    @Override
    public void start(Stage stage) {
        stage.close();
        new FXManager();
        Platform.setImplicitExit(false);

        Font.loadFont(Objects.requireNonNull(CoreLauncherFX.class.getResource("/com/cdev/corelauncher/font/Minecraft.ttf")).toExternalForm(), 16);

        FXManager.getManager().applyStage("main").show();
    }

    public static void launchFX(){
        launch();
    }
}