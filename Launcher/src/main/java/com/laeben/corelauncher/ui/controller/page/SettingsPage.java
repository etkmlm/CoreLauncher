package com.laeben.corelauncher.ui.controller.page;

import com.laeben.core.util.events.ChangeEvent;
import com.laeben.corelauncher.CoreLauncher;
import com.laeben.corelauncher.LauncherConfig;
import com.laeben.corelauncher.api.Tool;
import com.laeben.corelauncher.api.ui.entity.Announcement;
import com.laeben.corelauncher.api.util.OSUtil;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.Profiler;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.Account;
import com.laeben.corelauncher.api.entity.Config;
import com.laeben.corelauncher.discord.Discord;
import com.laeben.corelauncher.discord.entity.Activity;
import com.laeben.corelauncher.ui.control.*;
import com.laeben.corelauncher.ui.controller.HandlerController;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.api.ui.UI;
import com.laeben.corelauncher.ui.dialog.DProfileSelector;
import com.laeben.corelauncher.ui.util.RAMManager;
import com.laeben.corelauncher.util.ImageCacheManager;
import com.laeben.corelauncher.util.java.entity.JavaSourceType;
import com.laeben.corelauncher.util.java.JavaManager;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.entity.Java;
import com.laeben.core.entity.Path;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;

public class SettingsPage extends HandlerController {
    public static final String KEY = "pgsettings";

    @FXML
    private TextField txtCustomBackground;
    @FXML
    private CButton btnSelectBackground;
    @FXML
    private ChoiceBox<String> cbLanguage;
    @FXML
    private TextField txtGamePath;
    @FXML
    private CButton btnSelectGamePath;
    @FXML
    private TextField txtAccount;
    @FXML
    private CheckBox chkOnline;
    @FXML
    private ChoiceBox<String> cbJava;
    @FXML
    private CButton btnJavaMan;
    @FXML
    private CheckBox chkPlaceDock;
    @FXML
    private Spinner<Integer> txtMinRAM;
    @FXML
    private Spinner<Integer> txtMaxRAM;
    @FXML
    private Spinner<Integer> txtThreads;
    @FXML
    private Spinner<Integer> txtScale;
    @FXML
    private Slider sldRAM;
    @FXML
    private CheckBox chkOldReleases;
    @FXML
    private CheckBox chkShowSnaps;
    @FXML
    private CheckBox chkLogMode;
    @FXML
    private CheckBox chkHideAfter;
    @FXML
    private CheckBox chkAutoUpdate;
    @FXML
    private CheckBox chkDebugLogMode;
    @FXML
    private CheckBox chkSelectPlay;
    @FXML
    private CheckBox chkAutoLoader;
    @FXML
    private CheckBox chkOverwriteImportDefault;
    /*@FXML
    private CButton btnSaveRAM;
    @FXML
    private CButton btnReset;*/
    @FXML
    private CheckBox chkGamelog;
    @FXML
    private Label lblVersion;
    @FXML
    private CButton btnCheckUpdate;
    @FXML
    private CButton btnClearImages;
    @FXML
    private CWorker workerImages;

    @FXML
    private CButton btnSelectDefaultOptions;
    @FXML
    private CButton btnResetDefaultOptions;
    @FXML
    private CButton btnOpenDefaultOptions;
    @FXML
    private CButton btnGC;

    @FXML
    private CheckBox chkDiscordEnable;
    @FXML
    private CheckBox chkInGameRPC;
    @FXML
    private CheckBox chkGuiShortcut;
    @FXML
    private CCombo<JavaSourceType> cbJavaSource;
    /*@FXML
    private Spinner txtCommPort;*/


    private CButton btnSave;

    private final SpinnerValueFactory.IntegerSpinnerValueFactory fThreads;
    private final SpinnerValueFactory.IntegerSpinnerValueFactory fScale;

    //private final SpinnerValueFactory.IntegerSpinnerValueFactory fCommPort;
    private final ObservableList<String> languages;
    private final ObservableList<String> javas;

    private final RAMManager ram;

    private boolean changeAccount;

    private boolean needsToRestart;

    private void tryToEnableSave(){
        if (btnSave != null && !btnSave.isVisible())
            btnSave.setVisible(true);
    }

    public SettingsPage(){
        super(KEY);

        ram = new RAMManager() {
            @Override
            public void needsToSave() {
                tryToEnableSave();
            }
        };
        fThreads = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE);
        fThreads.setAmountToStepBy(1);

