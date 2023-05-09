package com.cdev.corelauncher.ui.controller;

import com.cdev.corelauncher.CoreLauncher;
import com.cdev.corelauncher.data.Configurator;
import com.cdev.corelauncher.data.Profiler;
import com.cdev.corelauncher.data.entities.Config;
import com.cdev.corelauncher.data.entities.Profile;
import com.cdev.corelauncher.minecraft.Launcher;
import com.cdev.corelauncher.minecraft.entities.LaunchInfo;
import com.cdev.corelauncher.ui.controls.CProfile;
import com.cdev.corelauncher.utils.JavaManager;
import com.cdev.corelauncher.utils.Logger;
import com.cdev.corelauncher.utils.entities.Java;
import com.cdev.corelauncher.utils.entities.Path;
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
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

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
    @FXML
    private Label status;
    @FXML
    private Label detailedStatus;



    private final ObservableList<Profile> profiles;

    public Main(){
        profiles = FXCollections.observableArrayList(Profiler.getProfiler().getAllProfiles());

        Profiler.getProfiler().getHandler().addHandler("mainWindow", (a) -> {
            if (a.getKey().equals("profileCreate"))
                profiles.add((Profile) a.getNewValue());
            else if (a.getKey().equals("profileDelete"))
                profiles.remove((Profile) a.getOldValue());
        });

        //Profiler.getProfiler().deleteProfile(Profiler.getProfiler().getProfile("testprofile3"));
    }

    @FXML
    private void initialize(){
        Launcher.getLauncher().getHandler().addHandler("main", (a) -> {
            switch (a.getType()) {
                case NEED -> {
                    if (a.getKey().startsWith("java")) {
                        Java j = Java.fromVersion(Integer.parseInt(a.getKey().substring(4)));
                        JavaManager.getManager().download(j, (b) -> Platform.runLater(() -> prg.setProgress(b)));
                        l(((LaunchInfo) a.getValue()).versionId);
                    }
                }
                case PROGRESS -> Platform.runLater(() -> prg.setProgress((double) a.getValue()));
                //case STATE -> Platform.runLater(() -> .setText(a.getKey()));
            }

        });

        /*btnPro.setOnMouseClicked((a) -> {
            String vId = txtPro.getText();
            l(vId);
        });

        txtPro.setOnKeyPressed((a) -> {
            if (a.getCode() == KeyCode.ENTER)
                l(txtPro.getText());
        });*/
        lvProfiles.setItems(profiles);
        lvProfiles.setCellFactory((x) -> new CProfile());

        Profiler.getProfiler().createProfile("uwu");
        Profiler.getProfiler().createProfile("uwu2");
        Profiler.getProfiler().createProfile("uwu3");

        btnStart.setOnMouseClicked((a) -> {
            String ver = "1.12.2";
            l(ver);
        });


    }

    private void l(String vId){
        new Thread(() -> {
            Launcher.getLauncher().downloadVersion(vId);
            Launcher.getLauncher().launch(vId, Configurator.getConfig().getUser().getUsername(), "T", JavaManager.getDefault(), null);
        }).start();
    }

    public void onBtnSettings(ActionEvent event) {
        System.out.println("Settings");
    }

    public void onBtnAbout(ActionEvent event) {
        System.out.printf("About");
    }

    public void onBtnAddProfile(ActionEvent event) {
        System.out.println("btnAddProfile clicked");
    }


}
