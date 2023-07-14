package com.laeben.corelauncher.ui.controller;

import com.laeben.core.util.Cat;
import com.laeben.core.util.events.KeyEvent;
import com.laeben.corelauncher.minecraft.Launcher;
import com.laeben.corelauncher.ui.controls.CButton;
import com.laeben.corelauncher.ui.controls.CJava;
import com.laeben.corelauncher.ui.entities.LStage;
import com.laeben.corelauncher.ui.utils.FXManager;
import com.laeben.corelauncher.utils.JavaMan;
import com.laeben.corelauncher.utils.entities.Java;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;

import java.util.ArrayList;
import java.util.Arrays;

public class JavaManager {
    private final ObservableList<Java> javas;
    private static final int[] supportedJavaVersions = {8, 16, 17};
    private static LStage instance;
    private boolean isOpenNewJava;
    private final ContextMenu menu;

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
                case "addJava" -> {
                    if (isOpenNewJava)
                        isOpenNewJava = false;
                    else{
                        var f = (Java) a.getNewValue();
                        javas.add(f);
                    }
                }
                case "delJava" -> javas.remove((Java) a.getOldValue());
            }
        }, true);

        menu = new ContextMenu();
        menu.setStyle("-fx-background-color: #252525;");
        menu.getItems().addAll(Arrays.stream(supportedJavaVersions).mapToObj(x -> {
            var item = new MenuItem();
            String name = "JDK " + x;
            item.setText(name);
            item.setStyle("-fx-text-fill: white;");
            item.setOnAction(a -> {
                if (JavaMan.getManager().getAllJavaVersions().stream().anyMatch(y -> y.getName().equals(name)))
                    return;
                new Thread(() -> {
                    Platform.runLater(() -> FXManager.getManager().focus("main"));
                    JavaMan.getManager().download(Java.fromVersion(x));
                    Launcher.getLauncher().getHandler().execute(new KeyEvent("jvdown"));
                    Cat.sleep(500);
                    Platform.runLater(() -> FXManager.getManager().focus("javaman"));
                }).start();
            });
            return item;
        }).toList());
    }

    @FXML
    private ListView<Java> lvJava;
    @FXML
    private CButton btnAdd;
    @FXML
    private CButton btnDownload;

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

        btnDownload.setContextMenu(menu);
        btnDownload.setOnMouseClicked(a -> {
            if (a.getButton() != MouseButton.PRIMARY)
                return;

            menu.show(btnDownload, a.getScreenX(), a.getScreenY());
        });
    }


}
