package com.laeben.corelauncher.ui.controller.page;

import com.laeben.core.entity.Path;
import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.corelauncher.api.ui.UI;
import com.laeben.corelauncher.api.ui.entity.Announcement;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.Profiler;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.minecraft.modding.Modder;
import com.laeben.corelauncher.minecraft.modding.entity.ResourceType;
import com.laeben.corelauncher.minecraft.modding.entity.resource.CResource;
import com.laeben.corelauncher.minecraft.modding.entity.resource.Modpack;
import com.laeben.corelauncher.ui.controller.HandlerController;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.ui.controller.cell.CDockObject;
import com.laeben.corelauncher.ui.controller.cell.CPRCell;
import com.laeben.corelauncher.ui.control.*;
import com.laeben.corelauncher.ui.controller.cell.CProfile;
import com.laeben.corelauncher.ui.dialog.DProfileSelector;
import com.laeben.corelauncher.ui.dialog.DResourceSelector;
import com.laeben.corelauncher.ui.entity.EventFilter;
import com.laeben.corelauncher.util.ImageCacheManager;
import com.laeben.corelauncher.util.ImageUtil;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.*;
import java.util.function.Predicate;

public class ProfilePage extends HandlerController {
    public static final String KEY = "pgprofile";

    public static final String COPYSET = "copyset";
    public static final String UPDATE_ALL = "upall";
    public static final String SELECT_ALL = "selall";
    public static final String DESELECT_ALL = "deselall";

    private class ResourceSelectionModeListener implements ChangeListener<Boolean> {
        private final CList<?> list;
        public ResourceSelectionModeListener(CList<?> list){
            this.list = list;
        }


        @Override
        public void changed(ObservableValue<? extends Boolean> observableValue, Boolean o, Boolean n) {
            if (n != null && n)
                lastActiveList = list;
        }
    }

    public GridPane grid;

    private Profile profile;

    private boolean ignoreReload = false;

    private final DProfileSelector selector;

    public ProfilePage(){
        super(KEY);
        lvMods = new CList<>();
        lvModpacks = new CList<>();
        lvResources = new CList<>();
        lvShaders = new CList<>();
        lvWorlds = new CList<>();

        selector = new DProfileSelector(DProfileSelector.Functionality.SINGLE_PROFILE_SELECTOR);

        registerHandler(Profiler.getProfiler().getHandler(), a -> {
            var oldp = (Profile)a.getOldValue();

            if (oldp == null || !profile.getName().equals(oldp.getName()))
                return;

            if (a.getKey().equals(Profiler.PROFILE_DELETE))
                Main.getMain().closeTab((Tab) this.parentObj);
            else if (a.getKey().equals(Profiler.PROFILE_UPDATE)){ // experimental
                if (!ignoreReload)
                    setProfile((Profile)a.getNewValue());
                else
                    ignoreReload = false;
            }
        }, true);

        registerHandler(Configurator.getConfigurator().getHandler(), a -> {
            if (a.getKey().equals(Configurator.USER_CHANGE)){
                setUser();
            }
        }, true);
    }

    @FXML
    private Label lblProfileName;
    @FXML
    private HBox badges;
    /*@FXML
    private CView imgUserHead;*/
    @FXML
    private CHeadView imgUserHead;
    @FXML
    private CView imgProfile;
    @FXML
    private ImageView bdgLoader;

    @FXML
    private VBox vMods;
    @FXML
    private VBox vModpacks;
    @FXML
    private VBox vWorlds;
    @FXML
    private VBox vResources;
    @FXML
    private VBox vShaders;

    @FXML
    private Label lblModpacks;
    @FXML
    private Label lblResources;
    @FXML
    private Label lblShaders;
    @FXML
    private Label lblWorlds;
    @FXML
    private Label lblMods;

    @FXML
    private CButton btnMenuResources;
    @FXML
    private CButton btnMenuShaders;
    @FXML
    private CButton btnMenuMods;

    private final CList<CResource> lvMods;
    private final CList<CResource> lvModpacks;
    private final CList<CResource> lvWorlds;
    private final CList<CResource> lvResources;
    private final CList<CResource> lvShaders;

    private CList<?> lastActiveList;

