package com.laeben.corelauncher.ui.controller.page;

import com.laeben.core.entity.Path;
import com.laeben.core.util.events.ChangeEvent;
import com.laeben.corelauncher.api.Tool;
import com.laeben.corelauncher.api.entity.*;
import com.laeben.corelauncher.api.ui.Controller;
import com.laeben.corelauncher.api.ui.entity.Announcement;
import com.laeben.corelauncher.api.ui.entity.FocusLimiter;
import com.laeben.corelauncher.api.util.OSUtil;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.Profiler;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.minecraft.Wrapper;
import com.laeben.corelauncher.minecraft.wrapper.Custom;
import com.laeben.corelauncher.minecraft.wrapper.Vanilla;
import com.laeben.corelauncher.minecraft.wrapper.entity.WrapperVersion;
import com.laeben.corelauncher.ui.controller.HandlerController;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.ui.control.CButton;
import com.laeben.corelauncher.ui.control.CTab;
import com.laeben.corelauncher.ui.control.CView;
import com.laeben.corelauncher.ui.dialog.DImageSelector;
import com.laeben.corelauncher.ui.util.RAMManager;
import com.laeben.corelauncher.util.ImageCacheManager;
import com.laeben.corelauncher.util.ImageUtil;
import com.laeben.corelauncher.util.JavaManager;
import com.laeben.core.util.StrUtil;
import com.laeben.corelauncher.util.NTSManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;
import org.controlsfx.control.SearchableComboBox;

public class EditProfilePage extends HandlerController implements FocusLimiter {
    public static final String KEY = "pgedit";

    @FXML
    private TextField txtName;

    @FXML
    private RadioButton vanilla;
    @FXML
    private RadioButton forge;
    @FXML
    private RadioButton neoforge;
    @FXML
    private RadioButton fabric;
    @FXML
    private RadioButton quilt;
    @FXML
    private RadioButton optifine;
    @FXML
    private RadioButton custom;

    @FXML
    private ImageView imgVanilla;
    @FXML
    private ImageView imgForge;
    @FXML
    private ImageView imgNeoForge;
    @FXML
    private ImageView imgFabric;
    @FXML
    private ImageView imgQuilt;
    @FXML
    private ImageView imgOptiFine;
    @FXML
    private ImageView imgCustom;

    @FXML
    private CButton btnBack;

    @FXML
    private SearchableComboBox<String> cbWrapperVersion;
    @FXML
    private SearchableComboBox<String> cbGameVersion;
    @FXML
    private ComboBox<String> cbJavaVersion;
    @FXML
    private TextField txtAccount;
    @FXML
    private Button btnJavaManager;
    @FXML
    private Button btnSave;
    @FXML
    private CheckBox chkAccOnline;
    @FXML
    private TextField txtArgs;
    @FXML
    private Spinner<Integer> txtMinRAM;
    @FXML
    private Spinner<Integer> txtMaxRAM;
    @FXML
    private Slider sldRAM;
    @FXML
    private GridPane pWrapper;
    @FXML
    private TextField txtWrapper;
    @FXML
    private Button btnSelectWrapper;

    @FXML
    public CView icon;

    private final ToggleGroup wrapperGroup;
    private final ObservableList<String> versions;
    private final ObservableList<String> javaVersions;
    private final ObservableList<String> wrapperVersions;
    private final RAMManager ram;

    private Profile profile;
    private Profile tempProfile;

    /**
     * NTS Info
     * icon - 0
     * name - 1
     * loader - 2
     * version - 3
     * loader version - 4
     * java - 5
     * account - 6
     * online - 7
     * jvm - 8
     * mm ram - 9
     */
    private final NTSManager nts;
    private Bounds headerCache;

    public EditProfilePage setProfile(Profile p){
        profile = p;
        reload();
        return this;
    }

