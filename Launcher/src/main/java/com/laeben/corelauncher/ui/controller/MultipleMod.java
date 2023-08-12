package com.laeben.corelauncher.ui.controller;

import com.laeben.core.entity.Path;
import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.corelauncher.data.Translator;
import com.laeben.corelauncher.data.entities.Profile;
import com.laeben.corelauncher.data.nbt.entities.NBTList;
import com.laeben.corelauncher.minecraft.modding.Modder;
import com.laeben.corelauncher.minecraft.modding.curseforge.CurseForge;
import com.laeben.corelauncher.minecraft.modding.curseforge.entities.*;
import com.laeben.corelauncher.minecraft.modding.entities.CResource;
import com.laeben.corelauncher.minecraft.modding.entities.Mod;
import com.laeben.corelauncher.minecraft.modding.entities.ResourceType;
import com.laeben.corelauncher.minecraft.modding.entities.World;
import com.laeben.corelauncher.minecraft.modding.modrinth.Modrinth;
import com.laeben.corelauncher.minecraft.modding.modrinth.entities.*;
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
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    @FXML
    public RadioButton rbRinth;
    @FXML
    public RadioButton rbForge;

    private ScrollPane pane;
    private final ToggleGroup group;

    private final ObservableList<LMod> mods;
    public MultipleMod(){
        mods = FXCollections.observableArrayList();
        group = new ToggleGroup();
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

        rbRinth.setToggleGroup(group);
        rbForge.setToggleGroup(group);
        group.selectToggle(rbForge);

        btnFromWorld.setOnMouseClicked(a -> {

            if (!(profile.getWrapper() instanceof Forge)){
                CMsgBox.msg(Alert.AlertType.INFORMATION, Translator.translate("error.oops"), Translator.translate("import.multiple.forge")).show();
                return;
            }

            var chooser = new DirectoryChooser();
            chooser.setInitialDirectory(profile.getPath().to("saves").prepare().toFile());
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
            chooser.setInitialDirectory(profile.getPath().to("mods").prepare().toFile());
            var file = chooser.showDialog(btnFromWorld.getScene().getWindow());

            if (file == null)
                return;

            var path = Path.begin(file.toPath());
            var ps = path.getFiles();

            var files = ps.stream().filter(x -> x.getExtension() != null && x.getExtension().equals("jar")).map(x -> x.getNameWithoutExtension().split("-")[0]).toList();
            files.forEach(this::println);
        });

        btnSearch.setOnMouseClicked(a -> {
            var lines = txtQuery.getText().lines().filter(x -> !x.isBlank()).toList();

            new Thread(() -> {
                var secondary = new ArrayList<String>();

                String version = profile.getVersionId();
                String loader = profile.getWrapper().getIdentifier();

                var sf = new SearchForge();
                sf.gameVersion = version;
                sf.classId = ResourceType.MOD.getId();
                sf.sortField = ModsSearchSortField.POPULARITY;
                sf.setSortOrder(false);
                sf.modLoaderType = profile.getWrapper().getType();
                sf.pageSize = 1;

                var sr = new SearchRinth();
                sr.facets = new FacetBuilder()
                        .add(Facet.get("project_type", ResourceType.MOD.getName()))
                        .setGameVersion(version)
                        .setLoader(loader);
                sr.limit = 1;

                int i = 1;
                int size = lines.size();

                var toggle = group.getSelectedToggle();

                final var rinthPatt = Pattern.compile(".*(?<mngrp> ?modrinth ?).*");
                final var forgePatt = Pattern.compile(".*(?<mngrp> ?curseforge ?).*");

                for (var line : lines){

                    List<CResource> lMods = null;

                    sf.setSearchFilter(line);
                    sr.setQuery(line);

                    Matcher mtc;

                    if ((mtc = rinthPatt.matcher(line)).matches()){
                        sr.setQuery(line.replace(mtc.group(1), "").trim());
                        lMods = rinth(sr, version, loader);
                    }
                    else if ((mtc = forgePatt.matcher(line)).matches()){
                        sf.setSearchFilter(line.replace(mtc.group(1), "").trim());
                        lMods = forge(sf);
                    }
                    else if (toggle.equals(rbRinth))
                        lMods = rinth(sr, version, loader);
                    else if (toggle.equals(rbForge))
                        lMods = forge(sf);

                    if (lMods == null || lMods.isEmpty()){
                        secondary.add(line);
                        continue;
                    }

                    var mds = lMods.stream().toList();
                    String status = i++ + "/" + size;
                    Platform.runLater(() -> {
                        for (var mod : mds){
                            if (mods.stream().noneMatch(x -> x.get().name.equals(mod.name)))
                                mods.add(new LMod(mod, profile).setAction(mods::remove));
                        }
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
                    Modder.getModder().include(profile, mod);
                } catch (NoConnectionException | HttpException ignored) {

                }
            }

            Platform.runLater(() -> FXManager.getManager().closeStage(btnApply.getScene().getWindow()));
        }).start());

        lvMods.setCellFactory(x -> new CMod());
        lvMods.setItems(mods);
    }

    private List<CResource> rinth(SearchRinth sr, String version, String loader){
        SearchResponseRinth r = null;

        try{
            r = Modrinth.getModrinth().search(sr);
        } catch (NoConnectionException | HttpException ignored) {

        }

        if (r != null && !r.hits.isEmpty()) {
            var res = r.hits.get(0);
            List<CResource> v = null;
            try {
                var versions = Modrinth.getModrinth().getProjectVersions(res.getId(), version, loader, DependencyInfo.includeDependencies(version, loader));
                var resources = Modrinth.getModrinth().getResources(versions.stream().map(x -> x.projectId).toList());

                v = versions.stream().map(x -> (CResource)CResource.fromRinthResourceGeneric(resources.stream().filter(y -> y.id.equals(x.projectId)).findFirst().get(), x)).toList();
            } catch (NoConnectionException | HttpException ignored) {

            }

            return v;
        }
        else
            return null;
    }

    private List<CResource> forge(SearchForge sf) {
        SearchResponseForge f = null;
        ResourceForge full = null;
        try{
            f = CurseForge.getForge().search(sf);
        } catch (NoConnectionException | HttpException ignored) {

        }

        if (f == null || f.data.isEmpty())
            return null;



        try{
            full = CurseForge.getForge().getFullResource(profile.getVersionId(), profile.getWrapper().getType(), f.data.get(0));
        } catch (NoConnectionException | HttpException ignored) {

        }

        if (full == null)
            return null;

        var cres = Mod.fromForgeResource(profile.getVersionId(), profile.getWrapper().getIdentifier(), full);

        try {
            return CurseForge.getForge().getDependencies(List.of(cres), profile);
        } catch (NoConnectionException e) {
            return null;
        }
    }

    private void println(String text){
        String f = txtQuery.getText();
        if (f != null && !txtQuery.getText().isBlank())
            f += "\n\r";

        txtQuery.setText((f == null ? "" : f) + text);
    }
}
