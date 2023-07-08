package com.laeben.corelauncher.ui.controller;

import com.laeben.corelauncher.data.Profiler;
import com.laeben.corelauncher.data.Translator;
import com.laeben.corelauncher.data.entities.Profile;
import com.laeben.corelauncher.minecraft.modding.curseforge.CurseForge;
import com.laeben.corelauncher.minecraft.modding.curseforge.entities.*;
import com.laeben.corelauncher.ui.controls.BMod;
import com.laeben.corelauncher.ui.controls.CButton;
import com.laeben.corelauncher.ui.entities.LModLink;
import com.laeben.corelauncher.ui.entities.LStage;
import com.laeben.corelauncher.ui.utils.FXManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ForgeBrowser {
    private Profile profile;

    private static final Map<ModsSearchSortField, String> sTranslates = new HashMap<>(){{
       put(ModsSearchSortField.AUTHOR, "mods.sort.author");
       put(ModsSearchSortField.CATEGORY, "mods.sort.category");
       put(ModsSearchSortField.NAME, "mods.sort.name");
       put(ModsSearchSortField.FEATURED, "mods.sort.featured");
       put(ModsSearchSortField.GAME_VERSION, "mods.sort.gameVersion");
       put(ModsSearchSortField.LAST_UPDATED, "mods.sort.lastUpdated");
       put(ModsSearchSortField.POPULARITY, "mods.sort.popularity");
       put(ModsSearchSortField.TOTAL_DOWNLOADS, "mods.sort.totalDownload");
    }};

    public static LStage open(Profile p){
        var mds = FXManager.getManager().applyStage("forgebrowser");
        var ths = (ForgeBrowser)mds.getLScene().getController();
        ths.profile = p;
        ths.reset();

        return mds;
    }

    @FXML
    public ComboBox<String> cbMainCategories;
    @FXML
    public ComboBox<String> cbCategories;
    @FXML
    public ComboBox<String> cbSortBy;
    @FXML
    public ComboBox<String> cbSort;
    @FXML
    public TextField txtQuery;
    @FXML
    public CButton btnSearch;
    @FXML
    public ListView<LModLink> lvMods;

    private final ObservableList<LModLink> resources;
    private List<Category> categories;

    private Search search;
    private SearchResponse.Pagination pagination;


    public ForgeBrowser(){
        resources = FXCollections.observableArrayList();

        Profiler.getProfiler().getHandler().addHandler("fbrowser", a -> {
            if (!a.getKey().equals("profileUpdate"))
                return;

            search();
        });
    }

    @FXML
    public void initialize(){
        lvMods.setCellFactory(x -> new BMod());
        lvMods.setItems(resources);

        btnSearch.setOnMouseClicked(a -> {
            search();
        });

        txtQuery.setOnKeyPressed(a -> {
            if (a.getCode() != KeyCode.ENTER)
                return;

            search();
        });

        cbMainCategories.getItems().clear();
        cbMainCategories.getItems().addAll(
                Translator.translate("mods.type.modpack"),
                Translator.translate("mods.type.mod"),
                Translator.translate("mods.type.resource"),
                Translator.translate("mods.type.world"));

        cbMainCategories.valueProperty().addListener(a -> {

            var value = cbMainCategories.getValue();

            if (value == null || value.isEmpty())
                return;

            cbCategories.getItems().clear();
            //categories.clear();

            int index = cbMainCategories.getItems().indexOf(value);
            var type = switch (index){
                case 0 -> ClassType.MODPACK;
                case 2 -> ClassType.RESOURCE;
                case 3 -> ClassType.WORLD;
                default -> ClassType.MOD;
            };

            if (type == ClassType.RESOURCE || type == ClassType.WORLD){
                if (profile.getWrapper().getType() == CurseWrapper.Type.ANY)
                    search.modLoaderType = null;
                else
                    search.modLoaderType = profile.getWrapper().getType();
            }
            else
                search.modLoaderType = profile.getWrapper().getType();

            categories = CurseForge.getForge().getCategories().stream().filter(x -> x.classId == type.getId()).toList();
            cbCategories.getItems().add("...");
            cbCategories.getItems().addAll(categories.stream().map(x -> x.name).toList());


            search.classId = type.getId();
        });

        cbCategories.valueProperty().addListener(a -> {
            var value = cbCategories.getValue();

            if (value == null || value.isEmpty())
                return;

            var category = categories.stream().filter(x -> x.name.equals(value)).findFirst();
            search.categoryId = category.map(category1 -> category1.id).orElse(-1);
        });

        var sorts = ModsSearchSortField.class.getEnumConstants();

        cbSortBy.getItems().clear();
        var fx = Arrays.stream(sorts).filter(x -> x != ModsSearchSortField.NONE).toList();
        cbSortBy.getItems().addAll(fx.stream().map(x -> Translator.translate(sTranslates.get(x))).toList());
        cbSortBy.valueProperty().addListener(a -> {
            var value = cbSortBy.getValue();

            if (value == null || value.isEmpty())
                return;

            int index = cbSortBy.getItems().indexOf(value);

            search.sortField = fx.get(index);
        });

        cbSort.getItems().clear();
        cbSort.getItems().addAll(Translator.translate("ascending"), Translator.translate("descending"));
        cbSort.valueProperty().addListener(a -> {
            var value = cbSort.getValue();

            if (value == null || value.isEmpty())
                return;

            int index = cbSort.getItems().indexOf(value);

            search.sortOrder = index == 0 ? "asc" : "desc";
        });

        txtQuery.textProperty().addListener(a -> search.searchFilter = txtQuery.getText());
    }

    public void reset(){
        search = new Search();
        search.classId = ClassType.MOD.getId();
        search.gameVersion = profile.getVersionId();
        search.modLoaderType = profile.getWrapper().getType();
        search.sortField = ModsSearchSortField.POPULARITY;
        search.sortOrder = "desc";

        search();
    }

    public void search(){
        resources.clear();
        var sData = CurseForge.getForge().search(search);

        if (sData == null)
            return;

        resources.addAll(sData.data.stream().map(x -> new LModLink(profile, x)).toList());
        pagination = sData.pagination;
    }
}
