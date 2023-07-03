package com.cdev.corelauncher.ui.controller;

import com.cdev.corelauncher.data.Profiler;
import com.cdev.corelauncher.data.Translator;
import com.cdev.corelauncher.data.entities.Profile;
import com.cdev.corelauncher.minecraft.modding.curseforge.CurseForge;
import com.cdev.corelauncher.minecraft.modding.curseforge.entities.ClassType;
import com.cdev.corelauncher.minecraft.modding.entities.Mod;
import com.cdev.corelauncher.minecraft.modding.entities.Modpack;
import com.cdev.corelauncher.minecraft.modding.entities.Resourcepack;
import com.cdev.corelauncher.minecraft.modding.entities.World;
import com.cdev.corelauncher.minecraft.modding.modrinth.Modrinth;
import com.cdev.corelauncher.minecraft.wrappers.fabric.Fabric;
import com.cdev.corelauncher.minecraft.wrappers.forge.Forge;
import com.cdev.corelauncher.minecraft.wrappers.optifine.OptiFine;
import com.cdev.corelauncher.minecraft.wrappers.optifine.entities.OptiVersion;
import com.cdev.corelauncher.minecraft.wrappers.quilt.Quilt;
import com.cdev.corelauncher.ui.controls.CMod;
import com.cdev.corelauncher.ui.controls.CMsgBox;
import com.cdev.corelauncher.ui.entities.LMod;
import com.cdev.corelauncher.ui.entities.LStage;
import com.cdev.corelauncher.ui.utils.FXManager;
import com.cdev.corelauncher.utils.NetUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Mods {

    private Profile profile;

    public static LStage open(Profile p){
        var mds = FXManager.getManager().applyStage("mods");
        var ths = (Mods)mds.getLScene().getController();
        ths.profile = p;
        ths.reload();

        return mds;
    }

    @FXML
    public ListView lvMods;
    @FXML
    public ListView lvWorlds;
    @FXML
    public ListView lvResources;
    @FXML
    public ListView lvModpacks;
    @FXML
    public TextField txtSearch;
    @FXML
    public Button btnBrowse;
    @FXML
    public Button btnSodium;
    @FXML
    public Button btnOpti;
    @FXML
    public Button btnUpdate;

    private final ObservableList<LMod> mods;
    private final ObservableList<LMod> resources;
    private final ObservableList<LMod> modpacks;
    private final ObservableList<LMod> worlds;

    private final ContextMenu cmOptifine;
    private final ContextMenu cmSodium;

    public Mods(){
        mods = FXCollections.observableArrayList();
        modpacks = FXCollections.observableArrayList();
        resources = FXCollections.observableArrayList();
        worlds = FXCollections.observableArrayList();

        Profiler.getProfiler().getHandler().addHandler("mods", a -> {
            if (a.getKey().equals("profileUpdate")){
                reload();
            }
        });

        String cStyle = "-fx-background-color: #252525;";
        cmOptifine = new ContextMenu();
        cmSodium = new ContextMenu();
        cmOptifine.setStyle(cStyle);
        cmSodium.setStyle(cStyle);
    }

    public void reload(){
        mods.clear();
        modpacks.clear();
        worlds.clear();
        resources.clear();
        txtSearch.clear();

        if (profile == null)
            return;

        mods.addAll(profile.getMods().stream().map(x -> new LMod(x, profile).setAction(this::onRemove)).toList());
        modpacks.addAll(profile.getModpacks().stream().map(x -> new LMod(x, profile).setAction(this::onRemove)).toList());
        resources.addAll(profile.getResources().stream().map(x -> new LMod(x, profile).setAction(this::onRemove)).toList());
        worlds.addAll(profile.getOnlineWorlds().stream().map(x -> new LMod(x, profile).setAction(this::onRemove)).toList());

        new Thread(() -> {
            Platform.runLater(() -> {
                cmOptifine.getItems().clear();
                cmSodium.getItems().clear();
            });

            if (profile.getWrapper() instanceof Forge){

                String ver = profile.getWrapperVersion();
                var vers = OptiFine.getOptiFine().getVersions(profile.getVersionId()).stream().sorted((x, y) -> Boolean.compare(x.checkForge(x.forgeWrapperVersion), y.checkForge(y.forgeWrapperVersion))).toList();
                for (var v : vers){
                    var item = new MenuItem();
                    item.setText(v.getWrapperVersion() + " - " + v.forgeWrapperVersion + (v.checkForge(ver) ? " *" : ""));
                    item.setOnAction(a -> {
                        OptiFine.getOptiFine().refreshUrl(v);
                        if (v.url == null)
                            return;
                        var mod = new Mod();
                        mod.fileName = v.getJsonName() + ".jar";
                        mod.fileUrl = v.url;
                        mod.name = v.getJsonName();
                        mod.logoUrl = "/com/cdev/corelauncher/images/optifine.png";
                        mod.classId = ClassType.MOD.getId();

                        Profiler.getProfiler().setProfile(profile.getName(), x -> x.getMods().add(mod));

                        //OptiFine.installForge(v, modsPath);
                    });
                    item.setStyle("-fx-text-fill: white;");
                    Platform.runLater(() -> cmOptifine.getItems().add(item));
                }
            }
            else if (profile.getWrapper() instanceof Fabric){
                var identifier = profile.getWrapper().getIdentifier();
                var vers = Modrinth.getModrinth().searchSodium(identifier, profile.getVersionId());
                for (var v : vers){
                    var item = new MenuItem();
                    item.setText(v.name);
                    item.setOnAction(a -> Profiler.getProfiler().setProfile(profile.getName(), x -> x.getMods().add(v)));
                    Platform.runLater(() -> cmSodium.getItems().add(item));
                }
            }
        }).start();
    }


    @FXML
    public void initialize() {
        lvMods.setCellFactory(x -> new CMod());
        lvModpacks.setCellFactory(x -> new CMod());
        lvResources.setCellFactory(x -> new CMod());
        lvWorlds.setCellFactory(x -> new CMod());

        lvMods.setItems(mods);
        lvModpacks.setItems(modpacks);
        lvResources.setItems(resources);
        lvWorlds.setItems(worlds);

        txtSearch.textProperty().addListener(x -> {
            String text = txtSearch.getText();
            if (text.isEmpty() || text.isBlank()){
                lvMods.setItems(mods);
                return;
            }

            var filtered = mods.filtered(a -> a.get().name.toLowerCase(Locale.getDefault()).contains(text.toLowerCase(Locale.getDefault())));
            lvMods.setItems(filtered);
        });

        btnBrowse.setOnAction(x -> {
            if (!NetUtils.check())
                return;
            ForgeBrowser.open(profile).show();
        });

        btnSodium.setContextMenu(cmSodium);
        btnSodium.setOnMouseClicked(x -> {
            if (!(profile.getWrapper() instanceof Fabric))
                return;

            cmSodium.show(btnSodium, x.getScreenX(), x.getScreenY());
        });
        btnOpti.setContextMenu(cmOptifine);
        btnOpti.setOnMouseClicked(x -> {
            if (!(profile.getWrapper() instanceof Forge))
                return;

            cmOptifine.show(btnOpti, x.getScreenX(), x.getScreenY());
        });

        btnUpdate.setOnMouseClicked(a -> {

            if (!NetUtils.check())
                return;

            var f = CMsgBox.msg(Alert.AlertType.WARNING, Translator.translate("ask.sure"), Translator.translate("mods.update.msg"))
                    .setButtons(ButtonType.YES, ButtonType.CANCEL)
                    .showAndWait();

            if (f.isEmpty() || f.get() != ButtonType.YES)
                return;

            var mods = profile.getMods().stream().filter(x -> x.mpId == 0).toList();
            String vId = profile.getVersionId();

            var all = CurseForge.getForge().getResources(vId, mods.stream().map(x -> x.id).toList(), profile, false).stream().map(x -> (Mod)x).toList();
            for (var m : mods){
                var mod = all.stream().filter(x -> x.id == m.id).findFirst();
                if (mod.isEmpty() || mod.get().fileName.equals(m.fileName))
                    continue;

                CurseForge.getForge().remove(profile, m);
                CurseForge.getForge().include(profile, mod.get());

            }
        });

    }

    public void onRemove(LMod mod){
        new Thread(() -> CurseForge.getForge().remove(profile, mod.get())).start();
    }
}
