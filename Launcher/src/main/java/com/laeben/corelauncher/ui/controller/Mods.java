package com.laeben.corelauncher.ui.controller;

import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.corelauncher.data.Profiler;
import com.laeben.corelauncher.data.Translator;
import com.laeben.corelauncher.data.entities.Profile;
import com.laeben.corelauncher.minecraft.modding.Modder;
import com.laeben.corelauncher.minecraft.modding.curseforge.CurseForge;
import com.laeben.corelauncher.minecraft.modding.entities.CResource;
import com.laeben.corelauncher.minecraft.modding.entities.Mod;
import com.laeben.corelauncher.minecraft.modding.entities.Modpack;
import com.laeben.corelauncher.minecraft.modding.modrinth.Modrinth;
import com.laeben.corelauncher.minecraft.wrappers.forge.Forge;
import com.laeben.corelauncher.minecraft.wrappers.optifine.OptiFine;
import com.laeben.corelauncher.ui.controller.browser.ForgeBrowser;
import com.laeben.corelauncher.ui.controller.browser.ModBrowser;
import com.laeben.corelauncher.ui.controller.browser.ModrinthBrowser;
import com.laeben.corelauncher.ui.controls.CMod;
import com.laeben.corelauncher.ui.controls.CMsgBox;
import com.laeben.corelauncher.ui.entities.LMod;
import com.laeben.corelauncher.ui.entities.LStage;
import com.laeben.corelauncher.ui.utils.FXManager;
import com.laeben.corelauncher.utils.NetUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

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
    public Button btnBrowseForge;
    @FXML
    public Button btnBrowseRinth;
    @FXML
    public Button btnOpti;
    @FXML
    public Button btnUpdate;
    @FXML
    public Button btnCustom;

    private final ObservableList<LMod> mods;
    private final ObservableList<LMod> resources;
    private final ObservableList<LMod> modpacks;
    private final ObservableList<LMod> worlds;

    private final ContextMenu cmOptifine;

    public Mods(){
        mods = FXCollections.observableArrayList();
        modpacks = FXCollections.observableArrayList();
        resources = FXCollections.observableArrayList();
        worlds = FXCollections.observableArrayList();

        Profiler.getProfiler().getHandler().addHandler("mods", a -> {
            if (a.getKey().equals("profileUpdate")){
                reload();
            }
        }, false);

        String cStyle = "-fx-background-color: #252525;";
        cmOptifine = new ContextMenu();
        cmOptifine.setStyle(cStyle);
    }

    public void reload(){
        Platform.runLater(() -> {
            mods.clear();
            modpacks.clear();
            worlds.clear();
            resources.clear();
            txtSearch.clear();
        });

        if (profile == null)
            return;
        new Thread(() -> {

            Platform.runLater(() -> {
                mods.addAll(profile.getMods().stream().map(x -> new LMod(x, profile).setAction(this::onRemove)).toList());
                modpacks.addAll(profile.getModpacks().stream().map(x -> new LMod(x, profile).setAction(this::onRemove)).toList());
                resources.addAll(Stream.concat(profile.getResources().stream().map(x -> new LMod(x, profile).setAction(this::onRemove)), profile.getShaders().stream().map(x -> new LMod(x, profile).setAction(this::onRemove))).toList());
                worlds.addAll(profile.getOnlineWorlds().stream().map(x -> new LMod(x, profile).setAction(this::onRemove)).toList());

                cmOptifine.getItems().clear();
            });

            if (profile.getWrapper() instanceof Forge){
                String ver = profile.getWrapperVersion();
                var vers = OptiFine.getOptiFine().getVersions(profile.getVersionId()).stream().sorted((x, y) -> Boolean.compare(x.checkForge(x.forgeWrapperVersion), y.checkForge(y.forgeWrapperVersion))).toList();
                for (var v : vers){
                    var item = new MenuItem();
                    item.setText(v.getWrapperVersion() + " - " + v.forgeWrapperVersion + (v.checkForge(ver) ? " *" : ""));
                    item.setOnAction(a -> {
                        var mod = new Mod();
                        mod.fileName = v.getJsonName() + ".jar";
                        mod.fileUrl = v.getWrapperVersion();
                        mod.name = v.getJsonName();
                        mod.logoUrl = "/com/laeben/corelauncher/images/optifine.png";

                        Profiler.getProfiler().setProfile(profile.getName(), x -> x.getMods().add(mod));
                    });
                    item.setStyle("-fx-text-fill: white;");
                    Platform.runLater(() -> cmOptifine.getItems().add(item));
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
            if (text.isBlank()){
                lvMods.setItems(mods);
                lvModpacks.setItems(modpacks);
                lvResources.setItems(resources);
                lvWorlds.setItems(worlds);
                return;
            }

            var fMods = mods.filtered(a -> a.get().name.toLowerCase(Locale.getDefault()).contains(text.toLowerCase(Locale.getDefault())));
            lvMods.setItems(fMods);

            var fModpacks = modpacks.filtered(a -> a.get().name.toLowerCase(Locale.getDefault()).contains(text.toLowerCase(Locale.getDefault())));
            lvModpacks.setItems(fModpacks);

            var fRes = resources.filtered(a -> a.get().name.toLowerCase(Locale.getDefault()).contains(text.toLowerCase(Locale.getDefault())));
            lvResources.setItems(fRes);

            var fWorlds = worlds.filtered(a -> a.get().name.toLowerCase(Locale.getDefault()).contains(text.toLowerCase(Locale.getDefault())));
            lvWorlds.setItems(fWorlds);
        });

        btnBrowseForge.setOnAction(x -> {
            if (!NetUtils.check())
                return;
            ModBrowser.open(profile, new ForgeBrowser(), "forgebrowser").show();
        });

        btnCustom.setOnMouseClicked(a -> ImportMod.open(profile).show());

        btnBrowseRinth.setOnMouseClicked(x -> {
            if (!NetUtils.check())
                return;

            ModBrowser.open(profile, new ModrinthBrowser(), "rinthbrowser").show();
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

            var mods = Stream.concat(profile.getMods().stream().filter(x -> x.getModpackId() == null), profile.getModpacks().stream()).toList();

            List<CResource> allMods = getLastVersions(mods);

            if (!allMods.isEmpty()){
                for (var m : mods){
                    var mod = allMods.stream().filter(m::equals).findFirst();
                    if (mod.isEmpty() || mod.get().fileName.equals(m.fileName))
                        continue;

                    Modder.getModder().remove(profile, m);
                    try {
                        Modder.getModder().include(profile, mod.get());
                    } catch (NoConnectionException | HttpException ignored) {

                    }
                }

            }
        });

    }

    private <T extends CResource> List<T> getLastVersions(List<T> olds){
        List<T> all = new ArrayList<>();
        var vId = profile.getVersionId();
        try {
            var rinth = olds.stream().filter(T::isModrinth).map(x -> x.id.toString()).toList();
            var forge = olds.stream().filter(T::isForge).map(x -> (int)x.id).toList();
            if (!rinth.isEmpty())
                all.addAll(Modrinth.getModrinth().getCResources(rinth, vId, profile.getWrapper().getIdentifier()));
            if (!forge.isEmpty())
                all.addAll(CurseForge.getForge().getResources(vId, forge, profile, false).stream().map(x -> (T)x).toList());
        } catch (NoConnectionException | HttpException ignored) {

        }

        return all;
    }

    public void onRemove(LMod mod){
        new Thread(() -> Modder.getModder().remove(profile, mod.get())).start();
    }
}
