package com.laeben.corelauncher.ui.controller.page;

import com.laeben.core.entity.Path;
import com.laeben.corelauncher.api.ui.entity.Announcement;
import com.laeben.core.util.StrUtil;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.Profiler;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.api.util.OSUtil;
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
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

public class WorldsPage extends HandlerController {
    public static final String KEY = "pgworlds";

    private Profile profile;
    private final ObservableList<World> worlds;

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
        ((CTab)parentObj).setText(Translator.translate("world.title") + " - " + StrUtil.sub(p.getName(), 0, 18));
    }

    public void reload(){
        worlds.clear();
        select(null);

        if (profile == null)
            return;

        lblProfileName.setText(profile.getName());
        reloadTitle(profile);
        profileIcon.setImageAsync(ImageUtil.getImageFromProfile(profile, 32, 32));

        btnBack.enableTransparentAnimation();
        btnBack.setOnMouseClicked(a -> Main.getMain().replaceTab(this, "pages/profile", profile.getName(), true, ProfilePage.class).setProfile(profile));
        btnBack.setText("⤶ " + Translator.translate("option.back"));

        worlds.addAll(profile.getLocalWorlds());
    }

    @FXML
    private ListView<World> lvWorlds;
    @FXML
    private Label lblLevelName;
    @FXML
    private Label lblDirName;
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
        lvWorlds.setCellFactory(a -> new ListCell<>() {
            private World item;
            {
                setPrefWidth(0);
                setOnMouseEntered(a -> {
                    if (item != null)
                        setText(item.dirName);
                });
                setOnMouseExited(a -> {
                    if (item != null)
                        setText(item.levelName);
                });
            }

            @Override
            protected void updateItem(World item, boolean empty) {
                super.updateItem(item, empty);
                this.item = item;
                setText(item == null ? null : item.levelName);
            }
        });
        lvWorlds.getSelectionModel().selectedItemProperty().addListener((a, o, n) -> select(n));

        lblDirName.setOnMouseClicked(a -> {
            if (selectedWorld == null)
                return;

            OSUtil.open(profile.getPath().to("saves").to(selectedWorld.dirName).toFile());
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

            var r = showMsg(Alert.AlertType.CONFIRMATION, Translator.translate("ask.ask"), Translator.translate("ask.sure"))
                    .setButtons(CMsgBox.ResultType.YES, CMsgBox.ResultType.NO)
                    .executeForResult();
            if (r.isEmpty() || r.get().result() != CMsgBox.ResultType.YES)
                return;

            profile.getPath().to("saves").to(selectedWorld.dirName).delete();

            UI.runAsync(this::reload);
        });

        btnBackup.setTooltip(Translator.translate("profile.menu.backup"));
        btnBackup.setOnMouseClicked(a -> {
            if (selectedWorld == null)
                return;

            var worlds = profile.getPath().to("saves");
            var worldFolder = worlds.to(selectedWorld.dirName);
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
                    String dirName = path.getNameWithoutExtension();
                    var temp = Configurator.getConfig().getTemporaryFolder().to(dirName);
                    {
                        path.extract(temp, null);

                        var files = temp.getFiles();
                        if (files.size() == 1 && files.get(0).isDirectory()){
                            dirName = files.get(0).getName();
                            files.get(0).move(temp);
                        }
                    }

                    var world = World.fromGzip(null, temp.to("level.dat"));

                    String name = world.levelName;
                    final String finalDirName = dirName;

                    if (name == null){
                        temp.delete();
                        continue;
                    }

                    if (saves.getFiles().stream().anyMatch(x -> x.getName().equals(finalDirName))){
                        final var result = UI.runSync(() -> CMsgBox.msg(Alert.AlertType.CONFIRMATION, Translator.translate("ask.sure"), Translator.translateFormat("world.ask.overwrite", finalDirName))
                                .setButtons(CMsgBox.ResultType.YES, CMsgBox.ResultType.NO, CMsgBox.ResultType.CANCEL)
                                .executeForResult()
                                .map(CMsgBox.Result::result));

                        if (result == null){
                            temp.delete();
                            continue;
                        }
                        final var confirmation = result.orElse(null);

                        if (confirmation == null || confirmation == CMsgBox.ResultType.CANCEL){
                            temp.delete();
                            continue;
                        }
                        else if (confirmation.isPositive()){
                            saves.to(finalDirName).delete();
                        }
                        else{
                            String salt = String.valueOf(Instant.now().toEpochMilli());
                            dirName = finalDirName + salt;
                        }
                    }

                    arr.add(name);
                    temp.move(saves.to(StrUtil.pure(dirName)));
                }


                //Profiler.getProfiler().setProfile(profile.getName(), null);
                if (!arr.isEmpty()){
                    UI.runAsync(this::reload);
                    Main.getMain().announceLater(Translator.translate("world.title"), Translator.translateFormat("world.import.end", String.join(",", arr)), Announcement.AnnouncementType.INFO, Duration.seconds(2));
                }
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
            lblDirName.setText(null);
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
        lblDirName.setText(w.dirName);
        lblSeed.setText(String.valueOf(w.seed));
        lblDifficulty.setText(Translator.translate("world.difficulty." + w.difficulty.name().toLowerCase(Locale.US)));
        lblGameType.setText(Translator.translate("world.type." + w.gameType.name().toLowerCase(Locale.US)));
        lblSpawn.setText(w.worldSpawn.toString());
        lblCheats.setText(w.allowCommands ? "+" : "-");
    }
}
