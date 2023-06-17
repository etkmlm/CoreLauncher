package com.cdev.corelauncher.ui.controller;

import com.cdev.corelauncher.LauncherConfig;
import com.cdev.corelauncher.data.Configurator;
import com.cdev.corelauncher.data.Profiler;
import com.cdev.corelauncher.data.Translator;
import com.cdev.corelauncher.data.entities.Account;
import com.cdev.corelauncher.minecraft.Wrapper;
import com.cdev.corelauncher.utils.events.ChangeEvent;
import com.cdev.corelauncher.data.entities.Profile;
import com.cdev.corelauncher.minecraft.Launcher;
import com.cdev.corelauncher.minecraft.entities.ExecutionInfo;
import com.cdev.corelauncher.ui.controls.CMsgBox;
import com.cdev.corelauncher.ui.controls.CProfile;
import com.cdev.corelauncher.ui.entities.LProfile;
import com.cdev.corelauncher.ui.utils.FXManager;
import com.cdev.corelauncher.utils.JavaMan;
import com.cdev.corelauncher.utils.entities.Java;
import com.cdev.corelauncher.utils.events.KeyEvent;
import com.cdev.corelauncher.utils.events.ProgressEvent;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;

import java.util.stream.Collectors;

public class Main{

    @FXML
    private ProgressIndicator prg;
    @FXML
    private ImageView imgHeadView;
    @FXML
    private Label lblPlayerName;
    @FXML
    private Label gameVersion;
    @FXML
    private Button btnStart;
    @FXML
    private Button btnSettings;
    @FXML
    private Button btnAbout;
    @FXML
    private Button btnAddProfile;
    @FXML
    private ListView<LProfile> lvProfiles;
    @FXML
    private Label gameDescription;
    @FXML
    private Label status;
    @FXML
    private Label detailedStatus;

    private Profile selectedProfile;
    private ObservableList<LProfile> profiles;

    public Main(){
        reloadProfiles();
    }

    private void reloadProfiles(){
        profiles = FXCollections.observableList(Profiler.getProfiler().getAllProfiles().stream().map(x -> {
            var lp = LProfile.get(x);
            lp.setEventListener(this::onProfileSelectEvent);
            return lp;
        }).collect(Collectors.toList()), LProfile::getProperties);
    }

    @FXML
    private void initialize(){
        Launcher.getLauncher().getHandler().addHandler("main", this::onGeneralEvent);
        Profiler.getProfiler().getHandler().addHandler("mainWindow", this::onProfilerEvent);

        lvProfiles.setItems(profiles);
        lvProfiles.setCellFactory((x) -> new CProfile());

        btnStart.setOnMouseClicked((a) -> launch(selectedProfile));

        btnSettings.setOnMouseClicked((a) -> FXManager.getManager().applyStage("settings", Translator.translate("settings.title")).showStage());

        btnAbout.setOnMouseClicked((a) ->
                CMsgBox.msg(Alert.AlertType.INFORMATION, Translator.translate("about.title"), Translator.translateFormat("about.content", LauncherConfig.VERSION, "https://github.com/etkmlm", "CoreLauncher")).show());

        btnAddProfile.setOnMouseClicked((a) -> ProfileEdit.open(null));

        setUser(Configurator.getConfig().getUser().reload());

        if (Configurator.getConfig().getLastSelectedProfile() != null){
            var selected = Profiler.getProfiler().getProfile(Configurator.getConfig().getLastSelectedProfile().getName());
            profiles.stream().filter(x -> x.getProfile() == selected).findFirst().orElse(LProfile.get(selected)).setSelected(true);
        }
    }

    public void selectProfile(Profile p){
        selectedProfile = p;

        if (p != null){
            gameVersion.setText(p.getVersionId());
            gameDescription.setText(p.getName());
            setUser(p.getUser() == null ? Configurator.getConfig().getUser() : p.getUser().reload());
        }
        else{
            gameVersion.setText(null);
            gameDescription.setText(null);
            setUser(Configurator.getConfig().getUser());
        }

        Configurator.getConfig().setLastSelectedProfile(p);
        Configurator.save();
    }

    private void launch(Profile p){
        if (selectedProfile == null)
            return;

        ((Wrapper<?>)p.getWrapper()).getHandler().addHandler("main", this::onGeneralEvent);
        new Thread(() -> {
            //Launcher.getLauncher().downloadVersion(p.getVersionId());
            Launcher.getLauncher().prepare(p);
            Launcher.getLauncher().launch(ExecutionInfo.fromProfile(p));
        }).start();
    }

    private void setUser(Account acc){
        imgHeadView.setImage(acc.getHead());
        lblPlayerName.setText(acc.getUsername());
    }

    private void onGeneralEvent(Event e){
        if (e instanceof ProgressEvent p){
            Platform.runLater(() -> {
                detailedStatus.setText("%" + (p.progress * 100));
                prg.setProgress(p.progress);
            });
        }
        else if (e instanceof KeyEvent k){
            String status;
            String key = k.getKey();
            if (key.startsWith("java")){
                String major = k.getKey().substring(4);
                status = Translator.translateFormat("launch.state.download.java", major);
            }
            else if (key.equals("clientDownload"))
                status = Translator.translate("launch.state.download.client");
            else if (key.startsWith("lib")){
                status = Translator.translate("launch.state.download.libraries");
                Platform.runLater(() -> detailedStatus.setText(key.substring(3)));
            }
            else if (key.startsWith("asset")){
                status = Translator.translate("launch.state.download.assets");
                Platform.runLater(() -> detailedStatus.setText(key.substring(6)));
            }
            else if (key.equals("gameStart"))
                status = Translator.translate("launch.state.started");
            else if (key.equals("gameEnd")){
                status = "";
                Platform.runLater(() -> {
                    prg.setProgress(0);
                    detailedStatus.setText("");
                });
            }
            else if (key.startsWith("prepare")){
                status = Translator.translateFormat("launch.state.prepare", key.substring(7));
            }
            else
                status = key;

            Platform.runLater(() -> this.status.setText(status));

        }
    }
    private void onProfilerEvent(ChangeEvent a){
        var newProfile = (Profile)a.getNewValue();
        var oldProfile = (Profile)a.getOldValue();

        switch (a.getKey()) {
            case "profileCreate" ->
                    profiles.add(LProfile.get(newProfile)
                            .setEventListener(this::onProfileSelectEvent));
            case "profileDelete" -> {
                profiles.removeIf(x -> x.getProfile() == oldProfile);
                if (profiles.stream().noneMatch(LProfile::selected) && profiles.size() > 0) {
                    profiles.stream().findFirst().get().setSelected(true);
                }
            }
            case "profileUpdate" -> {
                lvProfiles.refresh();
                if (selectedProfile == newProfile)
                    selectProfile(selectedProfile);
            }
            case "reload" -> {
                reloadProfiles();
                lvProfiles.setItems(profiles);
                selectProfile(null);
            }
        }
    }
    private void onProfileSelectEvent(ChangeEvent e) {

        var profile = (LProfile) e.getSource();

        if (!profile.selected())
            return;

        selectProfile(profile.getProfile());

        profiles.forEach(c -> {
            if (c != profile)
                c.setSelected(false);
        });

        lvProfiles.setCellFactory(x -> new CProfile());
        lvProfiles.refresh();
    }

}
