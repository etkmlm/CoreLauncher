package com.laeben.corelauncher.ui.controller;

import com.laeben.corelauncher.api.ui.Controller;
import com.laeben.corelauncher.ui.control.CShapefulButton;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

public class Frame extends Controller implements com.laeben.corelauncher.api.ui.entity.Frame {
    @FXML
    private CShapefulButton btnClose;
    @FXML
    private CShapefulButton btnMinimize;
    @FXML
    private CShapefulButton btnMaximize;
    @FXML
    private StackPane content;


    public void setTitle(String title){
        //
    }

    @Override
    public void setContent(Node node) {
        content.getChildren().clear();
        content.getChildren().add(node);
    }


    @Override
    public void preInit(){
        btnClose.setOnMouseClicked(a -> close());
        btnMinimize.setOnMouseClicked(a -> getStage().setIconified(true));
        btnMaximize.setOnMouseClicked(a -> getStage().setMaximized(!getStage().isMaximized()));
    }
}
