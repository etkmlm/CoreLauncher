package com.cdev.corelauncher.ui.utils;

import com.cdev.corelauncher.CoreLauncherFX;
import com.cdev.corelauncher.data.Translator;
import com.cdev.corelauncher.ui.controller.Frame;
import com.cdev.corelauncher.ui.controls.CMsgBox;
import com.cdev.corelauncher.ui.entities.LScene;
import com.cdev.corelauncher.ui.entities.LStage;
import com.cdev.corelauncher.utils.Logger;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

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

    private boolean implicit = true;

    public FXManager(){
        openStages = new ArrayList<>();

        instance = this;
    }
    public LStage applyStage(String name){
        try{
            String title = Translator.translate("frame.title." + name);
            var loader = LStage.getDefaultLoader(CoreLauncherFX.class.getResource("layout/" + name + ".fxml"));
            var frameLoader = LStage.getDefaultLoader(CoreLauncherFX.class.getResource("layout/frame.fxml"));
            var frame = (Parent)frameLoader.load();
            var content = (Pane)frame.getChildrenUnmodifiable().stream().filter(x -> x.getId() != null && x.getId().equals("content")).findFirst().get();
            var c = (Node)loader.load();

            ((Frame)frameLoader.getController()).setTitle(title);

            var controller = loader.getController();

            content.getChildren().clear();
            content.getChildren().add(c);

            var newScene = new LScene<>(frame, controller);
            var stage = new LStage()
                    .setStageScene(newScene)
                    .setStageTitle(title)
                    .setStyle(StageStyle.UNDECORATED);

            openStages.add(stage);
            stage.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, (a) -> closeStage(stage));
            return stage;
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public void closeStage(LStage stage){
        if (stage.isShowing())
            stage.close();
        openStages.remove(stage);
        if (openStages.size() == 0 && implicit)
            Platform.exit();
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
        Platform.runLater(() -> {
            implicit = false;
            openStages.stream().toList().forEach(this::closeStage);
            implicit = true;
            applyStage("main").show();
        });

    }

    public void hideAll(){
        Platform.runLater(() -> openStages.stream().toList().forEach(LStage::hide));
    }

    public void showAll(){
        Platform.runLater(() -> openStages.stream().toList().forEach(LStage::show));
    }
}
