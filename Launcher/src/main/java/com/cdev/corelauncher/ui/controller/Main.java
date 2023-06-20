package com.cdev.corelauncher.ui.controller;

import com.cdev.corelauncher.LauncherConfig;
import com.cdev.corelauncher.data.Configurator;
import com.cdev.corelauncher.data.Profiler;
import com.cdev.corelauncher.data.Translator;
import com.cdev.corelauncher.data.entities.Account;
import com.cdev.corelauncher.data.entities.Profile;
import com.cdev.corelauncher.minecraft.Launcher;
import com.cdev.corelauncher.minecraft.Wrapper;
import com.cdev.corelauncher.minecraft.entities.ExecutionInfo;
import com.cdev.corelauncher.minecraft.wrappers.Vanilla;
import com.cdev.corelauncher.ui.controls.CMsgBox;
import com.cdev.corelauncher.ui.controls.CProfile;
import com.cdev.corelauncher.ui.entities.LProfile;
import com.cdev.corelauncher.ui.utils.FXManager;
import com.cdev.corelauncher.utils.Logger;
import com.cdev.corelauncher.utils.entities.NoConnectionException;
import com.cdev.corelauncher.utils.entities.Path;
import com.cdev.corelauncher.utils.events.ChangeEvent;
import com.cdev.corelauncher.utils.events.KeyEvent;
import com.cdev.corelauncher.utils.events.ProgressEvent;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.stage.FileChooser;

import java.util.Arrays;
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
    private Button btnImportProfile;
    @FXML
    private ListView<LProfile> lvProfiles;
    @FXML
    private Label gameDescription;
    @FXML
    private Label status;
    @FXML
    private Label detailedStatus;

    @FXML
    private AnchorPane root;
    @FXML
    private AnchorPane leftRoot;

    private Profile selectedProfile;
    private ObservableList<LProfile> profiles;

    private static final Background bg = new Background(new BackgroundFill(Paint.valueOf("#202020"), new CornerRadii(0), null));

    public Main(){
        reloadProfiles();

        Launcher.getLauncher().getHandler().addHandler("main", this::onGeneralEvent);
        Vanilla.getVanilla().getHandler().addHandler("main", this::onGeneralEvent);
        Profiler.getProfiler().getHandler().addHandler("mainWindow", this::onProfilerEvent);

        Configurator.getConfigurator().getHandler().addHandler("main", a -> {
            if (a.getKey().equals("bgChange")){
                var path = (Path)a.getNewValue();
                setBackground(path);
            }
        });
    }

    private void setBackground(Path path){
        if (path == null){
            root.setBackground(bg);
            leftRoot.setOpacity(1);
            lvProfiles.setOpacity(1);
        }
        else {
            var nbg = new Background(new BackgroundImage(new Image(path.toString()), null, null, null, null));
            root.setBackground(nbg);
            leftRoot.setOpacity(0.9);
            lvProfiles.setOpacity(0.9);
        }
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
        lvProfiles.setItems(profiles);
        lvProfiles.setCellFactory((x) -> new CProfile());

        btnStart.setOnMouseClicked((a) -> launch(selectedProfile, a.isShiftDown()));

        btnSettings.setOnMouseClicked((a) -> FXManager.getManager().applyStage("settings").show());

        btnAbout.setOnMouseClicked((a) ->
                CMsgBox.msg(Alert.AlertType.INFORMATION, Translator.translate("about.title"), Translator.translateFormat("about.content", LauncherConfig.VERSION, "https://github.com/etkmlm", LauncherConfig.LAUNCHER_NAME)).show());

        btnAddProfile.setOnMouseClicked((a) -> ProfileEdit.open(null));
        btnImportProfile.setOnMouseClicked(a -> {
            var filer = new FileChooser();
            var filters = filer.getExtensionFilters();
            filters.add(new FileChooser.ExtensionFilter("ZIP / JSON", "*.json", "*.zip"));
            var f = filer.showOpenDialog(btnImportProfile.getScene().getWindow());
            if (f == null)
                return;

            try{
                var path = Path.begin(f.toPath());
                Profiler.getProfiler().importProfile(path);
            }
            catch (Exception e){
                Logger.getLogger().log(e);
            }
        });

        try{
            setUser(Configurator.getConfig().getUser().reload());

            if (Configurator.getConfig().getLastSelectedProfile() != null){
                var selected = Profiler.getProfiler().getProfile(Configurator.getConfig().getLastSelectedProfile().getName());
                profiles.stream().filter(x -> x.getProfile() == selected).findFirst().orElse(LProfile.get(selected)).setSelected(true);
            }

            setBackground(Configurator.getConfig().getBackgroundImage());
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }
    }

    public void selectProfile(Profile p){
        selectedProfile = p;

        try{
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
        catch (Exception e){
            Logger.getLogger().log(e);
        }
    }

    private void launch(Profile p, boolean cache){
        if (selectedProfile == null)
            return;

        var wr = (Wrapper<?>)p.getWrapper();
        wr.setDisableCache(cache).getHandler().addHandler("main", this::onGeneralEvent);
        new Thread(() -> {
            try{
                Launcher.getLauncher().prepare(p);
                Launcher.getLauncher().launch(ExecutionInfo.fromProfile(p));

                wr.setDisableCache(false);
            }
            catch (NoConnectionException e){
                Platform.runLater(() -> {
                    prg.setProgress(0);
                    status.setText(Translator.translate("error.connection"));
                    detailedStatus.setText(e.getMessage());
                });
            }
            catch (Exception e){
                Logger.getLogger().log(e);
                Platform.runLater(() -> {
                    prg.setProgress(0);
                    status.setText(Translator.translate("error.unknown"));
                    detailedStatus.setText(e.getMessage());
                });
            }
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
            else if (key.startsWith("sessionStart")){
                status = "";
                Platform.runLater(() -> {
                    detailedStatus.setText(null);
                    prg.setProgress(0);
                });
            }
            else if (key.startsWith("acqVersion"))
                status = Translator.translateFormat("launch.state.acquire", key.substring(10));
            else if (key.startsWith("prepare")){
                status = Translator.translateFormat("launch.state.prepare", key.substring(7));
            }
            else if (key.startsWith("."))
                status = dot(key.substring(1));
            else if (key.startsWith(",")){
                status = dot(key.split(":\\.")[1]);
                Platform.runLater(() -> detailedStatus.setText(key.substring(1)));
            }
            else
                status = key;

            Platform.runLater(() -> this.status.setText(status));

        }
    }

    private String dot(String f){
        String[] spl = f.split(";");
        return spl.length == 1 ? Translator.translate(spl[0]) : Translator.translateFormat(spl[0], Arrays.stream(spl).skip(1));
    }
    private void onProfilerEvent(ChangeEvent a){
        var newProfile = (Profile)a.getNewValue();
        var oldProfile = (Profile)a.getOldValue();

        switch (a.getKey()) {
            case "profileCreate" ->{
                var profile = LProfile.get(newProfile)
                        .setEventListener(this::onProfileSelectEvent);
                profiles.add(profile);
                profile.setSelected(true);
            }
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
