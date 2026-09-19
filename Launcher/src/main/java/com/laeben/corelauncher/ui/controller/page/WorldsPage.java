package com.laeben.corelauncher.ui.controller.page;

import com.laeben.core.entity.Path;
import com.laeben.corelauncher.api.concurrency.TaskRecord;
import com.laeben.corelauncher.api.concurrency.Tasker;
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
import com.laeben.corelauncher.ui.controller.worlds.cell.WorldCell;
import com.laeben.corelauncher.ui.controller.worlds.cell.WorldCellItem;
import com.laeben.corelauncher.util.ImageUtil;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class WorldsPage extends HandlerController {
    public static final String KEY = "pgworlds";

    private final Tasker tasker;
    private final ObjectProperty<WorldCellItem> selectedItem;

    private final ChangeListener<TaskRecord> exportTaskListener;

    private Profile profile;

    public WorldsPage(){
        super(KEY);

        tasker = new Tasker();

        selectedItem = new SimpleObjectProperty<>();

        exportTaskListener = (ob, o, n) -> {
            UI.runAsync(() -> setExportingState(n != null));
        };

        selectedItem.addListener(((observable, oldValue, item) -> {
            final var world = item.getWorld();

            if (oldValue != null) {
                oldValue.exportRecord().removeListener(exportTaskListener);
            }

            if (world == null){
                lblLevelName.setText(null);
                lblDirName.setText(null);
                lblSeed.setText(null);
                lblDifficulty.setText(null);
                lblGameType.setText(null);
                lblSpawn.setText(null);
                lblCheats.setText(null);
                icon.setImage((Image) null);
                setExportingState(false);
                return;
            }

            item.exportRecord().addListener(exportTaskListener);
            setExportingState(item.exportRecord().isRunning());

            try(var stream = new FileInputStream(world.getWorldIcon().toFile())) {
                var img = new Image(stream);
                //var img2 = CoreLauncherFX.resizeImage(img, 0, 0, 64, 64, 32);
                icon.setImage(img);

            } catch (IOException e) {
                icon.setImage(ImageUtil.getDefaultImage(128));
            }

            lblLevelName.setText(world.levelName);
            lblDirName.setText(world.dirName);
            lblSeed.setText(String.valueOf(world.seed));
            lblDifficulty.setText(Translator.translate("world.difficulty." + world.difficulty.name().toLowerCase(Locale.US)));
            lblGameType.setText(Translator.translate("world.type." + world.gameType.name().toLowerCase(Locale.US)));
            lblSpawn.setText(world.worldSpawn.toString());
            lblCheats.setText(world.allowCommands ? "+" : "-");
        }));

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
        lvWorlds.getItems().clear();
        select(null);

        if (profile == null)
            return;

        lblProfileName.setText(profile.getName());
        reloadTitle(profile);
        profileIcon.setImageAsync(ImageUtil.getImageFromProfile(profile, 32, 32));

        btnBack.enableTransparentAnimation();
        btnBack.setOnMouseClicked(a -> Main.getMain().replaceTab(this, "pages/profile", profile.getName(), true, ProfilePage.class).setProfile(profile));
        btnBack.setText("⤶ " + Translator.translate("option.back"));

        lvWorlds.getItems().addAll(profile.getLocalWorlds().stream().map(a -> new WorldCellItem(a, tasker, this::deleteDuplicate)).toList());
    }

    @FXML
    private CVirtualList<WorldCellItem> lvWorlds;
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
    private CButton btnCancelExport;
    @FXML
    private HBox exportContainer;

    private void setExportingState(boolean s){
        exportContainer.setVisible(s);
        exportContainer.setManaged(s);
    }

    private void deleteDuplicate(String dirName){
        if (dirName == null) return;
        UI.runAsync(() -> lvWorlds.getItems().removeIf(a -> a.getWorld() != null && dirName.equals(a.getWorld().dirName)));
    }

    @Override
    public void preInit() {
        icon.setCornerRadius(128, 128, 64);
        profileIcon.setCornerRadius(30, 30, 8);

        lvWorlds.setCellFactory(() -> new WorldCell().setSelectionProperty(selectedItem));

        btnCancelExport.setOnMouseClicked(a -> {
            final var item = selectedItem.get();
            if (item != null) item.exportRecord().cancelIfRunning();
        });

        lblDirName.setOnMouseClicked(a -> {
            if (getSelected() == null)
                return;

            OSUtil.open(profile.getPath().to("saves").to(getSelected().dirName).toFile());
        });

        lblSeed.setOnMouseClicked(a -> {
            if (getSelected() == null)
                return;

            setClipboard(getSelected().seed);
        });

        lblSpawn.setOnMouseClicked(a -> {
            if (getSelected() == null)
                return;

            setClipboard(getSelected().worldSpawn.toString());
        });

        btnDelete.setOnMouseClicked(a -> {
            if (getSelected() == null)
                return;

            var r = showMsg(Alert.AlertType.CONFIRMATION, Translator.translate("ask.ask"), Translator.translate("ask.sure"))
                    .setButtons(CMsgBox.ResultType.YES, CMsgBox.ResultType.NO)
                    .executeForResult();
            if (r.isEmpty() || r.get().result() != CMsgBox.ResultType.YES)
                return;

            profile.getPath().to("saves").to(getSelected().dirName).delete();

            UI.runAsync(this::reload);
        });

        btnBackup.setTooltip(Translator.translate("profile.menu.backup"));
        btnBackup.setOnMouseClicked(a -> {
            final var item = selectedItem.get();
            if (item == null || item.getWorld() == null)
                return;

            var worlds = profile.getPath().to("saves");

            var chooser = new FileChooser();
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP", "*.zip"));
            chooser.setInitialFileName(getSelected().getIdentifier() + ".zip");
            var f = chooser.showSaveDialog(btnBackup.getScene().getWindow());
            if (f == null)
                return;
            var path = Path.begin(f.toPath());

            item.handleWorldExport(path, worlds);
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

            Main.getMain().announceLater(Translator.translate("world.title"), Translator.translate("world.import.start"), Announcement.AnnouncementType.INFO, Duration.seconds(2));

            var saves = profile.getPath().to("saves");
            //var arr = new ArrayList<String>();
            for (var path : paths){
                var item = new WorldCellItem(tasker, this::deleteDuplicate);
                lvWorlds.getItems().add(item);
                item.handleWorldImport(path, saves);
            }

            //Profiler.getProfiler().setProfile(profile.getName(), null);
            /*if (!arr.isEmpty()){
                UI.runAsync(this::reload);
                Main.getMain().announceLater(Translator.translate("world.title"), Translator.translateFormat("world.import.end", String.join(",", arr)), Announcement.AnnouncementType.INFO, Duration.seconds(2));
            }*/
        });
    }

    public World getSelected(){
        return selectedItem.get() == null ? null : selectedItem.get().getWorld();
    }
    public void select(World w){
        selectedItem.set(new WorldCellItem(w, tasker, null));
    }

    public void setClipboard(Object text){
        Clipboard.getSystemClipboard().setContent(new HashMap<>(){{ put(DataFormat.PLAIN_TEXT, text.toString()); }});
    }
}
