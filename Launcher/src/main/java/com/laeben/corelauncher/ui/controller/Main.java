package com.laeben.corelauncher.ui.controller;

import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.core.util.Cat;
import com.laeben.core.util.events.BaseEvent;
import com.laeben.corelauncher.LauncherConfig;
import com.laeben.corelauncher.data.Configurator;
import com.laeben.corelauncher.data.Profiler;
import com.laeben.corelauncher.data.Translator;
import com.laeben.corelauncher.data.entities.Account;
import com.laeben.corelauncher.data.entities.Profile;
import com.laeben.corelauncher.minecraft.Launcher;
import com.laeben.corelauncher.minecraft.Wrapper;
import com.laeben.corelauncher.minecraft.entities.ExecutionInfo;
import com.laeben.corelauncher.minecraft.modding.curseforge.CurseForge;
import com.laeben.corelauncher.minecraft.wrappers.Vanilla;
import com.laeben.corelauncher.ui.controls.CMsgBox;
import com.laeben.corelauncher.ui.controls.CProfile;
import com.laeben.corelauncher.ui.entities.LProfile;
import com.laeben.corelauncher.ui.entities.LStage;
import com.laeben.corelauncher.ui.utils.FXManager;
import com.laeben.corelauncher.utils.Logger;
import com.laeben.core.entity.Path;
import com.laeben.core.util.events.ChangeEvent;
import com.laeben.core.util.events.KeyEvent;
import com.laeben.core.util.events.ProgressEvent;
import com.laeben.corelauncher.utils.NetUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.ImagePattern;
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
    private TextArea detailedStatus;

    @FXML
    private AnchorPane root;
    @FXML
    private AnchorPane leftRoot;
    @FXML
    private TextField txtSearch;

    private Profile selectedProfile;
    private ObservableList<LProfile> profiles;

    private static final Background bg = new Background(new BackgroundFill(Paint.valueOf("#202020"), new CornerRadii(0), null));

    public Main(){
        reloadProfiles();

        Launcher.getLauncher().getHandler().addHandler("main", this::onGeneralEvent, true);
        Vanilla.getVanilla().getHandler().addHandler("main", this::onGeneralEvent, true);
        CurseForge.getForge().getHandler().addHandler("main", this::onGeneralEvent, true);
        Profiler.getProfiler().getHandler().addHandler("main", this::onProfilerEvent, true);
        NetUtils.getHandler().addHandler("main", this::onProgressEvent, true);
        FXManager.getManager().getHandler().addHandler("main", this::onFXEvent, true);

        Configurator.getConfigurator().getHandler().addHandler("main", a -> {
            if (a.getKey().equals("bgChange")){
                var path = (Path)a.getNewValue();
                setBackground(path);
            }
        }, true);
    }

    private void setBackground(Path path){
        if (path == null){
            root.setBackground(bg);
            leftRoot.setOpacity(1);
            lvProfiles.setOpacity(1);
            btnSettings.setOpacity(1);
            btnAbout.setOpacity(1);
            txtSearch.setOpacity(1);
        }
        else {
            var fill = new BackgroundFill(new ImagePattern(new Image(path.toString())), null, null);
            var nbg = new Background(fill);
            root.setBackground(nbg);
            leftRoot.setOpacity(0.9);
            lvProfiles.setOpacity(0.9);
            btnSettings.setOpacity(0.9);
            btnAbout.setOpacity(0.9);
            txtSearch.setOpacity(0.9);
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

        btnStart.setOnMouseClicked((a) -> {
            if (selectedProfile == null)
                return;
            if (btnStart.getText().equals("⯈"))
                launch(selectedProfile, a.isShiftDown());
            else{
                selectedProfile.getWrapper().setStopRequested(true);
                Vanilla.getVanilla().setStopRequested(true);
                NetUtils.stop();
            }
        });

        btnSettings.setOnMouseClicked((a) -> FXManager.getManager().applyStage("settings").show());

        btnAbout.setOnMouseClicked((a) ->
                CMsgBox.msg(Alert.AlertType.INFORMATION, Translator.translate("about.title"), Translator.translateFormat("about.content", LauncherConfig.VERSION, "https://github.com/etkmlm", LauncherConfig.APPLICATION.getName())).show());

        txtSearch.textProperty().addListener(a -> {
            var text = txtSearch.getText();
            if (text == null || text.isBlank())
                lvProfiles.setItems(profiles);
            else
                lvProfiles.setItems(profiles.filtered(x -> x.getProfile().getName().toLowerCase().contains(text.toLowerCase())));
        });

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
            Platform.runLater(() -> {
                if (p != null){
                    gameVersion.setText(p.getVersionId());
                    gameDescription.setText(p.getName());
                    setUser(p.getUser() == null ? Configurator.getConfig().getUser().reload() : p.getUser().reload());
                }
                else{
                    gameVersion.setText(null);
                    gameDescription.setText(null);
                    setUser(Configurator.getConfig().getUser().reload());
                }

                Configurator.getConfig().setLastSelectedProfile(p);
                Configurator.save();
            });
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }
    }

    private void launch(Profile p, boolean cache){
        var wr = (Wrapper<?>)p.getWrapper();
        Vanilla.getVanilla().setDisableCache(cache);
        wr.setDisableCache(cache).getHandler().addHandler("main", this::onGeneralEvent, true);
        btnStart.setText("⏸");

        var task = new Task<>() {
            @Override
            protected Object call() throws NoConnectionException, StopException, HttpException {
                Launcher.getLauncher().prepare(p);
                Cat.sleep(200);
                Platform.runLater(() -> {
                    status.setText(null);
                    detailedStatus.setText(null);
                    prg.setProgress(0);
                    btnStart.setText("⯈");
                });

                if (wr.isStopRequested()){
                    wr.setStopRequested(false);
                    Vanilla.getVanilla().setStopRequested(false);
                    return null;
                }

                Launcher.getLauncher().launch(ExecutionInfo.fromProfile(p));

                Cat.sleep(200);
                if (Configurator.getConfig().hideAfter())
                    FXManager.getManager().showAll();
                Platform.runLater(() -> lvProfiles.refresh());
                wr.setDisableCache(false);
                Vanilla.getVanilla().setDisableCache(false);

                return null;
            }
        };

        task.setOnFailed(a -> {
            var f = a.getSource().getException();
            if (f instanceof NoConnectionException){
                Cat.sleep(200);
                Platform.runLater(() -> {
                    wr.setStopRequested(false);
                    Vanilla.getVanilla().setStopRequested(false);
                    prg.setProgress(0);
                    status.setText(Translator.translate("error.connection"));
                    btnStart.setText("⯈");
                });
            }
            else if (f instanceof StopException){
                Cat.sleep(200);
                Platform.runLater(() -> {
                    wr.setStopRequested(false);
                    Vanilla.getVanilla().setStopRequested(false);
                    status.setText(null);
                    detailedStatus.setText(null);
                    prg.setProgress(0);
                    btnStart.setText("⯈");
                });
            }
            else if (f instanceof Exception e){
                Logger.getLogger().log(e);
                Cat.sleep(200);
                Platform.runLater(() -> {
                    wr.setStopRequested(false);
                    Vanilla.getVanilla().setStopRequested(false);
                    prg.setProgress(0);
                    status.setText(Translator.translate("error.unknown"));
                    detailedStatus.setText(e.getMessage());
                    btnStart.setText("⯈");
                });
            }
        });

        new Thread(task).start();
    }

    private void setUser(Account acc){
        imgHeadView.setImage(acc.getHead());
        lblPlayerName.setText(acc.getUsername());
    }

    private void onGeneralEvent(BaseEvent e){
        if (e instanceof ProgressEvent p){
            onProgressEvent(p);
        }
        else if (e instanceof KeyEvent k){
            String key = k.getKey();
            if (key.startsWith("java")){
                String major = k.getKey().substring(4);
                status.setText(Translator.translateFormat("launch.state.download.java", major));
            }
            else if (key.equals("clientDownload"))
                status.setText(Translator.translate("launch.state.download.client"));
            else if (key.startsWith("lib")){
                status.setText(key.substring(3));
            }
            else if (key.equals("jvdown")){
                status.setText(null);
                detailedStatus.setText(null);
                prg.setProgress(0);
            }
            else if (key.startsWith("sessionEnd")){
                String code = key.substring(10);
                status.setText(code.equals("0") ? "" : Translator.translateFormat("error.crash", code));
            }
            else if (key.startsWith("asset")){
                status.setText(Translator.translate("launch.state.download.assets") + " " + key.substring(5));
            }
            else if (key.startsWith("sessionStart")){
                status.setText(null);
                selectedProfile.getWrapper().setStopRequested(false);
                Vanilla.getVanilla().setStopRequested(false);
                detailedStatus.setText(null);
                prg.setProgress(0);
                btnStart.setText("⯈");
                if (Configurator.getConfig().hideAfter())
                    FXManager.getManager().hideAll();
            }
            else if (key.startsWith("acqVersion"))
                status.setText(Translator.translateFormat("launch.state.acquire", key.substring(10)));
            else if (key.startsWith("prepare")){
                status.setText(Translator.translateFormat("launch.state.prepare", key.substring(7)));
            }
            else if (key.startsWith("."))
                status.setText(dot(key.substring(1)));
            else if (key.startsWith(",")){
                var s = key.split(":\\.");
                status.setText(dot(s[1]));
                detailedStatus.setText(s[0].substring(1));
            }
            else if (key.equals("stop")){
                status.setText(null);
                detailedStatus.setText(null);
                prg.setProgress(0);
            }
            else
                status.setText(key);
        }
    }

    private void onProgressEvent(ProgressEvent p){
        Platform.runLater(() -> {
            String id = p.getKey().equals("download") ? "mb" : p.getKey();
            detailedStatus.setText(p.getRemain() + id + " / " + p.getTotal() + id);
            prg.setProgress(p.getProgress());
        });
    }

    private String dot(String f){
        String[] spl = f.split(";");
        return spl.length == 1 ? Translator.translate(spl[0]) : Translator.getTranslator().getTranslateFormat(spl[0], Arrays.stream(spl).skip(1).map(x -> (Object) x).toList());
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
                if (profiles.stream().noneMatch(LProfile::selected)) {
                    if (profiles.size() > 0)
                        profiles.stream().findFirst().get().setSelected(true);
                    else{
                        selectProfile(null);
                    }
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
    private void onFXEvent(BaseEvent a){
        if (!(a instanceof KeyEvent e))
            return;
        if (!e.getKey().equals("close"))
            return;
        var stage = (LStage)e.getSource();
        if (!stage.getName().equals("settings"))
            return;
        if (selectedProfile != null && !selectedProfile.isEmpty())
            return;

        setUser(Configurator.getConfig().getUser().reload());
    }
}