    @FXML
    public Label lblUsername;
    @FXML
    public Label lblGameVersion;
    @FXML
    public Pane boxDrag;
    @FXML
    public CField txtSearch;

    @FXML
    public CButton btnAddResource;
    @FXML
    private CButton btnMultipleBrowser;
    @FXML
    public CButton btnWorlds;
    @FXML
    private CMenu menu;

    private void setUser(){
        var user = profile.tryGetUser().reload();
        //imgUserHead.setImage(user.getHead());
        imgUserHead.setHead(user.getHead());
        imgUserHead.setDecoration(user.getHeadDecoration());
        lblUsername.setText(user.getUsername());
    }

    public void setProfile(Profile p){
        profile = p;

        lblProfileName.setText(p.getName());

        //badges.getChildren().clear();

        bdgLoader.setImage(p.getLoader().getIcon());
        var tip = new Tooltip(p.getLoader().getType().getIdentifier() + " - " + p.getLoaderVersion());
        tip.setStyle("-fx-font-size: 14pt");
        Tooltip.install(bdgLoader, tip);

        imgProfile.setImageAsync(ImageUtil.getImageFromProfile(profile, 192, 192));

        setUser();

        lblGameVersion.setText(p.getVersionId());

        lvMods.setLoadLimit(10);
        lvMods.getItems().setAll(p.getMods(false));
        lvMods.load();

        lvModpacks.getItems().setAll(p.getModpacks(false));
        lvModpacks.load();

        lvWorlds.getItems().setAll(p.getOnlineWorlds(false));
        lvWorlds.load();

        lvResources.getItems().setAll(p.getResourcepacks(false));
        lvResources.load();

        lvShaders.getItems().setAll(p.getShaders(false));
        lvShaders.load();

        var btnMenu = new CButton();
        btnMenu.getStyleClass().add("profile-button");
        btnMenu.setId("btnMenu");
        btnMenu.setOnMouseClicked(a -> menu.show());

        menu.clear();
        CProfile.generateProfileMenu(menu, profile, btnMenu, a -> {
            if (a.equals(CProfile.EDIT)){
                Main.getMain().replaceTab(this, "pages/profileedit", "", true, EditProfilePage.class).setProfile(profile);
                return false;
            }
            else if (a.equals(CProfile.DELETE)){
                var x = CMsgBox.msg(Alert.AlertType.CONFIRMATION, Translator.translate("ask.ask"), Translator.translate("ask.sure"))
                        .setButtons(CMsgBox.ResultType.YES, CMsgBox.ResultType.NO).executeForResult();
                return x.isPresent() && x.get().result() == CMsgBox.ResultType.YES;
            }
            return true;
        });

        menu.removeItem(CDockObject.PAGE);

        menu.addItem(ImageCacheManager.getImage("copyset.png", 32), COPYSET, Translator.translate("profile.menu.copyset"), a -> {
            var r = selector.show(null, Profiler.getProfiler().getAllProfiles());
            if (r.isEmpty())
                return;

            var prf = r.get().getProfiles().get(0);
            if (prf.equals(profile))
                return;

            var p1 = prf.getPath().to("options.txt");
            if (!p1.exists()){
                Main.getMain().announceLater(Translator.translate("error.oops"), Translator.translate("profile.options.error"), Announcement.AnnouncementType.ERROR, Duration.seconds(3));
                return;
            }

            var p2 = profile.getPath().to("options.txt");
            p1.copy(p2);

            Main.getMain().announceLater(Translator.translate("profile.options.title"), Translator.translateFormat("profile.options.ok", prf.getName()), Announcement.AnnouncementType.INFO, Duration.seconds(3));
        }, true);

        menu.addItem(ImageCacheManager.getImage("update.png", 32), UPDATE_ALL, Translator.translate("mods.all.update"), a ->
                new Thread(this::updateAll).start()
        );

        btnAddResource.setOnMouseClicked(a -> {
            if (a.getButton() == MouseButton.PRIMARY){
                Main.getMain().replaceTab(this, "pages/browser", "", true, BrowserPage.class).setProfile(profile);
                return;
            }
            if (a.getButton() != MouseButton.SECONDARY)
                return;

            var r = new DResourceSelector(profile).execute();
            if (r.isEmpty())
                return;

            try {
                Modder.getModder().include(profile, r.get());
            } catch (NoConnectionException | StopException | HttpException e) {
                if (!Main.getMain().announceLater(e, Duration.seconds(2)))
                    Logger.getLogger().log(e);
            }
        });

        btnMultipleBrowser.setOnMouseClicked(a -> Main.getMain().replaceTab(this, "pages/multiplebrowser", "", true, MultipleBrowserPage.class).setProfile(profile));
        btnWorlds.setOnMouseClicked(a -> Main.getMain().replaceTab(this, "pages/worlds", "", true, WorldsPage.class).setProfile(profile));

        rootNode.setOnDragOver(a -> {
            if (a.getDragboard().getContent(DataFormat.FILES) == null)
                return;

            a.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            dragMode(true);
        });

        rootNode.setOnDragExited(a -> dragMode(false));

        rootNode.setOnDragDropped(a -> {
            dragMode(false);

            var files = a.getDragboard().getFiles().stream().map(x -> Path.begin(x.toPath())).toList();
            var result = MultipleBrowserPage.loadFromPath(profile.getLoader().getType(), files);

            if (result.found().isEmpty()){
                Main.getMain().announceLater(Translator.translate("error.oops"), Translator.translate("import.error.incompatible"), Announcement.AnnouncementType.ERROR, Duration.seconds(2));
                return;
            }

            var page = Main.getMain().replaceTab(this, "pages/multiplebrowser", "", true, MultipleBrowserPage.class);
            page.setProfile(profile);
            page.loadFromResult(result);
        });

        var pane = (ScrollPane)rootNode;

        pane.vvalueProperty().addListener((a, b, c) -> {
            if (b.equals(c) || pane.getVmax() > c.doubleValue())
                return;
            lvMods.load(false);
        });
    }