    public EditProfilePage(){
        super(KEY);
        wrapperGroup = new ToggleGroup();
        nts = new NTSManager(10);
        nts.setOnSet(a -> {
            boolean f = nts.needsToSave();
            boolean k = nts.calcNts();
            if (k && !profile.isEmpty())
                Main.getMain().setFocusLimiter(this);
            else
                Main.getMain().setFocusLimiter(null);

            if (f != k || a == 1)
                reloadTitle(tempProfile);
        });

        ram = new RAMManager() {
            @Override
            public void needsToSave() {
                nts.set(9, true);
                //needsToSave = true;
            }
        };

        var showOld = Configurator.getConfig().isShowOldReleases();
        var showSnap = Configurator.getConfig().isShowSnapshots();
        javaVersions = FXCollections.observableArrayList();
        reloadJava();
        versions = FXCollections.observableList(Vanilla.getVanilla().getAllVersions().stream().filter(x -> x.type == null || (x.type.equals("snapshot") && showSnap) || ((x.type.equals("old_beta") || x.type.equals("old_alpha")) && showOld) || x.type.equals("release")).map(x -> x.id).toList());
        wrapperVersions = FXCollections.observableArrayList();
        wrapperVersions.add("...");

        registerHandler(JavaManager.getManager().getHandler(), a -> {
            if (!(a instanceof ChangeEvent ce))
                return;

            switch (a.getKey()){
                case JavaManager.ADD -> {
                    var java = (Java)ce.getNewValue();
                    javaVersions.add(java.toIdentifier());
                }
                case JavaManager.UPDATE -> reloadJava();
                case JavaManager.DELETE -> {
                    var java = (Java)ce.getOldValue();
                    javaVersions.remove(java.toIdentifier());
                    if (java.toIdentifier().equals(cbJavaVersion.getValue()))
                        cbJavaVersion.setValue("...");
                }
            }
        }, true);
    }

    private void reloadJava(){
        var vers = JavaManager.getManager().getAllJavaVersions();
        javaVersions.setAll(vers.stream().map(Java::toIdentifier).toList());
        javaVersions.add(0, "...");

        if (cbJavaVersion == null)
            return;

        var defJava = tempProfile.getJava();
        if (defJava != null){
            var path = defJava.getPath().toString();
            for (int i = 0; i < vers.size(); i++) {
                var v = vers.get(i);
                if (v.getPath().toString().equals(path)){
                    cbJavaVersion.getSelectionModel().select(i + 1);
                    break;
                }
            }
        }
    }

    @Override
    public Node getTargetFocusNode() {
        return getRootNode();
    }

    @Override
    public void onFocusLimitIgnored(Controller by, Node target){
        Main.getMain().announceLater(Translator.translate("announce.warn"), Translator.translate("profile.edit.focus"), Announcement.AnnouncementType.ERROR, Duration.millis(1000));
    }

