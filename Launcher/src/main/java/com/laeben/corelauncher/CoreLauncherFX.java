package com.laeben.corelauncher;

import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.FloatDock;
import com.laeben.corelauncher.api.entity.Logger;
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

    static Profile profileToLaunch;

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
            //UI.shutdown();
        }
        else{
            UI.getUI().create("main").show();
            if (profileToLaunch != null){
                Main.getMain().launch(profileToLaunch, null, null);
                profileToLaunch = null;
            }
        }

        ExtensionWrapper.getWrapper().fireEvent("onUILoad");

        // Version check
        try {
            CoreLauncher.updateCheck();
        } catch (NoConnectionException | StopException ignored) {

        }
        catch (HttpException e){
            Logger.getLogger().log(e);
        }

        new Thread(CoreLauncher::announcementCheck).start();
    }

    public static void launchFX(){
        /*System.setProperty("prism.lcdtext", "false");
        System.setProperty("prism.text", "t2k");*/
        System.setProperty("prism.allowhidpi", "true");
        System.setProperty("glass.win.uiScale", Configurator.getConfig().getUIScale() + "%");
        System.setProperty("glass.gtk.uiScale", Configurator.getConfig().getUIScale() + "%");
        launch();
    }

    public static boolean isAnyPopupOpen(){
        return Window.getWindows().stream().anyMatch(a -> a instanceof Popup);
    }
    public static List<Popup> getAllPopupWindows(){
        return Window.getWindows().stream().filter(a -> a instanceof Popup).map(a -> (Popup)a).toList();
    }
}