    private void dragMode(boolean val){
        boxDrag.setManaged(val);
        boxDrag.setVisible(val);
    }

    private void updateAll(){
        try {
            UI.runAsync(() -> Main.getMain().getAnnouncer().announce(new Announcement(Translator.translate("announce.info.update.title"), Translator.translateFormat("announce.info.update.search.multiple", profile.getName()), Announcement.AnnouncementType.INFO), Duration.seconds(2)));
            var all = new ArrayList<CResource>(Modder.getModder().getModpackUpdates(profile, profile.getModpacks(false)));

            //var allStream = Stream.of(p.getMods().stream(), p.getResourcepacks().stream(), p.getShaders().stream()).flatMap(n -> n).map(x -> (CResource)x).toList();

            var conflicts = new HashMap<Object, List<CResource>>();
            var c1 = Modder.getModder().getUpdates(profile, profile.getAllResources().stream().filter(x -> x.getType() != ResourceType.MODPACK || x.getType() != ResourceType.WORLD).toList());
            for (var c : c1.keySet()){
                var n = c1.get(c);
                if (n.size() == 1)
                    all.add(n.get(0));
                else if (n.size() == 2){
                    var n0 = n.get(0);
                    var n1 = n.get(1);
                    all.add(n0.createDate.before(n1.createDate) ? n0 : n1);
                }
                else{
                    var dis = n.stream().distinct().toList();
                    if (dis.size() >= 3)
                        conflicts.put(c, dis);
                    else if (dis.size() == 2){
                        var n0 = dis.get(0);
                        var n1 = dis.get(1);
                        all.add(n0.createDate.before(n1.createDate) ? n0 : n1);
                    }
                    else
                        all.add(dis.get(0));
                }
            }

            Modder.getModder().include(profile, all);

            if (!conflicts.isEmpty()){
                var result = CMsgBox.msg(Alert.AlertType.WARNING, Translator.translate("announce.info.update.title"), Translator.translateFormat("announce.info.update.conflict", all.size(), conflicts.size(), String.join(",", conflicts.values().stream().map(k -> k.get(0).name).toList()))).setButtons(CMsgBox.ResultType.OPTION, CMsgBox.ResultType.OPTION, CMsgBox.ResultType.OPTION).executeForResult();
                if (result.isEmpty() || (int)result.get().extra() == 3)
                    return;
                all.clear();
                boolean reverse = (int)result.get().extra() == 1; // latest
                all.addAll(conflicts.values().stream().map(f -> {
                    Collections.sort(f);
                    return f.get(reverse ? f.size() -1 : 0);
                }).toList());

                Modder.getModder().include(profile, all);

                UI.runAsync(() -> Main.getMain().getAnnouncer().announce(new Announcement(Translator.translate("announce.info.update.title"), Translator.translateFormat("announce.info.update.ok.multiple", profile.getName(), all.size()), Announcement.AnnouncementType.INFO), Duration.seconds(2)));
            }
            else
                UI.runAsync(() -> Main.getMain().getAnnouncer().announce(new Announcement(Translator.translate("announce.info.update.title"), Translator.translateFormat("announce.info.update.ok.multiple", profile.getName(), all.size()), Announcement.AnnouncementType.INFO), Duration.seconds(2)));

            UI.runAsync(() -> setProfile(profile));

        } catch (NoConnectionException | HttpException | StopException ignored) {

        }
    }

