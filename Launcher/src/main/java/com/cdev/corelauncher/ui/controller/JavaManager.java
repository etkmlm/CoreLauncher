package com.cdev.corelauncher.ui.controller;

import com.cdev.corelauncher.ui.controls.CButton;
import com.cdev.corelauncher.ui.controls.CJava;
import com.cdev.corelauncher.ui.entities.LScene;
import com.cdev.corelauncher.ui.entities.LStage;
import com.cdev.corelauncher.ui.utils.FXManager;
import com.cdev.corelauncher.utils.JavaMan;
import com.cdev.corelauncher.utils.entities.Java;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class JavaManager {
    private final ObservableList<Java> javas;
    private static LStage instance;
    private boolean isOpenNewJava;

    public static LStage getManager(){
        return instance == null ? (instance = FXManager.getManager().applyStage("javaman", "Java Manager")) : instance;
    }

    public static void openManager(){
        if (getManager().isShowing())
            getManager().requestFocus();
        else{
            getManager().close();
            instance = null;
            getManager().showStage();
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
