package com.laeben.corelauncher.ui.controller.page;

import com.laeben.core.entity.Path;
import com.laeben.corelauncher.api.ui.entity.Announcement;
import com.laeben.core.util.StrUtil;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.Profiler;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.minecraft.modding.entity.resource.World;
import com.laeben.corelauncher.ui.controller.HandlerController;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.ui.control.*;
import com.laeben.corelauncher.api.ui.UI;
import com.laeben.corelauncher.util.ImageUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class WorldsPage extends HandlerController {
    public static final String KEY = "pgworlds";

    private Profile profile;
    private final ObservableList<String> worlds;
    private List<World> local;

    public WorldsPage(){
        super(KEY);
        worlds = FXCollections.observableArrayList();

        registerHandler(Profiler.getProfiler().getHandler(), a -> {
            if (a.getKey().equals(Profiler.PROFILE_UPDATE))
                reload();
        }, true);
    }

    public WorldsPage setProfile(Profile p){
        this.profile = p;

        reload();
        return this;
    }

    private void reloadTitle(Profile p){
        ((CTab)parentObj).setText(Translator.translate("frame.title.worlds") + " - " + StrUtil.sub(p.getName(), 0, 30));
    }

    public void reload(){
        worlds.clear();
        select(null);

        if (profile == null || profile.isEmpty())
            return;

        lblProfileName.setText(profile.getName());
        reloadTitle(profile);
        profileIcon.setImageAsync(ImageUtil.getImageFromProfile(profile, 32, 32));

        btnBack.enableTransparentAnimation();
        btnBack.setOnMouseClicked(a -> Main.getMain().replaceTab(this, "pages/profile", profile.getName(), true, ProfilePage.class).setProfile(profile));
        btnBack.setText("⤶ " + Translator.translate("option.back"));

        local = profile.getLocalWorlds();
        worlds.addAll(local.stream().map(x -> x.levelName).toList());
    }

    @FXML
    private ListView<String> lvWorlds;
    @FXML
    private Label lblLevelName;
    @FXML
    private Label lblSeed;
    @FXML
    private Label lblDifficulty;
    @FXML
    private Label lblGameType;
    @FXML
    private Label lblSpawn;
    @FXML
    private Label lblCheats;
    @FXML
    private CButton btnBackup;
    @FXML
    private CView icon;
    @FXML
    private CButton btnImport;
    @FXML
    private CButton btnDelete;

    @FXML
    private CButton btnBack;
    @FXML
    private CView profileIcon;
    @FXML
    private Label lblProfileName;

    @FXML
    private CWorker<Void, Void> worker;

    private World selectedWorld;

    @Override
    public void preInit() {
        icon.setCornerRadius(128, 128, 64);
        profileIcon.setCornerRadius(30, 30, 8);

        lvWorlds.setItems(worlds);

        lvWorlds.getSelectionModel().selectedItemProperty().addListener(a -> {
            if (local == null)
                return;

            var s = local.get(lvWorlds.getSelectionModel().getSelectedIndex());

            select(s);
        });

        lblSeed.setOnMouseClicked(a -> {
            if (selectedWorld == null)
                return;

            setClipboard(selectedWorld.seed);
        });

        lblSpawn.setOnMouseClicked(a -> {
            if (selectedWorld == null)
                return;

            setClipboard(selectedWorld.worldSpawn.toString());
        });

        btnDelete.setOnMouseClicked(a -> {
            if (selectedWorld == null)
                return;

            var r = CMsgBox
                    .msg(Alert.AlertType.CONFIRMATION, Translator.translate("ask.ask"), Translator.translate("ask.sure"))
                    .setButtons(CMsgBox.ResultType.YES, CMsgBox.ResultType.NO)
                    .executeForResult();
            if (r.isEmpty() || r.get().result() != CMsgBox.ResultType.YES)
                return;

            profile.getPath().to("saves").to(selectedWorld.dirName).delete();

            UI.runAsync(this::reload);
        });

        btnBackup.setTooltip(new Tooltip(Translator.translate("profile.menu.backup")));
        btnBackup.setOnMouseClicked(a -> {
            if (selectedWorld == null)
                return;

            var worlds = profile.getPath().to("saves");
            var worldFolder = worlds.to(selectedWorld.levelName);
            if (!worldFolder.exists())
                return;

            var chooser = new FileChooser();
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP", "*.zip"));
            chooser.setInitialFileName(selectedWorld.getIdentifier() + ".zip");
            var f = chooser.showSaveDialog(btnBackup.getScene().getWindow());
            if (f == null)
                return;
            var path = Path.begin(f.toPath());
            worker.begin().withTask(k -> new Task() {
                @Override
                protected Void call() {
                    worldFolder.zip(path);
                    return null;
                }
            }).onDone(x -> Main.getMain().announceLater(Translator.translate("world.title"), Translator.translateFormat("world.backup", selectedWorld.levelName), Announcement.AnnouncementType.INFO, Duration.seconds(2))).run();
        });


        btnImport.setOnMouseClicked(a -> {
            var chooser = new FileChooser();
            var p = Configurator.getConfig().getLastBackupPath();

            if (p != null && p.exists())
                chooser.setInitialDirectory(p.toFile());

            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP", "*.zip"));
            var f = chooser.showOpenMultipleDialog(btnImport.getScene().getWindow());
            if (f == null || f.isEmpty())
                return;

            var paths = f.stream().map(x -> Path.begin(x.toPath())).toList();

            Configurator.getConfig().setLastBackupPath(paths.get(0).parent());
            Configurator.save();

            new Thread(() -> {
                Main.getMain().announceLater(Translator.translate("world.title"), Translator.translate("world.import.start"), Announcement.AnnouncementType.INFO, Duration.seconds(2));

                var saves = profile.getPath().to("saves");
                var arr = new ArrayList<String>();
                for (var path : paths){
                    var temp = Configurator.getConfig().getTemporaryFolder().to(path.getNameWithoutExtension());
                    {
                        path.extract(temp, null);

                        var files = temp.getFiles();
                        if (files.size() == 1 && files.get(0).isDirectory())
                            temp = files.get(0);
                    }

                    var world = World.fromGzip(null, temp.to("level.dat"));

                    String name = world.levelName;

                    if (name == null || saves.getFiles().stream().anyMatch(x -> x.getName().equals(name))){
                        temp.delete();
                        continue;
                    }

                    arr.add(name);
                    temp.move(saves.to(StrUtil.pure(name)));
                }

                UI.runAsync(this::reload);
                //Profiler.getProfiler().setProfile(profile.getName(), null);
                if (!arr.isEmpty())
                    Main.getMain().announceLater(Translator.translate("world.title"), Translator.translateFormat("world.import.end", String.join(",", arr)), Announcement.AnnouncementType.INFO, Duration.seconds(2));
            }).start();
        });
    }

    public void setClipboard(Object text){
        Clipboard.getSystemClipboard().setContent(new HashMap<>(){{ put(DataFormat.PLAIN_TEXT, text.toString()); }});
    }

    public void select(World w){
        selectedWorld = w;

        if (w == null){
            lblLevelName.setText(null);
            lblSeed.setText(null);
            lblDifficulty.setText(null);
            lblGameType.setText(null);
            lblSpawn.setText(null);
            lblCheats.setText(null);
            icon.setImage((Image) null);
            return;
        }

        try(var stream = new FileInputStream(w.getWorldIcon().toFile())) {
            var img = new Image(stream);
            //var img2 = CoreLauncherFX.resizeImage(img, 0, 0, 64, 64, 32);
            icon.setImage(img);

        } catch (IOException e) {
            icon.setImage(ImageUtil.getDefaultImage(128));
        }

        lblLevelName.setText(w.levelName);
        lblSeed.setText(String.valueOf(w.seed));
        lblDifficulty.setText(Translator.translate("world.difficulty." + w.difficulty.name().toLowerCase(Locale.US)));
        lblGameType.setText(Translator.translate("world.type." + w.gameType.name().toLowerCase(Locale.US)));
        lblSpawn.setText(w.worldSpawn.toString());
        lblCheats.setText(w.allowCommands ? "+" : "-");
    }
}