    private <T extends CResource> void setSelectedResourcesDisabled(CList<T> list, boolean disabled){
        for (var x : list.getSelectedItems()){
            Profiler.setResourceDisabled(profile, x, disabled, false);
            for (var a : list.getList().getChildren()){
                if (a instanceof CPRCell<?> cpr && x.equals(cpr.getItem())){
                    cpr.invalidateToggle();
                    break;
                }
            }
        }

        UI.runAsync(profile::save);
        list.setSelectionMode(false);
    }

    private <T extends CResource> void updateSelectedResources(CList<T> list){

    }

    private <T extends CResource> void removeSelectedResources(CList<T> list){
        var items = list.getSelectedItems();
        Modder.getModder().removeAll(profile, (ObservableList<CResource>)items);
        list.getItems().removeAll(items);
        list.setSelectionMode(false);
    }

    @Override
    public void preInit() {
        lvModpacks.setSelectionEnabled(false);
        btnMenuMods.setOnMouseClicked(a -> lvMods.setSelectionMode(!lvMods.getSelectionMode()));
        lvWorlds.setSelectionEnabled(false);
        btnMenuResources.setOnMouseClicked(a -> lvResources.setSelectionMode(!lvResources.getSelectionMode()));
        btnMenuShaders.setOnMouseClicked(a -> lvShaders.setSelectionMode(!lvShaders.getSelectionMode()));

        vMods.getChildren().add(lvMods);
        vModpacks.getChildren().add(lvModpacks);
        vWorlds.getChildren().add(lvWorlds);
        vResources.getChildren().add(lvResources);
        vShaders.getChildren().add(lvShaders);

        /*lvMods.setCellFactory(this::cellFactory);
        lvModpacks.setCellFactory(this::cellFactory);
        lvWorlds.setCellFactory(this::cellFactory);
        lvResources.setCellFactory(this::cellFactory);
        lvShaders.setCellFactory(this::cellFactory);*/
        lvMods.setCellFactory(() -> cellFactory(lvMods, true));
        lvModpacks.setCellFactory(() -> cellFactory(lvModpacks, false));
        lvWorlds.setCellFactory(() -> cellFactory(lvWorlds, false));
        lvResources.setCellFactory(() -> cellFactory(lvResources, true));
        lvShaders.setCellFactory(() -> cellFactory(lvShaders, true));

        lvMods.selectionModeProperty().addListener(this.new ResourceSelectionModeListener(lvMods));
        lvResources.selectionModeProperty().addListener(this.new ResourceSelectionModeListener(lvResources));
        lvShaders.selectionModeProperty().addListener(this.new ResourceSelectionModeListener(lvShaders));

        /*lvMods.setItemEqualsFactory(CResource::isSameResource);
        lvModpacks.setItemEqualsFactory(CResource::isSameResource);
        lvWorlds.setItemEqualsFactory(CResource::isSameResource);
        lvResources.setItemEqualsFactory(CResource::isSameResource);
        lvShaders.setItemEqualsFactory(CResource::isSameResource);*/

        Predicate<CList.Filter<CResource>> f = (a) -> a.input().name.toLowerCase(Locale.US).contains(a.query().toLowerCase(Locale.US));

        lvMods.setFilterFactory(f);
        lvModpacks.setFilterFactory(f);
        lvWorlds.setFilterFactory(f);
        lvResources.setFilterFactory(f);
        lvShaders.setFilterFactory(f);

        lvMods.setOnVisibleCountChanged(c -> invalidateCount(c, lblMods, "mods.type.mods"));
        lvModpacks.setOnVisibleCountChanged( c -> invalidateCount(c, lblModpacks, "mods.type.modpacks"));
        lvWorlds.setOnVisibleCountChanged( c -> invalidateCount(c, lblWorlds, "mods.type.worlds"));
        lvResources.setOnVisibleCountChanged( c -> invalidateCount(c, lblResources, "mods.type.resources"));
        lvShaders.setOnVisibleCountChanged( c -> invalidateCount(c, lblShaders, "mods.type.shaders"));

        prepareListNavigation(lvMods);
        prepareListNavigation(lvResources);
        prepareListNavigation(lvShaders);

        txtSearch.textProperty().addListener(a -> {
            String text = txtSearch.getText();
            lvMods.filter(text);
            lvModpacks.filter(text);
            lvWorlds.filter(text);
            lvResources.filter(text);
            lvShaders.filter(text);
        });

        imgProfile.setCornerRadius(192, 192, 40);
        imgUserHead.setCornerRadius(32, 32, 16);

        txtSearch.setFocusedAnimation(Duration.millis(200));


    }

