package com.laeben.corelauncher.ui.controller.page;

import com.laeben.core.util.StrUtil;
import com.laeben.corelauncher.api.Profiler;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.minecraft.modding.curseforge.CurseForge;
import com.laeben.corelauncher.minecraft.modding.curseforge.entity.ModsSearchSortField;
import com.laeben.corelauncher.minecraft.modding.entity.LoaderType;
import com.laeben.corelauncher.minecraft.modding.entity.ResourcePreferences;
import com.laeben.corelauncher.minecraft.modding.entity.ResourceType;
import com.laeben.corelauncher.minecraft.modding.modrinth.Modrinth;
import com.laeben.corelauncher.minecraft.modding.modrinth.entity.*;
import com.laeben.corelauncher.minecraft.wrapper.Vanilla;
import com.laeben.corelauncher.minecraft.wrapper.optifine.OptiFine;
import com.laeben.corelauncher.minecraft.wrapper.optifine.entity.OptiVersion;
import com.laeben.corelauncher.ui.controller.HandlerController;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.ui.controller.browser.*;
import com.laeben.corelauncher.ui.control.*;
import com.laeben.corelauncher.ui.entity.filter.FilterPreset;
import com.laeben.corelauncher.ui.entity.filter.FilterSection;
import com.laeben.corelauncher.util.ImageUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

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
        ((CTab)parentObj).setText(Translator.translate("frame.title.browser") + (p != null ? " - " + StrUtil.sub(p.getName(), 0, 30) : ""));
    }

    /**
     * Sets the profile and behavior of the browser.
     * Full mode will be activated if the given profile is null.
     */
    public BrowserPage setProfile(Profile profile){
        this.profile = profile;

        if (profile != null){
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

            String defCat = !profile.getWrapper().getType().isNative() ? "mod" : "resource";

            filterPane.getPreset("modrinth").getSection("maincat").defaultChoice(defCat);
            filterPane.getPreset("curseforge").getSection("maincat").defaultChoice(defCat);
        }

        var pinned = filterPane.getPreset("pinned");
        var versionSect = pinned.getSection("version");
        var loaderSect = pinned.getSection("loader");
        versionSect.setVisible(profile == null);
        loaderSect.setVisible(profile == null);
        topBar.setVisible(profile != null);
        topBar.setManaged(profile != null);

        if (profile == null){
            versionSect.resetChoices();
            for (var v : Vanilla.getVanilla().getAllVersions()){
                if (v.type != null && !v.type.equals("release"))
                    continue;
                versionSect.addChoice(v.id, v.id);
            }

            loaderSect.resetChoices();
            for (var l : LoaderType.values()){
                if (!l.isSupported() || l.isNative())
                    continue;
                loaderSect.addChoice(l.getIdentifier(), l.getIdentifier());
            }
        }

        pinned.getSection("source").selectChoice("modrinth");
        search("");

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
    @FXML
    private HBox topBar;
    @FXML
    private Label lblNotFound;

    @Override
    public void preInit() {
        lvResources.setItems(resources);
        icon.setCornerRadius(30, 30, 8);

        filterPane.addPreset("pinned",
                FilterSection.create(Translator.translate("profile.edit.gameVersion"), "version", FilterSection.FilterType.MULTIPLE_COMBO),
                FilterSection.create(Translator.translate("mods.browse.loader"), "loader", FilterSection.FilterType.MULTIPLE_COMBO),
                FilterSection.create(Translator.translate("mods.browse.source"), "source", FilterSection.FilterType.SINGLE_RADIO)
                        .setOnAction(a -> {
                            if (a.choiceId().equals("modrinth")){
                                search = new ModrinthSearch(profile);
                                search.reset();
                                filterPane.setPreset("modrinth");
                                //filterPane.clearSections();
                            }
                            else if (a.choiceId().equals("curseforge")){
                                search = new CurseForgeSearch(profile);
                                search.reset();
                                filterPane.setPreset("curseforge");
                                //filterPane.clearSections();
                            }
                        })
                        .addChoice("Modrinth", "modrinth")
                        .addChoice("CurseForge", "curseforge")
        );

        filterPane.setPinnedPreset("pinned");

        filterPane.addPreset("modrinth",
                FilterSection.<ResourceType>create(Translator.translate("mods.browse.mainCategories"), "maincat", FilterSection.FilterType.SINGLE_RADIO)
                        .setOnAction(a -> setModrinthCategories(a.preset(), a.state()))
                        .addChoice(Translator.translate("mods.type.modpack"), "modpack", ResourceType.MODPACK)
                        .addChoice(Translator.translate("mods.type.mod"), "mod", ResourceType.MOD)
                        .addChoice(Translator.translate("mods.type.resourcepack"), "resource", ResourceType.RESOURCEPACK)
                        .addChoice(Translator.translate("mods.type.shader"), "shader", ResourceType.SHADER)
                        //.addChoice(Translator.translate("mods.type.world"), "world", ResourceType.WORLD),
                        .setSingleton(true)
                        .defaultChoice("mod"),
                FilterSection.create(Translator.translate("mods.browse.categories"), "cat", FilterSection.FilterType.MULTIPLE_CHECKBOX),
                FilterSection.<Index>create(Translator.translate("mods.browse.sortBy"), "sortby", FilterSection.FilterType.SINGLE_RADIO)
                        .setOnAction(a -> search.setSortField(a.state()))
                        .addChoice(Translator.translate("mods.sort.relevance"), "relevance", Index.RELEVANCE)
                        .addChoice(Translator.translate("mods.sort.newest"), "newest", Index.NEWEST)
                        .addChoice(Translator.translate("mods.sort.follows"), "follows", Index.FOLLOWS)
                        .addChoice(Translator.translate("mods.sort.lastUpdated"), "lastup", Index.UPDATED)
                        .addChoice(Translator.translate("mods.sort.totalDownload"), "totaldown", Index.DOWNLOADS)
                        .defaultChoice("relevance"),
                FilterSection.<Boolean>create(Translator.translate("mods.browse.sortmode"), "smode", FilterSection.FilterType.SINGLE_RADIO)
                        .setOnAction(a -> search.setSortOrder(a.state()))
                        .addChoice(Translator.translate("ascending"), "asc", true)
                        .addChoice(Translator.translate("descending"), "desc", false)
                        //.defaultChoice("desc")
        );

        filterPane.addPreset("curseforge",
                FilterSection.<ResourceType>create(Translator.translate("mods.browse.mainCategories"), "maincat", FilterSection.FilterType.SINGLE_RADIO)
                        .setOnAction(a -> {
                            if (a.choiceId().equals("optifine"))
                                setOptiFine(a.preset());
                            else
                                setCurseForgeCategories(a.preset(), a.state());
                        })
                        .addChoice(Translator.translate("mods.type.modpack"), "modpack", ResourceType.MODPACK)
                        .addChoice(Translator.translate("mods.type.mod"), "mod", ResourceType.MOD)
                        .addChoice(Translator.translate("mods.type.resourcepack"), "resource", ResourceType.RESOURCEPACK)
                        .addChoice("OptiFine", "optifine", null)
                        .addChoice(Translator.translate("mods.type.shader"), "shader", ResourceType.SHADER)
                        .addChoice(Translator.translate("mods.type.world"), "world", ResourceType.WORLD)
                        .setSingleton(true)
                        .defaultChoice("mod"),
                FilterSection.create(Translator.translate("mods.browse.categories"), "cat", FilterSection.FilterType.MULTIPLE_CHECKBOX),
                FilterSection.<ModsSearchSortField>create(Translator.translate("mods.browse.sortBy"), "sortby", FilterSection.FilterType.SINGLE_RADIO)
                        .setOnAction(a -> search.setSortField(a.state()))
                        .addChoice(Translator.translate("mods.sort.author"), "author", ModsSearchSortField.AUTHOR)
                        .addChoice(Translator.translate("mods.sort.category"), "category", ModsSearchSortField.CATEGORY)
                        .addChoice(Translator.translate("mods.sort.name"), "name", ModsSearchSortField.NAME)
                        .addChoice(Translator.translate("mods.sort.featured"), "featured", ModsSearchSortField.FEATURED)
                        .addChoice(Translator.translate("mods.sort.gameVersion"), "gamever", ModsSearchSortField.GAME_VERSION)
                        .addChoice(Translator.translate("mods.sort.lastUpdated"), "lastup", ModsSearchSortField.LAST_UPDATED)
                        .addChoice(Translator.translate("mods.sort.popularity"), "popularity", ModsSearchSortField.POPULARITY)
                        .addChoice(Translator.translate("mods.sort.totalDownload"), "totaldown", ModsSearchSortField.TOTAL_DOWNLOADS)
                        .defaultChoice("featured"),
                FilterSection.<Boolean>create(Translator.translate("mods.browse.sortmode"), "smode", FilterSection.FilterType.SINGLE_RADIO)
                        .setOnAction(a -> search.setSortOrder(a.state()))
                        .addChoice(Translator.translate("ascending"), "asc", true)
                        .addChoice(Translator.translate("descending"), "desc", false)
                        .defaultChoice("desc")
        );

        lvResources.setCellFactory(a -> new ResourceCell().setOnNewProfileCreated(this::onProfileCreated).bindWidth(lvResources.widthProperty().subtract(10)));

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

    private void onProfileCreated(Profile profile) {
        Main.getMain().replaceTab(this, "pages/browser", Translator.translate("frame.title.browser"), true, BrowserPage.class).setProfile(profile);
    }

    private void search(String query){
        var preset = filterPane.getPreset(filterPane.getLoadedPreset());
        List<String> vers = profile != null ? null : filterPane.getPreset("pinned").getSection("version").getSelectedChoices();

        resources.clear();

        var maincat = preset.getSection("maincat");
        if (maincat.getSelectedChoice() != null && maincat.getSelectedChoice().equals("optifine")) {
            if (profile != null && profile.getWrapper().getType().isNative())
                return;

            Stream<OptiVersion> versions;
            ResourcePreferences preferences;

            if (profile != null){
                preferences = ResourcePreferences.fromProfile(profile);
                versions = OptiFine.getOptiFine().getVersions(profile.getVersionId()).stream();
            }
            else{
                versions = OptiFine.getOptiFine().getAllVersions().stream();
                if (vers != null)
                    versions = versions.filter(x -> vers.stream().anyMatch(x::checkId));

                preferences = ResourcePreferences.empty()
                        .includeGameVersions(vers)
                        .includeLoaders(List.of(LoaderType.OPTIFINE));
            }

            resources.addAll(versions.sorted((x, y) -> Boolean.compare(x.checkForge(x.forgeWrapperVersion), y.checkForge(y.forgeWrapperVersion))).map(a -> new ResourceCell.Link(preferences, ResourceOpti.fromOptiVersion(a.id, a))).toList());

            return;
        }
        List<String> selectedCats = preset.getSection("cat").getSelectedChoices();

        search.setCategories(selectedCats);

        search.setGameVersions(vers);

        if (profile == null){
            FilterSection<LoaderType> sect = filterPane.getPreset("pinned").getSection("loader");
            List<String> selected = sect.getSelectedChoices();

            if (!selected.isEmpty()){
                var loaders = new ArrayList<LoaderType>();
                for (var s : selected){
                    LoaderType loader = LoaderType.TYPES.get(s);
                    if (loader == null)
                        continue;
                    loaders.add(loader);
                }

                search.setLoaders(loaders);
            }
        }
        else
            search.setLoaders(null);

        resources.addAll(search.search(query));

        lblNotFound.setVisible(resources.isEmpty());

        paginator.setTotalPages(search.getTotalPages());
    }

    private void setModrinthCategories(FilterPreset a, ResourceType type){
        mainType = type;
        search.setMainType(type);

        var categories = Modrinth.getModrinth().getCategories(mainType.getName());
        var section = a.getSection("cat");
        section.resetChoices();
        for (var c : categories)
            section.addChoice(StrUtil.toUpperFirst(c.name), c.name);
    }

    private void setCurseForgeCategories(FilterPreset a, ResourceType type){
        mainType = type;
        search.setMainType(type);

        var categories = CurseForge.getForge().getCategories().stream().filter(x -> x.classId == mainType.getId()).toList();
        var section = a.getSection("cat");
        section.resetChoices();
        for (var c : categories)
            section.addChoice(StrUtil.toUpperFirst(c.name), "" + c.id);
    }

    private void setOptiFine(FilterPreset a){
        a.getSection("cat").resetChoices();
    }


    @Override
    public void dispose(){
        paginator.setOnPageChange(null);
        filterPane.dispose();
        super.dispose();
    }
}
