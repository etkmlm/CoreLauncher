package com.laeben.corelauncher.ui.controller;

import com.laeben.corelauncher.ui.controls.CButton;
import com.laeben.corelauncher.ui.controls.CJava;
import com.laeben.corelauncher.ui.entities.LStage;
import com.laeben.corelauncher.ui.utils.FXManager;
import com.laeben.corelauncher.utils.JavaMan;
import com.laeben.corelauncher.utils.entities.Java;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

import java.util.ArrayList;

public class JavaManager {
    private final ObservableList<Java> javas;
    private static LStage instance;
    private boolean isOpenNewJava;

    public static LStage getManager(){
        return instance == null ? (instance = FXManager.getManager().applyStage("javaman")) : instance;
    }

    public static void openManager(){
        if (getManager().isShowing())
            getManager().requestFocus();
        else{
            getManager().close();
            instance = null;
            getManager().show();
        }
    }

    public JavaManager(){
        javas = FXCollections.observableList(new ArrayList<>(JavaMan.getManager().getAllJavaVersions()));

        JavaMan.getManager().getHandler().addHandler("javaman", (a) -> {
            switch (a.getKey()){
                case "addJava" -> isOpenNewJava = false;
                case "delJava" -> javas.remove((Java) a.getOldValue());
            }
        });
    }

    @FXML
    private ListView<Java> lvJava;
    @FXML
    private CButton btnAdd;

    @FXML
    public void initialize(){
        lvJava.setCellFactory(x -> new CJava());
        lvJava.setItems(javas);

        btnAdd.setOnMouseClicked((a) -> {
            if (!isOpenNewJava){
                javas.add(new Java());
                isOpenNewJava = true;
            }
        });
    }


}
