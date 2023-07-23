package com.laeben.corelauncher.ui.utils;

import com.laeben.core.util.events.BaseEvent;
import com.laeben.core.util.events.KeyEvent;
import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.data.Translator;
import com.laeben.corelauncher.ui.controller.Frame;
import com.laeben.corelauncher.ui.entities.LScene;
import com.laeben.corelauncher.ui.entities.LStage;
import com.laeben.corelauncher.utils.EventHandler;
import com.laeben.corelauncher.utils.Logger;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FXManager {

    private static FXManager instance;
    private EventHandler<BaseEvent> handler;

    private final List<LStage> openStages;

    private boolean implicit = true;

    public FXManager(){
        openStages = new ArrayList<>();
        handler = new EventHandler<>();

        instance = this;
    }

    public EventHandler<BaseEvent> getHandler(){
        return handler;
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
            newScene.setFill(Color.TRANSPARENT);
            var stage = new LStage()
                    .setStageScene(newScene)
                    .setStageTitle(title)
                    .setStyle(StageStyle.TRANSPARENT)
                    .setName(name);

            openStages.add(stage);
            stage.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, (a) -> closeStage(stage));
            return stage;
        }
        catch (Exception e){
            Logger.getLogger().log(e);
            return null;
        }
    }

    public void closeStage(Window s){
        var st = (Stage) s;
        if (st.isShowing())
            st.close();

        if (st instanceof LStage stage){
            openStages.remove(stage);
            handler.execute(new KeyEvent("close").setSource(stage));
            if (openStages.size() == 0 && implicit){
                Platform.exit();
            }
        }
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

    public void focus(String name){
        var f = openStages.stream().filter(x -> x.getName().equals(name)).findFirst();
        f.ifPresent(Window::requestFocus);
    }

    public LStage get(String name){
        return openStages.stream().filter(x -> x.getName().equals(name)).findFirst().orElse(null);
    }

    public void hideAll(){
        Platform.runLater(() -> openStages.stream().toList().forEach(LStage::hide));
    }

    public void showAll(){
        Platform.runLater(() -> openStages.stream().toList().forEach(LStage::show));
    }
}
