package com.laeben.corelauncher.ui.controller;

import com.laeben.core.entity.Path;
import com.laeben.corelauncher.data.Configurator;
import com.laeben.corelauncher.data.Profiler;
import com.laeben.corelauncher.data.Translator;
import com.laeben.corelauncher.data.entities.Profile;
import com.laeben.corelauncher.minecraft.modding.entities.World;
import com.laeben.corelauncher.ui.entities.LStage;
import com.laeben.corelauncher.ui.utils.FXManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.stage.FileChooser;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class Worlds {
    private Profile profile;

    public static LStage open(Profile p){
        var mds = FXManager.getManager().applyStage("worlds");
        var ths = (Worlds)mds.getLScene().getController();
        ths.profile = p;
        ths.reload();

        return mds;
    }

    private final ObservableList<String> worlds;
    private List<World> local;

    public Worlds(){
        worlds = FXCollections.observableArrayList();

        Profiler.getProfiler().getHandler().addHandler("worlds", a -> {
            if (!a.getKey().equals("profileUpdate"))
                return;

            reload();
        }, true);
    }

    public void reload(){
        worlds.clear();

        if (profile == null || profile.isEmpty())
            return;

        local = profile.getLocalWorlds();
        worlds.addAll(local.stream().map(x -> x.levelName).toList());
    }

    @FXML
    public ListView<String> lvWorlds;
    @FXML
    public Label lblLevelName;
    @FXML
    public Label lblSeed;
    @FXML
    public Label lblDifficulty;
    @FXML
    public Label lblGameType;
    @FXML
    public Label lblSpawn;
    @FXML
    public Label lblCheats;
    @FXML
    public Button btnBackup;
    @FXML
    public Button btnImport;
    private World selectedWorld;

    @FXML
    public void initialize(){
        lvWorlds.setItems(worlds);

        lvWorlds.getSelectionModel().selectedItemProperty().addListener(a -> {
            var item = lvWorlds.getSelectionModel().getSelectedItem();

            if (local == null)
                return;

            var s = local.stream().filter(x -> x.levelName.equals(item)).findFirst();

            s.ifPresent(this::select);
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

            new Thread(() -> worldFolder.zip(path)).start();
        });

        btnImport.setOnMouseClicked(a -> {
            var chooser = new FileChooser();
            var p = Configurator.getConfig().getLastBackupPath();

            if (p != null && p.exists())
                chooser.setInitialDirectory(p.toFile());

            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP", "*.zip"));
            var f = chooser.showOpenDialog(btnImport.getScene().getWindow());
            if (f == null)
                return;
            var path = Path.begin(f.toPath());

            Configurator.getConfig().setLastBackupPath(path.parent());
            Configurator.save();

            var saves = profile.getPath().to("saves");
            var temp = Configurator.getConfig().getTemporaryFolder().to(path.getNameWithoutExtension());
            Path w;
            path.extract(temp, null);

            var files = temp.getFiles();
            if (files.size() == 1 && files.get(0).isDirectory())
                w = files.get(0);
            else
                w = temp;

            String name = w.getName();

            if (saves.getFiles().stream().anyMatch(x -> x.getName().equals(name))){
                temp.delete();
                return;
            }

            w.move(saves.to(name));


            Profiler.getProfiler().setProfile(profile.getName(), null);
        });
    }

    public void setClipboard(Object text){
        Clipboard.getSystemClipboard().setContent(new HashMap<>(){{ put(DataFormat.PLAIN_TEXT, text.toString()); }});
    }

    public void select(World w){
        selectedWorld = w;

        lblLevelName.setText(w.levelName);
        lblSeed.setText(String.valueOf(w.seed));
        lblDifficulty.setText(Translator.translate("world.difficulty." + w.difficulty.name().toLowerCase(Locale.US)));
        lblGameType.setText(Translator.translate("world.type." + w.gameType.name().toLowerCase(Locale.US)));
        lblSpawn.setText(w.worldSpawn.toString());
        lblCheats.setText(w.allowCommands ? "+" : "-");
    }
}
