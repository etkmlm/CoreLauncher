package com.laeben.corelauncher;

import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.corelauncher.data.Configurator;
import com.laeben.corelauncher.data.Translator;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.ui.controls.CMsgBox;
import com.laeben.corelauncher.ui.utils.FXManager;
import com.laeben.corelauncher.utils.JavaMan;
import com.laeben.corelauncher.utils.Logger;
import com.laeben.corelauncher.utils.NetUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.Objects;

public class CoreLauncherFX extends Application {

    @Override
    public void start(Stage stage) {
        stage.close();
        new FXManager();
        Platform.setImplicitExit(false);

        Font.loadFont(Objects.requireNonNull(CoreLauncherFX.class.getResource("/com/laeben/corelauncher/font/Minecraft.ttf")).toExternalForm(), 16);

        CoreLauncher.GUI_INIT = true;

        var mainStage = FXManager.getManager().applyStage("main");
        var main = (Main)mainStage.getLScene().getController();

        mainStage.show();

        // Version check
        var latest = LauncherConfig.APPLICATION.getLatest();
        if (latest != null && LauncherConfig.VERSION < latest.version() && Configurator.getConfig().isEnabledAutoUpdate()){
            var result = CMsgBox.msg(Alert.AlertType.INFORMATION, Translator.translate("update.title"), Translator.translateFormat("update.newVersion", latest.version()))
                    .setButtons(ButtonType.YES, ButtonType.NO)
                    .showAndWait();

            if (result.isPresent() && result.get() == ButtonType.YES){
                var n = CoreLauncher.LAUNCHER_PATH.to("clnew.jar");
                new Thread(() -> {
                    try{
                        NetUtils.download(latest.url(), n, false, true);
                    }
                    catch (NoConnectionException | StopException | HttpException e){
                        return;
                    }

                    try{
                        var name = CoreLauncher.LAUNCHER_EX_PATH;
                        if (name == null)
                            return;
                        new ProcessBuilder()
                                .command(JavaMan.getDefault().getExecutable().toString(), "-jar", n.toString(), "--old", name.getName())
                                .start();
                        System.exit(0);
                    }
                    catch (Exception e){
                        Logger.getLogger().log(e);
                    }
                }).start();
            }
        }


    }

    public static void launchFX(){
        launch();

    }
}