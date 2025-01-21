package com.laeben.corelauncher.ui.controller.page;

import com.laeben.core.util.StrUtil;
import com.laeben.corelauncher.api.Profiler;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.minecraft.modding.curseforge.CurseForge;
import com.laeben.corelauncher.minecraft.modding.curseforge.entity.ModsSearchSortField;
import com.laeben.corelauncher.minecraft.modding.entity.ResourceType;
import com.laeben.corelauncher.minecraft.modding.modrinth.Modrinth;
import com.laeben.corelauncher.minecraft.modding.modrinth.entity.*;
import com.laeben.corelauncher.minecraft.wrapper.optifine.OptiFine;
import com.laeben.corelauncher.ui.controller.HandlerController;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.ui.controller.browser.*;
import com.laeben.corelauncher.ui.control.*;
import com.laeben.corelauncher.ui.entity.FilterPreset;
import com.laeben.corelauncher.ui.entity.FilterSection;
import com.laeben.corelauncher.util.ImageUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.List;

public class BrowserPage extends HandlerController {
    public static final String KEY = "pgbrowser";

    private Search search;

    private Profile profile;
    private String wrVersion;
    private String version;

    private ResourceType mainType;

    private final ObservableList<ResourceCell.Link> resources;

    public BrowserPage(){
        super(KEY);
        resources = FXCollections.observableArrayList();
    }

    private void reloadTitle(Profile p){
        ((CTab)parentObj).setText(Translator.translate("frame.title.browser") + " - " + StrUtil.sub(p.getName(), 0, 30));
    }

    public BrowserPage setProfile(Profile profile){
        this.profile = profile;

        registerHandler(Profiler.getProfiler().getHandler(), a -> {
            if (a.getKey().equals(Profiler.PROFILE_UPDATE)){
                icon.setImageAsync(ImageUtil.getImageFromProfile(profile, 32, 32));
                lblProfileName.setText(profile.getName());
                reloadTitle(profile);
                lblInfo.setText(profile.getWrapper().getType().getIdentifier() + " - " + profile.getWrapperVersion());
                if (!profile.getWrapperVersion().equals(wrVersion) || !version.equals(profile.getVersionId())){
                    wrVersion = profile.getWrapperVersion();
                    version = profile.getVersionId();
                    search.reset();
                    filterPane.clearSections();
                    resources.clear();
                }
            }
        }, true);

        wrVersion = profile.getWrapperVersion();
        version = profile.getVersionId();

        icon.setImageAsync(ImageUtil.getImageFromProfile(profile, 32, 32));
        lblProfileName.setText(profile.getName());
        reloadTitle(profile);
        lblInfo.setText(profile.getWrapper().getType().getIdentifier() + " - " + profile.getWrapperVersion());

        filterPane.getPreset("pinned").sections().get(0).selectChoice("modrinth");

        return this;
    }

    @FXML
    private CFilterPane filterPane;
    @FXML
    private CButton btnSearch;
    @FXML
    private CField txtQuery;
    @FXML
    private ListView<ResourceCell.Link> lvResources;
    @FXML
    private Label lblProfileName;
    @FXML
    private CButton btnBack;
    @FXML
    private Label lblInfo;
    @FXML
    private CView icon;
    @FXML
    private CPaginator paginator;

