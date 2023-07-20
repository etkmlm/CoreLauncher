package com.laeben.corelauncher.ui.controller;

import com.laeben.core.entity.Path;
import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.corelauncher.data.Translator;
import com.laeben.corelauncher.data.entities.Profile;
import com.laeben.corelauncher.data.nbt.entities.NBTCompound;
import com.laeben.corelauncher.data.nbt.entities.NBTList;
import com.laeben.corelauncher.minecraft.modding.curseforge.CurseForge;
import com.laeben.corelauncher.minecraft.modding.curseforge.entities.ClassType;
import com.laeben.corelauncher.minecraft.modding.curseforge.entities.ModsSearchSortField;
import com.laeben.corelauncher.minecraft.modding.curseforge.entities.Search;
import com.laeben.corelauncher.minecraft.modding.entities.CResource;
import com.laeben.corelauncher.minecraft.modding.entities.Mod;
import com.laeben.corelauncher.minecraft.modding.entities.World;
import com.laeben.corelauncher.minecraft.wrappers.forge.Forge;
import com.laeben.corelauncher.ui.controls.CButton;
import com.laeben.corelauncher.ui.controls.CMod;
import com.laeben.corelauncher.ui.controls.CMsgBox;
import com.laeben.corelauncher.ui.entities.LMod;
import com.laeben.corelauncher.ui.entities.LStage;
import com.laeben.corelauncher.ui.utils.FXManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultipleMod {

    private Profile profile;

    public static LStage open(Profile p){
        var f = FXManager.getManager().applyStage("multiplemod");
        var cont = (MultipleMod)f.getLScene().getController();
        cont.profile = p;

        return f;
    }

    @FXML
    public TextArea txtQuery;
    @FXML
    public ListView lvMods;
    @FXML
    public CButton btnFromWorld;
    @FXML
    public CButton btnFromFolder;
    @FXML
    public CButton btnSearch;
    @FXML
    public CButton btnApply;
    @FXML
    public Label lblStatus;

    private ScrollPane pane;

    private final ObservableList<LMod> mods;

    public MultipleMod(){
        mods = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize(){
        txtQuery.promptTextProperty().setValue(Translator.translate("import.multiple.hint"));
        txtQuery.setCache(false);
        txtQuery.textProperty().addListener(x -> {
            if (pane == null){
                pane = (ScrollPane) txtQuery.getChildrenUnmodifiable().get(0);
                pane.setCache(false);
            }

            for (var node : pane.getChildrenUnmodifiable())
                node.setCache(false);
        });

        btnFromWorld.setOnMouseClicked(a -> {

            if (!(profile.getWrapper() instanceof Forge)){
                CMsgBox.msg(Alert.AlertType.INFORMATION, Translator.translate("error.oops"), Translator.translate("import.multiple.forge")).show();
                return;
            }

            var chooser = new DirectoryChooser();
            chooser.setInitialDirectory(profile.getPath().to("saves").toFile());
            var file = chooser.showDialog(btnFromWorld.getScene().getWindow());

            if (file == null)
                return;

            var path = Path.begin(file.toPath());
            var dat = path.to("level.dat");
            if (!dat.exists())
                return;

            var nbt = World.openNBT(dat);
            if (nbt == null)
                return;
            var fml = (NBTList)nbt.first().asCompound().firstForName("FML").asCompound().firstForName("ModList");
            var ids = fml.getItems().stream().map(x -> x.asCompound().firstForName("ModId").stringValue()).filter(x -> !x.equals("minecraft") && !x.equals("forge") && !x.equals("FML") && !x.equals("mcp")).toList();

            txtQuery.setText(txtQuery.getText() + "\n\r" + String.join("\n\r", ids));
        });

        btnFromFolder.setOnMouseClicked(a -> {
            var chooser = new DirectoryChooser();
            chooser.setInitialDirectory(profile.getPath().to("mods").toFile());
            var file = chooser.showDialog(btnFromWorld.getScene().getWindow());

            if (file == null)
                return;

            var path = Path.begin(file.toPath());

            var files = path.getFiles().stream().filter(x -> x.getExtension() != null && x.getExtension().equals("jar")).map(x -> x.getNameWithoutExtension().split("-")[0]).toList();
            files.forEach(this::println);
        });

        btnSearch.setOnMouseClicked(a -> {
            var lines = txtQuery.getText().lines().filter(x -> !x.isBlank()).toList();

            new Thread(() -> {
                var secondary = new ArrayList<String>();

                String version = profile.getVersionId();

                var s = new Search();
                s.gameVersion = version;
                s.classId = ClassType.MOD.getId();
                s.sortField = ModsSearchSortField.POPULARITY;
                s.setSortOrder(false);
                s.pageSize = 1;

                int i = 1;
                int size = lines.size();
                for (var line : lines){
                    s.setSearchFilter(line);
                    var f = CurseForge.getForge().search(s);
                    if (f == null || f.data.size() == 0){
                        secondary.add(line);
                        continue;
                    }

                    var full = CurseForge.getForge().getFullResource(version, profile.getWrapper().getType(), f.data.get(0));

                    var r = Mod.fromResource(version, profile.getWrapper().getIdentifier(), full);
                    String status = i++ + "/" + size;
                    Platform.runLater(() -> {
                        if (mods.stream().noneMatch(x -> x.get().name.equals(r.name)))
                            mods.add(new LMod(r, profile).setAction(mods::remove));
                        lblStatus.setText(status);
                    });
                }

                Platform.runLater(() -> {
                    txtQuery.setText(null);
                    secondary.forEach(this::println);
                });
            }).start();
        });

        btnApply.setOnMouseClicked(a -> new Thread(() -> {
            int i = 1;
            int size = mods.size();
            for (var m : mods){
                var mod = (Mod)m.get();
                String status = i++ + "/" + size;
                Platform.runLater(() -> lblStatus.setText(status));
                if (profile.getMods().stream().anyMatch(x -> x.name.equals(mod.name)))
                    continue;

                try {
                    CurseForge.getForge().include(profile, mod);
                } catch (NoConnectionException | HttpException ignored) {

                }
            }

            Platform.runLater(() -> FXManager.getManager().closeStage(btnApply.getScene().getWindow()));
        }).start());

        lvMods.setCellFactory(x -> new CMod());
        lvMods.setItems(mods);
    }

    private void println(String text){
        String f = txtQuery.getText();
        if (txtQuery.getText() != null && !txtQuery.getText().isBlank())
            f += "\n\r";
        else
            f = "\n\r" + f;

        txtQuery.setText(f + text);
    }
}