    @Override
    public void preInit(){
        btnBack.enableTransparentAnimation();
        btnBack.setText("⤶ " + Translator.translate("option.back"));
        btnBack.setOnMouseClicked(a -> Main.getMain().replaceTab(this, "pages/profile", profile.getName(), true, ProfilePage.class).setProfile(profile));

        ram.setControls(txtMinRAM, txtMaxRAM, sldRAM);
        ram.setup();

        icon.setCornerRadius(icon.getFitWidth(), icon.getFitHeight(), 16);
        icon.setOnMouseClicked(a -> {
            var s = new DImageSelector();
            var x = s.action();
            if (x.isEmpty())
                return;
            var ent = x.get();
            tempProfile.setIcon(ent.isEmpty() ? null : ent);
            ImageCacheManager.remove(profile);
            icon.setImageAsync(ImageUtil.getImage(tempProfile.getIcon()));
            nts.set(0, true);
        });

        cbGameVersion.valueProperty().addListener(x -> {
            String value = cbGameVersion.getValue();

            if (value == null || value.isEmpty())
                return;

            tempProfile.setVersionId(value);

            nts.set(3, !value.equals(profile.getVersionId()));

            refreshWrapperVersions();
        });
        cbGameVersion.setItems(versions);
        cbWrapperVersion.setItems(wrapperVersions);

        cbJavaVersion.valueProperty().addListener(x -> {
            var java = tryGetSelectedJava();

            tempProfile.setJava(java);

            if (profile == null || profile.isEmpty())
                return;
            if (profile.getJava() == null && java == null)
                nts.set(5, false);

            nts.set(5, java != null && profile.getJava() == null || !profile.getJava().equals(java));
        });
        cbJavaVersion.setItems(javaVersions);

        btnJavaManager.setOnMouseClicked((a) -> {
            if (nts.needsToSave())
                onFocusLimitIgnored(null, null);
            else
                Main.getMain().addTab("pages/java", Translator.translate("frame.title.javaman"), true, JavaPage.class);
        });


        imgVanilla.setOnMouseClicked(a -> wrapperGroup.selectToggle(vanilla));
        imgForge.setOnMouseClicked(a -> wrapperGroup.selectToggle(forge));
        imgNeoForge.setOnMouseClicked(a -> wrapperGroup.selectToggle(neoforge));
        imgFabric.setOnMouseClicked(a -> wrapperGroup.selectToggle(fabric));
        imgQuilt.setOnMouseClicked(a -> wrapperGroup.selectToggle(quilt));
        imgOptiFine.setOnMouseClicked(a -> wrapperGroup.selectToggle(optifine));
        imgCustom.setOnMouseClicked(a -> wrapperGroup.selectToggle(custom));

        vanilla.setToggleGroup(wrapperGroup);
        vanilla.setTooltip(new Tooltip("Vanilla"));
        Tooltip.install(imgVanilla, vanilla.getTooltip());

        forge.setToggleGroup(wrapperGroup);
        forge.setTooltip(new Tooltip("Forge"));
        Tooltip.install(imgForge, forge.getTooltip());

        neoforge.setToggleGroup(wrapperGroup);
        neoforge.setTooltip(new Tooltip("NeoForge"));
        Tooltip.install(imgNeoForge, neoforge.getTooltip());

        fabric.setToggleGroup(wrapperGroup);
        fabric.setTooltip(new Tooltip("Fabric"));
        Tooltip.install(imgFabric, fabric.getTooltip());

        quilt.setToggleGroup(wrapperGroup);
        quilt.setTooltip(new Tooltip("Quilt"));
        Tooltip.install(imgQuilt, quilt.getTooltip());

        optifine.setToggleGroup(wrapperGroup);
        optifine.setTooltip(new Tooltip("OptiFine"));
        Tooltip.install(imgOptiFine, optifine.getTooltip());

        custom.setToggleGroup(wrapperGroup);
        custom.setTooltip(new Tooltip(Translator.translate("mods.custom")));
        Tooltip.install(imgCustom, custom.getTooltip());

        wrapperGroup.selectToggle(vanilla);

        wrapperGroup.selectedToggleProperty().addListener(a -> {
            var wrapper = Wrapper.getWrapper(((RadioButton)wrapperGroup.getSelectedToggle()).getId());
            nts.set(2, profile.getWrapper().getType() != wrapper.getType());
            tempProfile.setWrapper(wrapper);
            if (cbGameVersion.getValue() != null && !cbGameVersion.getValue().isBlank())
                refreshWrapperVersions();
        });

        cbWrapperVersion.valueProperty().addListener(a -> {
            if (tempProfile.getVersionId() == null)
                return;

            var value = cbWrapperVersion.getValue();

            if (value == null || value.isEmpty())
                return;

            tempProfile.setWrapperVersion(value);
            nts.set(3, !value.equals(profile.getWrapperVersion()));
        });
        txtWrapper.setCursor(Cursor.HAND);
        btnSelectWrapper.setOnMouseClicked(a -> {
            var chooser = new DirectoryChooser();
            var file = chooser.showDialog(btnSelectWrapper.getScene().getWindow());
            if (file == null)
                return;
            var path = Path.begin(file.toPath());
            var json = path.to(path.getName() + ".json");
            if (!path.parent().equals(Configurator.getConfig().getGamePath().to("versions")) || !json.exists()){
                txtWrapper.setText(Translator.translate("error.wrongPath"));
                return;
            }

            try{
                txtWrapper.setText(path.toString());
                tempProfile.setWrapperVersion(path.getName());
            }
            catch (Exception ignored){
                txtWrapper.setText(Translator.translate("error.wrongPath"));
            }

        });
        txtWrapper.setOnMouseClicked(a -> {
            var path = ((Custom)tempProfile.getWrapper()).getPath(tempProfile.getVersionId());
            if (path.exists())
                OSUtil.open(path.toFile());
        });

        btnSave.setOnMouseClicked(a -> {
            String name = txtName.getText();

            if (name == null || name.isBlank()){
                Main.getMain().getAnnouncer().announce(new Announcement(
                        Translator.translate("error.oops"),
                        Translator.translate("profile.edit.error.name"),
                        Announcement.AnnouncementType.ERROR), Duration.seconds(2));
                return;
            }

            if (!Tool.checkStringValidity(name, Tool.ValidityDegree.HIGH)){
                Main.getMain().getAnnouncer().announce(new Announcement(
                        Translator.translate("error.oops"),
                        Translator.translate("profile.edit.error.invalidName"),
                        Announcement.AnnouncementType.ERROR), Duration.seconds(2));
                return;
            }

            if (name.endsWith("."))
                name = StrUtil.trimEnd(name, '.');

            tempProfile
                    .setName(name)
                    .setJvmArgs(txtArgs.getText().split(" "))
                    .setMinRAM(ram.getMin())
                    .setMaxRAM(ram.getMax());
            if (txtAccount.getText() == null || txtAccount.getText().isBlank())
                tempProfile.setCustomUser(null);
            else
                tempProfile.setCustomUser(Account.fromUsername(txtAccount.getText()).setOnline(chkAccOnline.isSelected()));

            boolean check1 = tempProfile.getVersionId() == null || tempProfile.getVersionId().isBlank();
            boolean check2 = tempProfile.getWrapper() != null && !(tempProfile.getWrapper() instanceof Vanilla) && (tempProfile.getWrapperVersion() == null || tempProfile.getWrapperVersion().isBlank() || tempProfile.getWrapperVersion().equals("..."));
            if (check1 || check2){
                Main.getMain().getAnnouncer().announce(new Announcement(
                        Translator.translate("error.oops"),
                        Translator.translate("profile.edit.error.wrapper"),
                        Announcement.AnnouncementType.ERROR), Duration.seconds(2));
                return;
            }

            boolean isNew = profile.isEmpty();

            if (!name.equals(profile.getName()) && !Profiler.getProfiler().getProfile(name).isEmpty()) {
                Main.getMain().getAnnouncer().announce(new Announcement(
                        Translator.translate("error.oops"),
                        Translator.translate("profile.edit.error.contains"),
                        Announcement.AnnouncementType.ERROR), Duration.seconds(2));
                return;
            }

            try{
                if (isNew){
                    var p = Profiler.getProfiler().createAndSetProfile(txtName.getText(), b -> b.cloneFrom(tempProfile));
                    setProfile(p);
                }
                else
                    Profiler.getProfiler().setProfile(profile.getName(), b -> b.cloneFrom(tempProfile));
            }
            catch (Exception e){
                Logger.getLogger().log(e);
            }

            Main.getMain().setFocusLimiter(null);

            if (isNew && Configurator.getConfig().shouldPlaceNewProfileToDock()){
                Main.getMain().closeTab((CTab)getParentObject());
                Main.getMain().getTab().getSelectionModel().select(0);
            }
            else if (isNew)
                Main.getMain().replaceTab(this, "pages/profile", profile.getName(), true, ProfilePage.class).setProfile(profile);
            else{
                nts.clear();
                Main.getMain().announceLater(Translator.translate("announce.successful"), Translator.translate("announce.info.profile.save"), Announcement.AnnouncementType.INFO, Duration.seconds(2));
            }
        });


    }

