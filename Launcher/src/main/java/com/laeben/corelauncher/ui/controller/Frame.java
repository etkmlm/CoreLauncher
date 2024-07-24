package com.laeben.corelauncher.ui.controller;

import com.laeben.corelauncher.api.ui.Controller;
import com.laeben.corelauncher.ui.control.CButton;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

public class Frame extends Controller implements com.laeben.corelauncher.api.ui.entity.Frame {
    @FXML
    private CButton btnClose;
    @FXML
    private CButton btnMinimize;
    @FXML
    private CButton btnMaximize;
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
        btnClose.enableTransparentAnimation();
        btnMaximize.enableTransparentAnimation();
        btnMinimize.enableTransparentAnimation();
        btnClose.setOnMouseClicked(a -> close());
        btnMinimize.setOnMouseClicked(a -> getStage().setIconified(true));
        btnMaximize.setOnMouseClicked(a -> getStage().setMaximized(!getStage().isMaximized()));
    }
}
