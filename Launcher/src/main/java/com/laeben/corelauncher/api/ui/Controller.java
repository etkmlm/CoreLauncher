package com.laeben.corelauncher.api.ui;

import com.laeben.corelauncher.ui.entity.EventFilter;
import com.laeben.corelauncher.ui.entity.LScene;
import com.laeben.corelauncher.ui.entity.LStage;
import com.laeben.corelauncher.ui.util.EventFilterManager;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.stage.WindowEvent;

import java.util.function.Consumer;

public abstract class Controller {
    public static final Controller DEFAULT = new Controller() {
        @Override
        public void preInit() {

        }
    };

    private final EventFilterManager efManager;

    private LStage stage;
    protected Node rootNode;
    protected Object parentObj;

    private Consumer<WindowEvent> onWindowShown;

    public Controller(){
        efManager = new EventFilterManager();
    }

    public final Controller setStage(LStage stage){
        if (this.stage != null)
            efManager.removeEventFilter(this.stage);

        this.stage = stage;

        init();

        efManager.addEventFilter(EventFilter.window(stage, WindowEvent.WINDOW_SHOWN, a -> onShown()));

        return this;
    }

    public final Controller setNode(Node node){
        this.rootNode = node;
        rootNode.getProperties().put("controller", this);

        onRootSet(node);

        return this;
    }

    public final Controller setParentObject(Object obj){
        this.parentObj = obj;

        onParentSet(obj);

        return this;
    }

    public Node getRootNode(){
        return rootNode;
    }

    public Object getParentObject(){
        return parentObj;
    }

    public LStage getStage(){
        return stage;
    }

    public LScene getScene(){
        return stage.getLScene();
    }

    public void close(){
        UI.getUI().close(getStage());
    }

    public void focus(){
        getStage().requestFocus();
    }

    public double getWidth(){
        return rootNode == null ? 0 : rootNode.getLayoutBounds().getWidth();
    }

    public double getHeight(){
        return rootNode == null ? 0 : rootNode.getLayoutBounds().getHeight();
    }

    @FXML
    public void initialize(){
        preInit();
    }

    /**
     * FXML initialization.
     * Stage may be null.
     */
    public abstract void preInit();


    /**
     * Post FXML initialization.
     * Stage is not null.
     */
    public void init(){

    }

    public void onParentSet(Object obj){

    }

    public void onShown(){

    }

    public void onRootSet(Node n){

    }

    public void onFocusLimitIgnored(Controller by, Node target){

    }

    public void removeRegisteredEventFilter(Object s){
        efManager.removeEventFilter(s);
    }

    public void addRegisteredEventFilter(EventFilter filter){
        efManager.addEventFilter(filter);
    }

    public void dispose(){
        efManager.clear();
    }
}