        fScale = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE);
        fScale.setAmountToStepBy(1);
        //fCommPort = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, Integer.MAX_VALUE);
        //fCommPort.setAmountToStepBy(1);

        /*fCommPort.valueProperty().addListener(a -> {
            Configurator.getConfig().setCommPort(fCommPort.getValue());
            Configurator.save();
        });*/

        languages = FXCollections.observableList(Translator.getTranslator().getAllLanguages().stream().map(x -> x.getDisplayLanguage(x)).toList());
        javas = FXCollections.observableArrayList();
        reloadJava();

        registerHandler(Configurator.getConfigurator().getHandler(), x -> {
            if (x.getKey().equals(Configurator.GAME_PATH_CHANGE)){
                var path = (Path)x.getNewValue();
                txtGamePath.setText(path.toString());
            }
            else if (x.getKey().equals(Configurator.BACKGROUND_CHANGE)){
                var path = (Path)x.getNewValue();
                txtCustomBackground.setText(path == null ? null : path.toString());
            }
            else if (x.getKey().equals(Configurator.LANGUAGE_CHANGE)){
                Discord.getDiscord().setActivity(Activity.setForIdling());
                UI.getUI().reset();
            }
        }, true);

        registerHandler(JavaManager.getManager().getHandler(), a -> {
            if (!(a instanceof ChangeEvent ce))
                return;

            switch (a.getKey()){
                case JavaManager.ADD -> {
                    var java = (Java)ce.getNewValue();
                    javas.add(java.toIdentifier());
                }
                case JavaManager.UPDATE -> reloadJava();
                case JavaManager.DELETE -> {
                    var java = (Java)ce.getOldValue();
                    javas.remove(java.toIdentifier());
                    if (java.toIdentifier().equals(cbJava.getValue()))
                        cbJava.setValue("...");
                }
            }
        }, true);
    }

    private void reloadJava(){
        var vers = JavaManager.getManager().getAllJavaVersions();
        javas.setAll(vers.stream().map(Java::toIdentifier).toList());
        javas.add(0, "...");

        if (cbJava == null)
            return;

        var defJava = Configurator.getConfig().getDefaultJava();
        if (defJava != null){
            var path = defJava.getPath().toString();
            for (int i = 0; i < vers.size(); i++) {
                var v = vers.get(i);
                if (v.getPath().toString().equals(path)){
                    cbJava.getSelectionModel().select(i + 1);
                    break;
                }
            }
        }
    }

    @Override
    public void preInit(){
        cbJavaSource.setValueFactory(JavaSourceType::getDisplayName);
        cbJavaSource.getItems().setAll(JavaSourceType.values());

        reload();

        fThreads.valueProperty().addListener(a -> tryToEnableSave());
        fScale.valueProperty().addListener(a -> {
            needsToRestart = true;
            tryToEnableSave();
        });

        txtAccount.textProperty().addListener(a -> tryToEnableSave());

        ram.setControls(txtMinRAM, txtMaxRAM, sldRAM);
        ram.setup();

        btnGC.setOnMouseClicked(a -> System.gc());

        cbLanguage.valueProperty().addListener(x -> {
            try{
                String val = cbLanguage.getValue();
                var all = Translator.getTranslator().getAllLanguages();
                var l = all.stream().filter(y -> y.getDisplayLanguage(y).equals(val)).findFirst().orElse(null);
                if (l == null || l.equals(Configurator.getConfig().getLanguage()))
                    return;

                Translator.getTranslator().setLanguage(l);
                Configurator.getConfigurator().setLanguage(l);
            }
            catch (Exception e){
                Logger.getLogger().log(e);
            }
        });

        workerImages.begin().withTask(a -> new Task() {
            @Override
            protected Object call() {
                var usedNames = Profiler.getProfiler().getAllProfiles().stream().filter(a -> a.getIcon() != null && !a.getIcon().isEmpty() && !a.getIcon().isNetwork()).map(a -> a.getIcon().getIdentifier()).toList();
                var images = Configurator.getConfig().getImagePath();
                for (var f : images.getFiles()){
                    if (usedNames.contains(f.getName()))
                        continue;
                    f.delete();
                }

                ImageCacheManager.clear();
                return null;
            }
        }).onDone(a -> Main.getMain().announceLater(Translator.translate("settings"), Translator.translate("settings.images.done"), Announcement.AnnouncementType.INFO, Duration.seconds(2)));

        btnClearImages.setOnMouseClicked(a -> workerImages.run());

        btnSelectBackground.enableTransparentAnimation();
        btnSelectBackground.setOnMouseClicked(x -> {
            var chooser = new FileChooser();
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
            var file = chooser.showOpenDialog(btnSelectBackground.getScene().getWindow());
            if (file == null)
                return;

            try{
                var path = Path.begin(file.toPath());
                var bgPath = Configurator.getConfig().getLauncherPath().to("back.png");
                if (bgPath.exists())
                    bgPath.delete();
                path.copy(bgPath);

                Configurator.getConfigurator().setCustomBackground(bgPath);
            }
            catch (Exception e){
                Logger.getLogger().log(e);
            }
        });

        btnSelectDefaultOptions.setOnMouseClicked(a -> {
            var selector = new DProfileSelector(DProfileSelector.Functionality.SINGLE_PROFILE_SELECTOR);
            var x = selector.show(null, Profiler.getProfiler().getAllProfiles());
            if (x.isEmpty() || x.get().getProfiles().isEmpty())
                return;

            var profile = x.get().getProfiles().get(0);
            var options = profile.getPath().to("options.txt");
            if (!options.exists()){
                Main.getMain().announceLater(Translator.translate("error.oops"), Translator.translate("profile.options.error"), Announcement.AnnouncementType.ERROR, Duration.seconds(2));
                return;
            }

            options.copy(Configurator.getConfig().getLauncherPath().to("options.txt"));
            Main.getMain().announceLater(Translator.translate("profile.options.title"), Translator.translate("profile.options.okDefault"), Announcement.AnnouncementType.INFO, Duration.seconds(2));
        });

        btnResetDefaultOptions.setOnMouseClicked(a -> {
            var k = CMsgBox.msg(Alert.AlertType.CONFIRMATION, Translator.translate("ask.ask"), Translator.translate("ask.sure"))
                    .setButtons(CMsgBox.ResultType.YES, CMsgBox.ResultType.NO)
                    .executeForResult();

            if (k.isEmpty() || k.get().result() != CMsgBox.ResultType.YES)
                return;

            Configurator.getConfig().getLauncherPath().to("options.txt").delete();
            Main.getMain().announceLater(Translator.translate("announce.successful"), Translator.translate("profile.options.reset"), Announcement.AnnouncementType.INFO, Duration.seconds(2));
        });

        btnOpenDefaultOptions.setOnMouseClicked(a -> {
            var path = Configurator.getConfig().getLauncherPath().to("options.txt");
            if (!path.exists())
                return;
            OSUtil.open(path.toFile());
        });

        txtCustomBackground.setCursor(Cursor.HAND);
        txtCustomBackground.setOnMouseClicked(x -> {
            if (x.getButton() == MouseButton.PRIMARY){
                var path = Configurator.getConfig().getBackgroundImage();
                if (path != null)
                    OSUtil.open(path.parent().toFile());
            }
            else if (x.getButton() == MouseButton.SECONDARY){
                Configurator.getConfigurator().setCustomBackground(null);
            }
        });

        cbJava.valueProperty().addListener(x -> {
            var java = tryGetSelectedJava();

            Configurator.getConfig().setDefaultJava(java);
            Configurator.save();
        });

        cbJavaSource.setOnItemChanged(a -> Configurator.getConfigurator().setJavaSourceType(a));

        btnJavaMan.enableTransparentAnimation();
        btnJavaMan.setOnMouseClicked((a) -> Main.getMain().addTab("pages/java", Translator.translate("java.manager"), true, JavaPage.class));

        chkPlaceDock.selectedProperty().addListener(x -> {
            Configurator.getConfig().setPlaceNewProfileToDock(chkPlaceDock.isSelected());
            Configurator.save();
        });
        chkShowSnaps.selectedProperty().addListener(x -> {
            Configurator.getConfig().setShowSnapshots(chkShowSnaps.isSelected());
            Configurator.save();
        });
        chkOldReleases.selectedProperty().addListener(x -> {
            Configurator.getConfig().setShowOldReleases(chkOldReleases.isSelected());
            Configurator.save();
        });
        chkLogMode.selectedProperty().addListener(x -> {
            Configurator.getConfig().setLogMode(chkLogMode.isSelected());
            Configurator.save();
        });
        chkHideAfter.selectedProperty().addListener(x -> {
            Configurator.getConfig().setHideAfter(chkHideAfter.isSelected());
            Configurator.save();
        });
        chkAutoUpdate.selectedProperty().addListener(x -> {
            Configurator.getConfig().setAutoUpdate(chkAutoUpdate.isSelected());
            Configurator.save();
        });
        chkGuiShortcut.selectedProperty().addListener(x -> {
            Configurator.getConfig().setUseNonGuiShortcut(chkGuiShortcut.isSelected());
            Configurator.save();
        });

        chkDebugLogMode.selectedProperty().addListener(x -> {
            Configurator.getConfig().setDebugLogMode(chkDebugLogMode.isSelected());
            Configurator.save();
        });

        chkSelectPlay.selectedProperty().addListener(x -> {
            Configurator.getConfig().setEnabledSelectAndPlayDock(chkSelectPlay.isSelected());
            Configurator.save();
        });

        chkGamelog.selectedProperty().addListener(x -> {
            Configurator.getConfig().setDelGameLogs(chkGamelog.isSelected());
            Configurator.save();
        });

        chkDiscordEnable.selectedProperty().addListener(x -> {
            chkInGameRPC.setDisable(!chkDiscordEnable.isSelected());
            //txtCommPort.setDisable(!chkDiscordEnable.isSelected());
            Configurator.getConfig().setDisabledRPC(!chkDiscordEnable.isSelected());
            Configurator.save();
        });

        chkInGameRPC.selectedProperty().addListener(x -> {
            Configurator.getConfig().setEnabledInGameRPC(chkInGameRPC.isSelected());
            Configurator.save();
        });
        chkAutoLoader.selectedProperty().addListener(x -> {
            Configurator.getConfig().setAutoChangeLoader(chkAutoLoader.isSelected());
            Configurator.save();
        });
        chkOverwriteImportDefault.selectedProperty().addListener(x -> {
            Configurator.getConfig().setOverwriteImported(chkOverwriteImportDefault.isSelected());
            Configurator.save();
        });

        btnSelectGamePath.enableTransparentAnimation();
        btnSelectGamePath.setOnMouseClicked(x -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setInitialDirectory(Configurator.getConfig().getGamePath().toFile());
            var file = chooser.showDialog(btnSelectGamePath.getScene().getWindow());
            if (file == null)
                return;

            try{
                var path = Path.begin(file.toPath());
                var oldPath = Configurator.getConfig().getGamePath();
                if (!Tool.checkStringValidity(path.toString(), Tool.ValidityDegree.HIGH_PATH)){
                    var t = CMsgBox.msg(Alert.AlertType.WARNING, Translator.translate("ask.ask"), Translator.translate("settings.ask.invalidGamePath"))
                            .setButtons(CMsgBox.ResultType.YES, CMsgBox.ResultType.NO)
                            .executeForResult();
                    if (t.isEmpty() || t.get().result() == CMsgBox.ResultType.NO)
                        return;
                }

                Configurator.getConfigurator().setGamePath(path);
                var op = CMsgBox.msg(Alert.AlertType.CONFIRMATION, Translator.translate("ask.ask"), Translator.translate("settings.ask.gamePath"))
                        .setButtons(CMsgBox.ResultType.YES, CMsgBox.ResultType.NO)
                        .executeForResult();

                if (op.isPresent() && op.get().result() == CMsgBox.ResultType.YES)
                    Profiler.getProfiler().moveProfiles(oldPath);

                Profiler.getProfiler().reload();
            }
            catch (Exception e){
                Logger.getLogger().log(e);
            }
        });

        txtGamePath.setCursor(Cursor.HAND);
        txtGamePath.setOnMouseClicked(a -> OSUtil.open(Configurator.getConfig().getGamePath().toFile()));

        txtThreads.setValueFactory(fThreads);
        txtScale.setValueFactory(fScale);
        //txtCommPort.setValueFactory(fCommPort);

        txtAccount.setOnKeyPressed(a -> changeAccount = true);

        chkOnline.selectedProperty().addListener(a -> Configurator.getConfigurator().setDefaultAccount(Account.fromUsername(txtAccount.getText() == null || txtAccount.getText().isBlank() ? "IAMUSER" : txtAccount.getText()).setOnline(chkOnline.isSelected())));

        cbJava.setItems(javas);
        cbLanguage.setItems(languages);

        btnCheckUpdate.setText(Translator.translate("settings.checkup"));
        btnCheckUpdate.setOnMouseClicked(a -> CoreLauncher.updateCheck());
        lblVersion.setText(Double.toString(LauncherConfig.VERSION));
    }

    @Override
    public void onParentSet(Object n){
        var tab = (CTab)n;

        var c = Main.getExternalLayer(tab);

        var menuBox = new HBox();
        menuBox.setSpacing(8);
        AnchorPane.setBottomAnchor(menuBox, 20.0);
        AnchorPane.setRightAnchor(menuBox, 24.0);

        var btnReset = new CButton();
        btnReset.getStyleClass().addAll("circle-button", "reset-button");
        //btnReset.setText(Translator.translate("option.reset"));
        btnReset.setOnMouseClicked(a -> {
            var opt = CMsgBox.msg(Alert.AlertType.CONFIRMATION, Translator.translate("ask.ask"), Translator.translate("ask.sure"))
                    .setButtons(CMsgBox.ResultType.YES, CMsgBox.ResultType.NO)
                    .executeForResult();

            if (opt.isEmpty() || opt.get().result() != CMsgBox.ResultType.YES)
                return;

            Configurator.getConfigurator().reset();
            close();
        });

        btnSave = new CButton();
        btnSave.getStyleClass().addAll("circle-button", "save-button");
        //btnSave.setText(Translator.translate("option.save"));
        btnSave.setVisible(false);
        btnSave.setOnMouseClicked(x -> {
            if (changeAccount){
                Configurator.getConfigurator().setDefaultAccount(Account.fromUsername(txtAccount.getText() == null || txtAccount.getText().isBlank() ? "IAMUSER" : txtAccount.getText()).setOnline(chkOnline.isSelected()));
            }

            Configurator.getConfig().setUIScale(fScale.getValue());
            Configurator.getConfig().setDownloadThreadsCount(fThreads.getValue());
            Configurator.getConfig().setDefaultMinRAM(ram.getMin());
            Configurator.getConfig().setDefaultMaxRAM(ram.getMax());
            Configurator.save();

            btnSave.setVisible(false);

            if (needsToRestart){
                var r = CMsgBox.msg(Alert.AlertType.WARNING, Translator.translate("announce.warn"), Translator.translate("settings.warn.restart"))
                        .setButtons(CMsgBox.ResultType.YES, CMsgBox.ResultType.NO)
                        .executeForResult();

                if (r.isEmpty() || r.get().result() != CMsgBox.ResultType.YES)
                    return;


                CoreLauncher.restart();
            }
        });

        menuBox.getChildren().addAll(btnSave, btnReset);

        c.getChildren().add(menuBox);

        tab.setContent(c);
    }

    private void reload(){
        try{
            Config c = Configurator.getConfig();

            if (c.getDefaultJava() != null)
                cbJava.setValue(c.getDefaultJava().toIdentifier());
            else
                cbJava.setValue("...");

            cbJavaSource.setValue(c.getJavaSourceType());

            if (c.getUser() != null){
                txtAccount.setText(c.getUser().getUsername());
                chkOnline.setSelected(c.getUser().isOnline());
            }
            else{
                txtAccount.setText(null);
                chkOnline.setSelected(false);
            }

            ram.setDefaultMin(c.getDefaultMinRAM());
            ram.setDefaultMax(c.getDefaultMaxRAM());
            sldRAM.setValue(ram.getMax());
            fThreads.setValue(c.getDownloadThreadsCount());
            fScale.setValue(c.getUIScale());
            //fCommPort.setValue(c.getCommPort());
            //sldRAM.setValue(c.getDefaultMaxRAM());
            chkOldReleases.setSelected(c.isShowOldReleases());
            chkShowSnaps.setSelected(c.isShowSnapshots());
            chkPlaceDock.setSelected(c.shouldPlaceNewProfileToDock());
            txtGamePath.setText(c.getGamePath().toString());
            cbLanguage.setValue(c.getLanguage().getDisplayLanguage(c.getLanguage()));
            chkLogMode.setSelected(c.getLogMode());
            chkHideAfter.setSelected(c.hideAfter());
            chkAutoUpdate.setSelected(c.isEnabledAutoUpdate());
            chkDebugLogMode.setSelected(c.getDebugLogMode());
            chkSelectPlay.setSelected(c.isEnabledSelectAndPlayDock());
            chkGamelog.setSelected(c.delGameLogs());
            chkGuiShortcut.setSelected(c.useNonGUIShortcut());
            chkAutoLoader.setSelected(c.isAutoChangeLoader());
            chkOverwriteImportDefault.setSelected(c.isOverwriteImportedEnabled());

            chkInGameRPC.setDisable(c.isDisabledRPC());
            //txtCommPort.setDisable(c.isDisabledRPC());
            chkDiscordEnable.setSelected(!c.isDisabledRPC());
            chkInGameRPC.setSelected(c.isEnabledInGameRPC());

            var bgi = c.getBackgroundImage();
            if (bgi != null)
                txtCustomBackground.setText(bgi.toString());
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }
    }

    private Java tryGetSelectedJava(){
        String text = cbJava.getValue();
        if (text.equals("..."))
            return null;
        String name = text.split(" - ")[0];
        return JavaManager.getManager().tryGet(new Java(name));
    }

    @Override
    public void dispose(){
        ram.dispose();
        super.dispose();
    }
}
