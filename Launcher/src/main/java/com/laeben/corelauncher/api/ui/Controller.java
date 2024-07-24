package com.laeben.corelauncher.api.ui;


import com.laeben.corelauncher.ui.entity.LScene;
import com.laeben.corelauncher.ui.entity.LStage;
import javafx.fxml.FXML;
import javafx.scene.Node;

public abstract class Controller {

    public static final Controller DEFAULT = new Controller() {
        @Override
        public void preInit() {

        }
    };

    private LStage stage;
    protected Node rootNode;
    protected Object parentObj;

    public Controller(){

    }

    public final Controller setStage(LStage stage){
        this.stage = stage;

        init();

        return this;
    }

    public final Controller setNode(Node node){
        this.rootNode = node;

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

    public void dispose(){

    }
}