    @Override
    public void init(){
        reload();

        txtName.textProperty().addListener((a, b, c) -> {
            tempProfile.setName(c);
            nts.set(1, !c.equals(profile.getName()));
        });
        txtWrapper.textProperty().addListener((a, b, c) -> {
            nts.set(4, true);
        });
        txtAccount.textProperty().addListener((a, b, c) -> {
            nts.set(6, !profile.tryGetUser().getUsername().equals(c));
        });
        chkAccOnline.selectedProperty().addListener((a, b, c) -> {
            nts.set(7, profile.tryGetUser().isOnline() != c);
        });
        txtArgs.textProperty().addListener((a, b, c) -> {
            nts.set(8, true);
        });
    }

    private Java tryGetSelectedJava(){
        String text = cbJavaVersion.getValue();
        if (text.equals("..."))
            return null;
        String name = text.split(" - ")[0];
        return JavaManager.getManager().tryGet(new Java(name));
    }

    private void refreshWrapperVersions(){
        /*cbWrapperVersion.getItems().clear();
        cbWrapperVersion.getItems().add("...");*/
        wrapperVersions.setAll("...");
        cbWrapperVersion.setValue(null);


        String versionId = tempProfile.getVersionId();
        var wr = tempProfile.getWrapper();
        if (wr instanceof Custom){
            pWrapper.setVisible(true);
            cbWrapperVersion.setVisible(false);
        }
        else{
            pWrapper.setVisible(false);
            cbWrapperVersion.setVisible(true);
            if (!(wr instanceof Vanilla)){
                var m = tempProfile.getWrapper().getVersions(versionId).stream().map(x -> ((WrapperVersion)x).getWrapperVersion()).toList();
                //cbWrapperVersion.getItems().addAll(m);
                wrapperVersions.addAll(m);
            }

        }
    }

