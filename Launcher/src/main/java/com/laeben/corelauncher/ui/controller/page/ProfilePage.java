package com.laeben.corelauncher.ui.controller.page;

import com.laeben.core.entity.Path;
import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.api.ui.entity.Announcement;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.Profiler;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.api.util.OSUtil;
import com.laeben.corelauncher.minecraft.modding.Modder;
import com.laeben.corelauncher.minecraft.modding.entity.CResource;
import com.laeben.corelauncher.ui.controller.HandlerController;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.ui.controller.cell.CPRCell;
import com.laeben.corelauncher.ui.control.*;
import com.laeben.corelauncher.ui.dialog.DProfileSelector;
import com.laeben.corelauncher.ui.dialog.DResourceSelector;
import com.laeben.corelauncher.ui.util.ProfileUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.DataFormat;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ProfilePage extends HandlerController {

    public GridPane grid;

    private Profile profile;

    private final DProfileSelector selector;

    public ProfilePage(){
        super("pgprofile");
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

            if (a.getKey().equals("profileDelete"))
                Main.getMain().getTab().getTabs().remove((Tab) this.parentObj);
            else if (a.getKey().equals("profileUpdate"))
                setProfile((Profile)a.getNewValue());
        }, true);

        registerHandler(Configurator.getConfigurator().getHandler(), a -> {
            if (a.getKey().equals("userChange")){
                setUser();
            }
        }, true);
    }

    @FXML
    private Label lblProfileName;
    @FXML
    private HBox badges;
    @FXML
    private ImageView imgUserHead;
    @FXML
    private CView imgProfile;

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

    private final CList<CResource> lvMods;
    private final CList<CResource> lvModpacks;
    private final CList<CResource> lvWorlds;
    private final CList<CResource> lvResources;
    private final CList<CResource> lvShaders;

    @FXML
    public Label lblUsername;
    @FXML
    public Label lblGameVersion;
    @FXML
    public Pane boxDrag;
    @FXML
    public CField txtSearch;
    @FXML
    public CButton btnEdit;
    @FXML
    public CButton btnAdd;
    @FXML
    public CButton btnExport;
    @FXML
    public CButton btnBackup;
    @FXML
    public CButton btnDelete;
    @FXML
    public CButton btnUpdate;
    @FXML
    public CButton btnWorlds;
    @FXML
    private CButton btnCopySettings;
    @FXML
    private CButton btnAddMultiple;
    @FXML
    private CButton btnOpenFolder;

    private void setUser(){
        var user = profile.tryGetUser().reload();
        imgUserHead.setImage(user.getHead());
        lblUsername.setText(user.getUsername());
    }

    public void setProfile(Profile p){
        profile = p;

        lblProfileName.setText(p.getName());

        var wrIcon = new ImageView(p.getWrapper().getIcon());
        wrIcon.setFitHeight(32);
        wrIcon.setFitWidth(32);
        var tip = new Tooltip(p.getWrapper().getType().getIdentifier() + " - " + p.getWrapperVersion());
        tip.setStyle("-fx-font-size: 12pt");
        Tooltip.install(wrIcon, tip);

        badges.getChildren().clear();
        badges.getChildren().add(wrIcon);

        imgProfile.setImage(CoreLauncherFX.getImageFromProfile(profile, 96, 96));

        setUser();

        lblGameVersion.setText(p.getVersionId());

        lvMods.setLoadLimit(10);
        lvMods.getItems().setAll(p.getMods().stream().map(x -> (CResource)x).toList());
        lvMods.load();

        lvModpacks.getItems().setAll(p.getModpacks().stream().map(x -> (CResource)x).toList());
        lvModpacks.load();

        lvWorlds.getItems().setAll(p.getOnlineWorlds().stream().map(x -> (CResource)x).toList());
        lvWorlds.load();

        lvResources.getItems().setAll(p.getResources().stream().map(x -> (CResource)x).toList());
        lvResources.load();

        lvShaders.getItems().setAll(p.getShaders().stream().map(x -> (CResource)x).toList());
        lvShaders.load();

        btnEdit.setOnMouseClicked(a -> Main.getMain().replaceTab(this, "pages/profileedit", "", true, EditProfilePage.class).setProfile(profile));
        btnAdd.setOnMouseClicked(a -> {
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
                Modder.getModder().includeAll(profile, r.get());
            } catch (NoConnectionException | StopException | HttpException e) {
                if (!Main.getMain().announceLater(e, Duration.seconds(2)))
                    Logger.getLogger().log(e);
            }
        });

        btnAddMultiple.setOnMouseClicked(a -> Main.getMain().replaceTab(this, "pages/multiplebrowser", "", true, MultipleBrowserPage.class).setProfile(profile));
        btnWorlds.setOnMouseClicked(a -> Main.getMain().replaceTab(this, "pages/worlds", "", true, WorldsPage.class).setProfile(profile));
        btnExport.setOnMouseClicked(a -> ProfileUtil.export(profile, btnExport.getScene().getWindow()));
        btnBackup.setOnMouseClicked(a -> ProfileUtil.backup(profile, btnBackup.getScene().getWindow()));
        btnDelete.setOnMouseClicked(a -> {
            var x = CMsgBox.msg(Alert.AlertType.CONFIRMATION, Translator.translate("ask.ask"), Translator.translate("ask.sure"))
                            .setButtons(CMsgBox.ResultType.YES, CMsgBox.ResultType.NO).executeForResult();
            if (x.isPresent() && x.get().result() == CMsgBox.ResultType.YES)
                Profiler.getProfiler().deleteProfile(profile);
        });
        btnOpenFolder.setOnMouseClicked(a -> OSUtil.openFolder(profile.getPath().toFile().toPath()));
        btnUpdate.setOnMouseClicked(a -> new Thread(() -> {
            try {
                Platform.runLater(() -> Main.getMain().getAnnouncer().announce(new Announcement(Translator.translate("announce.info.update.title"), Translator.translateFormat("announce.info.update.search.multiple", profile.getName()), Announcement.AnnouncementType.INFO), Duration.seconds(2)));
                var all = new ArrayList<CResource>(Modder.getModder().getModpackUpdates(p, p.getModpacks()));

                var allStream = Stream.of(p.getMods().stream(), p.getResources().stream(), p.getShaders().stream()).flatMap(n -> n).map(x -> (CResource)x).toList();

                var conflicts = new HashMap<Object, List<CResource>>();
                var c1 = Modder.getModder().getUpdates(p, allStream);
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

                Modder.getModder().includeAll(p, all);

                if (!conflicts.isEmpty()){
                    var result = CMsgBox.msg(Alert.AlertType.WARNING, Translator.translate("announce.info.update.title"), Translator.translateFormat("announce.info.update.conflict", all.size(), conflicts.keySet().size(), String.join(",", conflicts.values().stream().map(k -> k.get(0).name).toList()))).setButtons(CMsgBox.ResultType.OPTION, CMsgBox.ResultType.OPTION, CMsgBox.ResultType.OPTION).executeForResult();
                    if (result.isEmpty() || (int)result.get().extra() == 3)
                        return;
                    all.clear();
                    boolean reverse = (int)result.get().extra() == 1; // latest
                    all.addAll(conflicts.values().stream().map(f -> {
                        Collections.sort(f);
                        return f.get(reverse ? f.size() -1 : 0);
                    }).toList());

                    Modder.getModder().includeAll(p, all);

                    Platform.runLater(() -> Main.getMain().getAnnouncer().announce(new Announcement(Translator.translate("announce.info.update.title"), Translator.translateFormat("announce.info.update.ok.multiple", profile.getName(), all.size()), Announcement.AnnouncementType.INFO), Duration.seconds(2)));
                }
                else
                    Platform.runLater(() -> Main.getMain().getAnnouncer().announce(new Announcement(Translator.translate("announce.info.update.title"), Translator.translateFormat("announce.info.update.ok.multiple", profile.getName(), all.size()), Announcement.AnnouncementType.INFO), Duration.seconds(2)));

            } catch (NoConnectionException | HttpException | StopException ignored) {

            }

        }).start());

        btnCopySettings.setOnMouseClicked(a -> {
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
        });

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
            var result = MultipleBrowserPage.loadFromPath(profile.getWrapper().getType(), files);

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
            lvMods.load();
        });
    }

    private void dragMode(boolean val){
        boxDrag.setManaged(val);
        boxDrag.setVisible(val);
    }

    @Override
    public void preInit() {
        vMods.getChildren().add(lvMods);
        vModpacks.getChildren().add(lvModpacks);
        vWorlds.getChildren().add(lvWorlds);
        vResources.getChildren().add(lvResources);
        vShaders.getChildren().add(lvShaders);

        lvMods.setCellFactory(this::cellFactory);
        lvModpacks.setCellFactory(this::cellFactory);
        lvWorlds.setCellFactory(this::cellFactory);
        lvResources.setCellFactory(this::cellFactory);
        lvShaders.setCellFactory(this::cellFactory);

        lvMods.setItemEqualsFactory(CResource::isSameResource);
        lvModpacks.setItemEqualsFactory(CResource::isSameResource);
        lvWorlds.setItemEqualsFactory(CResource::isSameResource);
        lvResources.setItemEqualsFactory(CResource::isSameResource);
        lvShaders.setItemEqualsFactory(CResource::isSameResource);

        Predicate<CList.Filter<CResource>> f = (a) -> a.input().name.toLowerCase(Locale.US).contains(a.query().toLowerCase(Locale.US));

        lvMods.setFilterFactory(f);
        lvModpacks.setFilterFactory(f);
        lvWorlds.setFilterFactory(f);
        lvResources.setFilterFactory(f);
        lvShaders.setFilterFactory(f);

        txtSearch.textProperty().addListener(a -> {
            String text = txtSearch.getText();
            lvMods.filter(text);
            lvModpacks.filter(text);
            lvWorlds.filter(text);
            lvResources.filter(text);
            lvShaders.filter(text);
        });

        imgProfile.setCornerRadius(128, 128, 40);

        txtSearch.setFocusedAnimation(Color.TEAL, Duration.millis(200));
    }

    private CPRCell cellFactory(){
        return new CPRCell(profile);
    }
}
