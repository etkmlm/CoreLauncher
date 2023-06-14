package com.cdev.corelauncher.ui.controller;

import com.cdev.corelauncher.LauncherConfig;
import com.cdev.corelauncher.data.Configurator;
import com.cdev.corelauncher.data.Profiler;
import com.cdev.corelauncher.data.Translator;
import com.cdev.corelauncher.data.entities.Account;
import com.cdev.corelauncher.data.entities.ChangeEvent;
import com.cdev.corelauncher.data.entities.Profile;
import com.cdev.corelauncher.minecraft.Launcher;
import com.cdev.corelauncher.minecraft.entities.ExecutionInfo;
import com.cdev.corelauncher.minecraft.entities.LauncherEvent;
import com.cdev.corelauncher.ui.controls.CMsgBox;
import com.cdev.corelauncher.ui.controls.CProfile;
import com.cdev.corelauncher.ui.entities.LProfile;
import com.cdev.corelauncher.ui.utils.FXManager;
import com.cdev.corelauncher.utils.JavaMan;
import com.cdev.corelauncher.utils.entities.Java;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
        Launcher.getLauncher().getHandler().addHandler("main", this::onLauncherEvent);
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

        new Thread(() -> {
            Launcher.getLauncher().downloadVersion(p.getVersionId());
            Launcher.getLauncher().launch(ExecutionInfo.fromProfile(p));
        }).start();
    }

    private void setUser(Account acc){
        imgHeadView.setImage(acc.getHead());
        lblPlayerName.setText(acc.getUsername());
    }

    private void onLauncherEvent(LauncherEvent a){
        switch (a.getType()) {
            case NEED -> {
                if (a.getKey().startsWith("java")) {
                    Java j = Java.fromVersion(Integer.parseInt(a.getKey().substring(4)));
                    Platform.runLater(() -> status.setText(Translator.translateFormat("launch.state.download.java", j.majorVersion)));
                    JavaMan.getManager().download(j, (b) -> Platform.runLater(() -> {
                        detailedStatus.setText("%" + (b * 100));
                        prg.setProgress(b);
                    }));
                    Launcher.getLauncher().launch((ExecutionInfo) a.getValue());
                }
            }
            case PROGRESS -> Platform.runLater(() -> {
                detailedStatus.setText("%" + ((double) a.getValue() * 100));
                prg.setProgress((double) a.getValue());
            });
            case STATE -> {
                String status;

                if (a.getKey().equals("clientDownload"))
                    status = Translator.translate("launch.state.download.client");
                else if (a.getKey().startsWith("lib")){
                    status = Translator.translate("launch.state.download.libraries");
                    Platform.runLater(() -> detailedStatus.setText(a.getKey().substring(3)));
                }
                else if (a.getKey().startsWith("asset")){
                    status = Translator.translate("launch.state.download.assets");
                    Platform.runLater(() -> detailedStatus.setText(a.getKey().substring(6)));
                }
                else if (a.getKey().equals("gameStart"))
                    status = Translator.translate("launch.state.started");
                else if (a.getKey().equals("gameEnd")){
                    status = "";
                    Platform.runLater(() -> {
                        prg.setProgress(0);
                        detailedStatus.setText("");
                    });
                }
                else
                    status = a.getKey();

                Platform.runLater(() -> this.status.setText(status));
            }
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

        var profile = (LProfile) e.getExtra();

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