    @Override
    public void preInit() {
        lvResources.setItems(resources);
        icon.setCornerRadius(30, 30, 8);

        filterPane.addPreset("pinned",
                FilterSection.create(Translator.translate("mods.browse.source"), "source", FilterSection.Type.SINGLE)
                        .addChoice("Modrinth", "modrinth", a -> {
                            filterPane.setPreset("modrinth");
                            filterPane.clearSections();
                            search = new ModrinthSearch(profile);
                            search.reset();
                        })
                        .addChoice("CurseForge", "curseforge", a -> {
                            filterPane.setPreset("curseforge");
                            filterPane.clearSections();
                            search = new ForgeSearch(profile);
                            search.reset();
                        })
        );

        filterPane.setPinnedPreset("pinned");

        filterPane.addPreset("modrinth",
                FilterSection.create(Translator.translate("mods.browse.mainCategories"), "maincat", FilterSection.Type.SINGLE)
                        .addChoice(Translator.translate("mods.type.modpack"), "modpack", a -> setModrinthCategories(a.preset(), ResourceType.MODPACK))
                        .addChoice(Translator.translate("mods.type.mod"), "mod", a -> setModrinthCategories(a.preset(), ResourceType.MOD))
                        .addChoice(Translator.translate("mods.type.resourcepack"), "resource", a -> setModrinthCategories(a.preset(), ResourceType.RESOURCE))
                        .addChoice(Translator.translate("mods.type.shader"), "shader", a -> setModrinthCategories(a.preset(), ResourceType.SHADER)),
                        //.addChoice(Translator.translate("mods.type.world"), "world", a -> setModrinthCategories(a.preset(), ResourceType.WORLD)),
                FilterSection.create(Translator.translate("mods.browse.categories"), "cat", FilterSection.Type.MULTIPLE),
                FilterSection.create(Translator.translate("mods.browse.sortBy"), "sortby", FilterSection.Type.SINGLE)
                        .addChoice(Translator.translate("mods.sort.relevance"), "relevance", a -> search.setSortField(Index.RELEVANCE))
                        .addChoice(Translator.translate("mods.sort.newest"), "newest", a -> search.setSortField(Index.NEWEST))
                        .addChoice(Translator.translate("mods.sort.follows"), "follows", a -> search.setSortField(Index.FOLLOWS))
                        .addChoice(Translator.translate("mods.sort.lastUpdated"), "lastup", a -> search.setSortField(Index.UPDATED))
                        .addChoice(Translator.translate("mods.sort.totalDownload"), "totaldown", a -> search.setSortField(Index.DOWNLOADS)),
                FilterSection.create(Translator.translate("mods.browse.sortmode"), "smode", FilterSection.Type.SINGLE)
                        .addChoice(Translator.translate("ascending"), "asc", a -> search.setSortOrder(true))
                        .addChoice(Translator.translate("descending"), "desc", a -> search.setSortOrder(false))
        );

        filterPane.addPreset("curseforge",
                FilterSection.create(Translator.translate("mods.browse.mainCategories"), "maincat", FilterSection.Type.SINGLE)
                        .addChoice(Translator.translate("mods.type.modpack"), "modpack", a -> setCurseForgeCategories(a.preset(), ResourceType.MODPACK))
                        .addChoice(Translator.translate("mods.type.mod"), "mod", a -> setCurseForgeCategories(a.preset(), ResourceType.MOD))
                        .addChoice(Translator.translate("mods.type.resourcepack"), "resource", a -> setCurseForgeCategories(a.preset(), ResourceType.RESOURCE))
                        .addChoice("OptiFine", "optifine", a -> setOptiFine(a.preset()))
                        .addChoice(Translator.translate("mods.type.shader"), "shader", a -> setCurseForgeCategories(a.preset(), ResourceType.SHADER))
                        .addChoice(Translator.translate("mods.type.world"), "world", a -> setCurseForgeCategories(a.preset(), ResourceType.WORLD)),
                FilterSection.create(Translator.translate("mods.browse.categories"), "cat", FilterSection.Type.MULTIPLE),
                FilterSection.create(Translator.translate("mods.browse.sortBy"), "sortby", FilterSection.Type.SINGLE)
                        .addChoice(Translator.translate("mods.sort.author"), "author", a -> search.setSortField(ModsSearchSortField.AUTHOR))
                        .addChoice(Translator.translate("mods.sort.category"), "category", a -> search.setSortField(ModsSearchSortField.CATEGORY))
                        .addChoice(Translator.translate("mods.sort.name"), "name", a -> search.setSortField(ModsSearchSortField.NAME))
                        .addChoice(Translator.translate("mods.sort.featured"), "featured", a -> search.setSortField(ModsSearchSortField.FEATURED))
                        .addChoice(Translator.translate("mods.sort.gameVersion"), "gamever", a -> search.setSortField(ModsSearchSortField.GAME_VERSION))
                        .addChoice(Translator.translate("mods.sort.lastUpdated"), "lastup", a -> search.setSortField(ModsSearchSortField.LAST_UPDATED))
                        .addChoice(Translator.translate("mods.sort.popularity"), "popularity", a -> search.setSortField(ModsSearchSortField.POPULARITY))
                        .addChoice(Translator.translate("mods.sort.totalDownload"), "totaldown", a -> search.setSortField(ModsSearchSortField.TOTAL_DOWNLOADS)),
                FilterSection.create(Translator.translate("mods.browse.sortmode"), "smode", FilterSection.Type.SINGLE)
                        .addChoice(Translator.translate("ascending"), "asc", a -> search.setSortOrder(true))
                        .addChoice(Translator.translate("descending"), "desc", a -> search.setSortOrder(false))
        );

        lvResources.setCellFactory(a -> new ResourceCell().bindWidth(lvResources.widthProperty().subtract(10)));

        txtQuery.setOnKeyPressed(a -> {
            if (a.getCode() != KeyCode.ENTER)
                return;

            search(txtQuery.getText());
        });
        txtQuery.setFocusedAnimation(Color.web("teal"), Duration.millis(200));

        btnSearch.enableTransparentAnimation();
        btnSearch.setOnMouseClicked(a -> search(txtQuery.getText()));

        btnBack.enableTransparentAnimation();
        btnBack.setOnMouseClicked(a -> Main.getMain().replaceTab(this, "pages/profile", profile.getName(), true, ProfilePage.class).setProfile(profile));
        btnBack.setText("⤶ " + Translator.translate("option.back"));

        paginator.setOnPageChange(a -> {
            search.setPageIndex(paginator.getPage());
            search(txtQuery.getText());
        });
    }

