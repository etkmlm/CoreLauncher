package com.cdev.corelauncher.ui.controller;

import com.cdev.corelauncher.CoreLauncher;
import com.cdev.corelauncher.data.Configurator;
import com.cdev.corelauncher.data.entities.Config;
import com.cdev.corelauncher.data.entities.Profile;
import com.cdev.corelauncher.minecraft.Launcher;
import com.cdev.corelauncher.minecraft.entities.LaunchInfo;
import com.cdev.corelauncher.ui.controls.CProfile;
import com.cdev.corelauncher.utils.JavaManager;
import com.cdev.corelauncher.utils.Logger;
import com.cdev.corelauncher.utils.entities.Java;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class Main{

    @FXML
    private ProgressIndicator prg;

    //-----------------------------


    @FXML
    private ImageView playerHeadView;
    @FXML
    private Label playerName;
    @FXML
    private Label gameVersion;

    @FXML
    private Button btnStart;

    @FXML
    private ListView<Profile> lvProfiles;
    @FXML
    private Label gameDescription;



    private final ObservableList<Profile> profiles;

    public Main(){
        profiles = FXCollections.observableArrayList();
        var p = new Profile(Configurator.getConfig().getGamePath());

        profiles.add(p);
        profiles.add(p);
        profiles.add(p);
        profiles.add(p);
        profiles.add(p);
        profiles.add(p);
    }

    @FXML
    private void initialize(){
        /*Launcher.getLauncher().getHandler().addHandler("main", (a) -> {
            switch (a.getType()) {
                case NEED -> {
                    if (a.getKey().startsWith("java")) {
                        Java j = Java.fromVersion(Integer.parseInt(a.getKey().substring(4)));
                        JavaManager.getManager().download(j, (b) -> Platform.runLater(() -> prg.setProgress(b)));
                        l(((LaunchInfo) a.getValue()).versionId);
                    }
                }
                case PROGRESS -> Platform.runLater(() -> prg.setProgress((double) a.getValue()));
                case STATE -> Platform.runLater(() -> lblState.setText(a.getKey()));
            }
        });*/

        /*btnPro.setOnMouseClicked((a) -> {
            String vId = txtPro.getText();
            l(vId);
        });

        txtPro.setOnKeyPressed((a) -> {
            if (a.getCode() == KeyCode.ENTER)
                l(txtPro.getText());
        });
        test.setItems(profiles);
        test.setCellFactory((x) -> new CProfile());*/



    }

    private void l(String vId){
        new Thread(() -> {
            Launcher.getLauncher().downloadVersion(vId);
            Launcher.getLauncher().launch(vId, "EvilMonster", "T", JavaManager.getDefault(), null);
        }).start();
    }






}
