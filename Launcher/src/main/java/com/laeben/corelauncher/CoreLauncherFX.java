package com.laeben.corelauncher;

import com.laeben.corelauncher.api.FloatDock;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.wrap.ExtensionWrapper;
import javafx.application.Application;
import com.laeben.corelauncher.api.ui.UI;
import javafx.scene.text.Font;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;

import javax.imageio.ImageIO;
import java.io.*;
import java.util.List;
import java.util.Objects;

public class CoreLauncherFX extends Application {
    public static final String CLUI_CSS;

    static {
        var clui = CoreLauncherFX.class.getResource("/com/laeben/corelauncher/style/controls/clui.css");
        assert clui != null;
        CLUI_CSS = clui.toExternalForm();
    }

    static Profile fromArgs;

    @Override
    public void start(Stage stage) throws IOException {
        stage.close();
        new UI();

        ImageIO.scanForPlugins();

        new FloatDock().reload();
        UI.setImplicitShutdown(false);

        Font.loadFont(Objects.requireNonNull(CoreLauncherFX.class.getResource("/com/laeben/corelauncher/font/Minecraft.ttf")).toExternalForm(), 16);

        CoreLauncher.GUI_INIT = true;

        if (Debug.DEBUG_UI){
            Debug.runUI();
            UI.shutdown();
        }
        else{
            UI.getUI().create("main").show();
            if (fromArgs != null){
                Main.getMain().launch(fromArgs, false, null);
                fromArgs = null;
            }
        }

        ExtensionWrapper.getWrapper().fireEvent("onUILoad");

        // Version check
        CoreLauncher.updateCheck();

        new Thread(CoreLauncher::announcementCheck).start();
    }

    public static void launchFX(){
        launch();
    }

    public static boolean isAnyPopupOpen(){
        return Window.getWindows().stream().anyMatch(a -> a instanceof Popup);
    }
    public static List<Popup> getAllPopupWindows(){
        return Window.getWindows().stream().filter(a -> a instanceof Popup).map(a -> (Popup)a).toList();
    }
}