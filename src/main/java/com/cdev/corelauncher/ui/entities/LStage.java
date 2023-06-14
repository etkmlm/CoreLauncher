package com.cdev.corelauncher.ui.entities;

import com.cdev.corelauncher.CoreLauncherFX;
import com.cdev.corelauncher.data.Configurator;
import com.cdev.corelauncher.data.Translator;
import com.cdev.corelauncher.utils.Logger;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LStage extends Stage {

    public static final InputStream LOGO16 = CoreLauncherFX.class.getResourceAsStream("/com/cdev/corelauncher/logo16x16.png");
    public static final InputStream LOGO32 = CoreLauncherFX.class.getResourceAsStream("/com/cdev/corelauncher/logo32x32.png");
    public static final InputStream LOGO64 = CoreLauncherFX.class.getResourceAsStream("/com/cdev/corelauncher/logo64x64.png");

    private LScene scene;

    public LStage(){
        getIcons().add(new Image(LOGO16));
        getIcons().add(new Image(LOGO32));
        getIcons().add(new Image(LOGO64));

    }

    public static FXMLLoader getDefaultLoader(URL url){
        FXMLLoader loader = new FXMLLoader(url);

        loader.setResources(Translator.getTranslator().getBundle());

        return loader;
    }

    protected static LScene getScene(String fxml, int w, int h){
        try {
            var loader = getDefaultLoader(CoreLauncherFX.class.getResource("layout/" + fxml + ".fxml"));
            return new LScene(loader.load(), w, h, loader);
        } catch (IOException e) {
            Logger.getLogger().log(e);
            return null;
        }
    }

    protected static LScene getScene(String fxml){
        try {
            var loader = getDefaultLoader(CoreLauncherFX.class.getResource("layout/" + fxml + ".fxml"));
            return new LScene(loader.load(), loader);
        } catch (IOException e) {
            Logger.getLogger().log(e);
            return null;
        }
    }

    public static LStage open(String fxml, int w, int h){
        return new LStage().setStageScene(getScene(fxml, w, h));
    }

    public static LStage open(String fxml){
        return new LStage().setStageScene(getScene(fxml));
    }

    public LStage setStageScene(LScene s){
        setScene(s);
        scene = s;
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

    public LScene getLScene(){
        return scene;
    }
}