    private void reloadTitle(Profile p){
        ((CTab)parentObj).setText(Translator.translate("frame.title.pedit") + (p.getName() == null ? "" : " - " + StrUtil.sub(p.getName(), 0, 30) + (nts.needsToSave() ? "*" : "")));
    }

    private void reload(){
        if (profile == null){
            tempProfile = Profile.empty();
            profile = Profile.empty();
        }
        else
            tempProfile = Profile.empty().cloneFrom(profile);

        try{
            btnBack.setVisible(!profile.isEmpty());

            txtName.setText(tempProfile.getName());
            reloadTitle(tempProfile);

            cbGameVersion.setValue(tempProfile.getVersionId());

            var j = tempProfile.getJava();
            cbJavaVersion.setValue(j != null ? j.toIdentifier() : "...");

            icon.setImageAsync(ImageUtil.getImage(tempProfile.getIcon()));

            if (tempProfile.getUser() != null){
                txtAccount.setText(tempProfile.getUser().getUsername());
                chkAccOnline.setSelected(tempProfile.getUser().isOnline());
            }
            else{
                txtAccount.setText(null);
                chkAccOnline.setSelected(false);
            }
            if (tempProfile.getJvmArgs() != null)
                txtArgs.setText(String.join(" ", tempProfile.getJvmArgs()));

            Wrapper wr = tempProfile.getWrapper();
            var wrapperToggle = wrapperGroup.getToggles().stream().filter(x -> ((RadioButton)x).getId().equals(wr.getType().getIdentifier())).findFirst().orElse(vanilla);
            wrapperGroup.selectToggle(wrapperToggle);

            refreshWrapperVersions();
            if (wr instanceof Custom c){
                txtWrapper.setText(c.getPath(tempProfile.getWrapperVersion()).toString());
            }
            else if (!(wr instanceof Vanilla) && tempProfile.getWrapperVersion() != null){
                cbWrapperVersion.setValue(tempProfile.getWrapperVersion());
            }
            ram.setDefaultMin(tempProfile.getMinRAM());
            ram.setDefaultMax(tempProfile.getMaxRAM());
            sldRAM.setValue(tempProfile.getMaxRAM());
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }
    }

    @Override
    public boolean verify(double mX, double mY) {
        if (headerCache == null){
            var pane = (StackPane)Main.getMain().getTab().lookup(".tab-header-area .headers-region");
            headerCache = pane.localToScene(pane.getBoundsInLocal());
        }
        return (headerCache.getMaxX() < mX && headerCache.getMaxY() > mY) || FocusLimiter.super.verify(mX, mY);
    }

    @Override
    public void dispose(){
        ram.dispose();
        Main.getMain().setFocusLimiter(null);
        super.dispose();
    }
}
