package com.laeben.corelauncher.api.ui;

import com.laeben.core.util.events.BaseEvent;
import com.laeben.core.util.events.KeyEvent;
import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.api.ui.entity.Frame;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.ui.entity.LScene;
import com.laeben.corelauncher.ui.entity.LStage;
import com.laeben.corelauncher.util.EventHandler;
import com.laeben.corelauncher.api.entity.Logger;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class UI {
    private final List<LStage> windows;
    private final EventHandler<BaseEvent> handler;
    private boolean implicit = true;
    private static UI instance;

    public UI(){
        windows = new ArrayList<>();
        handler = new EventHandler<>();

        instance = this;
    }

    public static UI getUI(){
        return instance;
    }

    public EventHandler<BaseEvent> getHandler(){
        return handler;
    }

    /**
     * Implicit mode triggers application shutdown when all windows are closed.
     */
    public void setImplicit(boolean value){
        implicit = value;
    }

    /**
     * Creates a new stage.
     * @param url fxml url
     * @param name identifier of the stage
     * @param controller controller of the stage, ui uses default controller on null value
     * @param resources custom resource bundle, ui uses default bundle on null value
     * @param useFrame create stage in a default frame
     * @return created stage.
     * @throws RuntimeException on load failure.
     */
    public LStage create(URL url, String name, Object controller, ResourceBundle resources, boolean useFrame)  {
        var loader = getDefaultLoader(url, resources);
        if (controller != null)
            loader.setController(controller);
        Parent root;
        try {
            root = loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        /*Parent root = null;
        try {
            root = loader.load();
        } catch (IOException e) {
            Logger.getLogger().log(e);
        }*/

        if (root == null)
            return null;

        LScene scene;
        var stage = new LStage()
                .setName(name);

        if (useFrame){
            var frameLoader = getDefaultLoader(CoreLauncherFX.class.getResource("layout/frame.fxml"));
            Parent frame = null;
            try {
                frame = frameLoader.load();
            } catch (IOException e) {
                Logger.getLogger().log(e);
            }
            if (frame == null)
                return null;
            var frameController = (Frame)frameLoader.getController();
            frameController.setContent(root);
            if (frameController instanceof Controller c)
                c.setStage(stage);

            scene = new LScene(frame, loader.getController());
            stage.setStyle(StageStyle.TRANSPARENT)
                    .setFrame(frameController);
        }
        else
            scene = new LScene(root, loader.getController());

        scene.setFill(Color.TRANSPARENT);
        stage.setStageScene(scene);

        stage.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, a -> close(stage));

        if (loader.getController() instanceof Controller c)
            c.setStage(stage).setNode(root);

        windows.add(stage);
        handler.execute(new KeyEvent("windowOpen").setSource(stage));

        return stage;
    }
    public LStage create(String name, Object controller, boolean useFrame) {
        return create(CoreLauncherFX.class.getResource("layout/" + name + ".fxml"), name, controller, null, useFrame)
                .setStageTitle(Translator.translate("frame.title." + name));
    }
    public LStage create(String name){
        return create(name, null, true);
    }

    public Node load(URL url, Object controller){
        var manager = UI.getDefaultLoader(url);
        manager.setController(controller);
        try{
            return manager.load();
        }
        catch (IOException e){
            Logger.getLogger().log(e);
            return new Rectangle();
        }
    }

    public static FXMLLoader getDefaultLoader(URL url, ResourceBundle resources){
        var loader = new FXMLLoader(url);

        loader.setResources(resources == null ? Translator.getTranslator().getBundle() : resources);

        return loader;
    }

    public void reset(){
        Platform.runLater(() -> {
            var i = implicit;
            implicit = false;
            windows.stream().toList().forEach(this::close);
            implicit = i;
            create("main").show();
        });
    }

    public LStage get(String name){
        return windows.stream().filter(x -> name.equals(x.getName())).findFirst().orElse(null);
    }

    public void focus(String name){
        var f = windows.stream().filter(x -> name.equals(x.getName())).findFirst();
        f.ifPresent(Window::requestFocus);
    }

    public void hideAll(){
        Platform.runLater(() -> windows.stream().toList().forEach(LStage::hide));
    }

    public void showAll(){
        Platform.runLater(() -> windows.stream().toList().forEach(LStage::show));
    }

    public static FXMLLoader getDefaultLoader(URL url){
        return getDefaultLoader(url, null);
    }

    public static Controller load(URL url){
        var loader = getDefaultLoader(url);
        try {
            Node node = loader.load();
            return loader.getController() instanceof Controller c ? c.setNode(node) : Controller.DEFAULT.setNode(node);
        } catch (IOException e) {
            Logger.getLogger().log(e);
            return Controller.DEFAULT;
        }
    }

    public void close(Window w){
        if (!(w instanceof Stage st))
            return;

        if (st.isShowing())
            st.close();

        if (st instanceof LStage stage){
            windows.remove(stage);
            handler.execute(new KeyEvent("windowClose").setSource(stage));
            if (windows.isEmpty() && implicit)
                Platform.exit();
        }
    }
}