    private void search(String query){
        var preset = filterPane.getPreset(filterPane.getLoadedPreset());

        resources.clear();

        var maincat = preset.getSection("maincat");
        if (maincat.getSelectedChoice() != null && maincat.getSelectedChoice().equals("optifine")) {
            if (!profile.getWrapper().getType().isNative())
                resources.addAll(OptiFine.getOptiFine().getVersions(profile.getVersionId()).stream().sorted((x, y) -> Boolean.compare(x.checkForge(x.forgeWrapperVersion), y.checkForge(y.forgeWrapperVersion))).map(a -> new ResourceCell.Link(profile, ResourceOpti.fromOptiVersion(profile, a))).toList());
            return;
        }
        var selectedCats = ((List)preset.getSection("cat").getSelectedChoices());

        search.setCategories(selectedCats);
        resources.addAll(search.search(query));

        paginator.setTotalPages(search.getTotalPages());
    }

    private void setModrinthCategories(FilterPreset a, ResourceType type){
        mainType = type;
        search.setMainType(type);

        var categories = Modrinth.getModrinth().getCategories(mainType.getName());
        var section = a.getSection("cat");
        section.resetChoices();
        for (var c : categories)
            section.addChoice(StrUtil.toUpperFirst(c.name), c.name, null);
    }

    private void setCurseForgeCategories(FilterPreset a, ResourceType type){
        mainType = type;
        search.setMainType(type);

        var categories = CurseForge.getForge().getCategories().stream().filter(x -> x.classId == mainType.getId()).toList();
        var section = a.getSection("cat");
        section.resetChoices();
        for (var c : categories)
            section.addChoice(StrUtil.toUpperFirst(c.name), "" + c.id, null);
    }

    private void setOptiFine(FilterPreset a){
        a.getSection("cat").resetChoices();
    }
}