    @Override
    public void init(){
        addRegisteredEventFilter(EventFilter.node(getRootNode(), KeyEvent.KEY_RELEASED, a -> {
            if (a.getTarget().equals(txtSearch))
                return;

            if (lastActiveList != null && lastActiveList.onKeyEvent(a))
                return;
        }));
    }

    private void invalidateCount(int size, Label lbl, String translateKey){
        var str = Translator.translate(translateKey);
        if (size > 0)
            str += " (" + size + ")";

        lbl.setText(str);
    }

    private void prepareListNavigation(CList list){
        list.getNav().addItem(Translator.translate("option.enable"), "-shape-check", a -> {
            setSelectedResourcesDisabled(list, false);
        }, 1);
        list.getNav().addItem(Translator.translate("option.disable"), "-shape-uncheck", a -> {
            setSelectedResourcesDisabled(list, true);
        }, 1);
        /*list.getNav().addItem(Translator.translate("option.update"), "-shape-update", a -> {
            updateSelectedResources(list);
        }, 1);*/
        list.getNav().addItem(Translator.translate("option.remove"), "-shape-remove", a -> {
            var x = CMsgBox
                    .msg(Alert.AlertType.CONFIRMATION, Translator.translate("ask.ask"), Translator.translate("ask.sure"))
                    .setButtons(CMsgBox.ResultType.YES, CMsgBox.ResultType.NO)
                    .executeForResult();
            if (x.isEmpty() || x.get().result() != CMsgBox.ResultType.YES)
                return;
            removeSelectedResources(list);
        }, 1);
    }

    private <T extends CResource> CPRCell<T> cellFactory(CList<T> list, boolean selectable){
        return new CPRCell<T>(profile, selectable).setOnAction(k -> {
            var cell = (CPRCell<T>)k.getSource();
            var item = (T)k.getValue();

            ignoreReload = true;

            if (k.getKey().equals(CPRCell.UPDATE)){
                /*int index = list.getItems().indexOf(cell.getItem());
                if (index != -1)
                    list.getItems().set(index, item);*/

                // profile update method implemented, above lines are not necessary

                if (list.getSelectionMode() && list.getSelectedItems().contains(item)){
                    updateSelectedResources(list);
                    return false;
                }
            }
            else if (k.getKey().equals(CPRCell.REMOVE)){
                if (item instanceof Modpack)
                    setProfile(profile);
                else{
                    if (list.getSelectionMode() && list.getSelectedItems().contains(item)){
                        removeSelectedResources(list);
                        return false;
                    }
                    else
                        list.getItems().remove(item);
                }
            }

            return true;
        });
    }
}
