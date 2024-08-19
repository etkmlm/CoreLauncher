package com.laeben.corelauncher.ui.controller.page;

import com.laeben.corelauncher.CoreLauncher;
import com.laeben.corelauncher.LauncherConfig;
import com.laeben.corelauncher.api.ui.entity.Announcement;
import com.laeben.corelauncher.api.util.OSUtil;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.Profiler;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.Account;
import com.laeben.corelauncher.api.entity.Config;
import com.laeben.corelauncher.ui.controller.HandlerController;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.ui.control.CButton;
import com.laeben.corelauncher.ui.control.CMsgBox;
import com.laeben.corelauncher.ui.control.CTab;
import com.laeben.corelauncher.ui.control.CWorker;
import com.laeben.corelauncher.ui.util.ControlUtil;
import com.laeben.corelauncher.api.ui.UI;
import com.laeben.corelauncher.util.ImageCacheManager;
import com.laeben.corelauncher.util.JavaManager;
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
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.util.StringConverter;

public class SettingsPage extends HandlerController {


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

    private CButton btnSave;

    private final SpinnerValueFactory.IntegerSpinnerValueFactory fMinRAM;
    private final SpinnerValueFactory.IntegerSpinnerValueFactory fMaxRAM;
    private final ObservableList<String> languages;
    private final ObservableList<String> javas;

    private boolean changeAccount;

    private void tryToEnableSave(){
        if (btnSave != null && !btnSave.isVisible())
            btnSave.setVisible(true);
    }

    public SettingsPage(){
        super("settings");
        fMinRAM = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Integer.MAX_VALUE);
        fMaxRAM = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Integer.MAX_VALUE);
        fMinRAM.setAmountToStepBy(512);
        fMaxRAM.setAmountToStepBy(512);

        fMinRAM.valueProperty().addListener(a -> tryToEnableSave());
        fMaxRAM.valueProperty().addListener(a -> tryToEnableSave());

        languages = FXCollections.observableList(Translator.getTranslator().getAllLanguages().stream().map(x -> x.getDisplayLanguage(x)).toList());
        javas = FXCollections.observableArrayList();
        reloadJava();

        registerHandler(Configurator.getConfigurator().getHandler(), x -> {
            if (x.getKey().equals("gamePathChange")){
                var path = (Path)x.getNewValue();
                txtGamePath.setText(path.toString());
            }
            else if (x.getKey().equals("bgChange")){
                var path = (Path)x.getNewValue();
                txtCustomBackground.setText(path == null ? null : path.toString());
            }
            else if (x.getKey().equals("languageChange"))
                UI.getUI().reset();
        }, true);

        registerHandler(JavaManager.getManager().getHandler(), a -> {
            switch (a.getKey()){
                case "add" -> {
                    var java = (Java)a.getNewValue();
                    javas.add(java.toIdentifier());
                }
                case "update" -> reloadJava();
                case "delete" -> {
                    var java = (Java)a.getOldValue();
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
        txtAccount.textProperty().addListener(a -> tryToEnableSave());

        sldRAM.setLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Double aDouble) {
                return (aDouble / 1024) + "GB";
            }

            @Override
            public Double fromString(String s) {
                String[] f = s.split(" ");
                return Double.parseDouble(f[0]) * 1024;
            }
        });
        sldRAM.valueProperty().addListener(x -> {
            var v = Math.floor(sldRAM.getValue() / 512.0) * 512;

            sldRAM.valueProperty().set(v);

            fMaxRAM.setValue((int) sldRAM.getValue());
        });

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

        txtCustomBackground.setCursor(Cursor.HAND);
        txtCustomBackground.setOnMouseClicked(x -> {
            if (x.getButton() == MouseButton.PRIMARY){
                var path = Configurator.getConfig().getBackgroundImage();
                if (path != null)
                    OSUtil.openFolder(path.parent().toFile().toPath());
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

        btnJavaMan.setOnMouseClicked((a) -> Main.getMain().addTab("pages/java", Translator.translate("frame.title.javaman"), true, JavaPage.class));

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

        chkDebugLogMode.selectedProperty().addListener(x -> {
            Configurator.getConfig().setDebugLogMode(chkDebugLogMode.isSelected());
            Configurator.save();
        });

        chkGamelog.selectedProperty().addListener(x -> {
            Configurator.getConfig().setDelGameLogs(chkGamelog.isSelected());
            Configurator.save();
        });

        btnSelectGamePath.setOnMouseClicked(x -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setInitialDirectory(Configurator.getConfig().getGamePath().toFile());
            var file = chooser.showDialog(btnSelectGamePath.getScene().getWindow());
            if (file == null)
                return;

            try{
                var path = Path.begin(file.toPath());
                var oldPath = Configurator.getConfig().getGamePath();

                Configurator.getConfigurator().setGamePath(path);
                var op = CMsgBox.msg(Alert.AlertType.CONFIRMATION, Translator.translate("ask.ask"), Translator.translate("settings.ask.gamePath")).setButtons(CMsgBox.ResultType.YES, CMsgBox.ResultType.NO).executeForResult();

                if (op.isPresent() && op.get().result() == CMsgBox.ResultType.YES)
                    Profiler.getProfiler().moveProfiles(oldPath);

                Profiler.getProfiler().reload();
            }
            catch (Exception e){
                Logger.getLogger().log(e);
            }
        });

        txtGamePath.setCursor(Cursor.HAND);
        txtGamePath.setOnMouseClicked(a -> OSUtil.openFolder(Configurator.getConfig().getGamePath().toFile().toPath()));

        txtMaxRAM.setValueFactory(fMaxRAM);
        txtMinRAM.setValueFactory(fMinRAM);

        txtMaxRAM.valueProperty().addListener(a -> {
            if (fMaxRAM.getValue() > 32 * 1024)
                return;
            sldRAM.setValue(fMaxRAM.getValue());
        });

        txtAccount.setOnKeyPressed(a -> changeAccount = true);

        chkOnline.selectedProperty().addListener(a -> Configurator.getConfigurator().setDefaultAccount(Account.fromUsername(txtAccount.getText() == null || txtAccount.getText().isBlank() ? "IAMUSER" : txtAccount.getText()).setOnline(chkOnline.isSelected())));

        txtMaxRAM.addEventFilter(ScrollEvent.ANY, ControlUtil::scroller);
        txtMinRAM.addEventFilter(ScrollEvent.ANY, ControlUtil::scroller);

        cbJava.setItems(javas);
        cbLanguage.setItems(languages);

        btnCheckUpdate.setText(Translator.translate("settings.checkup"));
        btnCheckUpdate.setOnMouseClicked(a -> CoreLauncher.updateCheck());
        lblVersion.setText(Double.toString(LauncherConfig.VERSION));

        reload();
    }

    @Override
    public void onParentSet(Object n){
        var tab = (CTab)n;

        var c = Main.getExternalLayer(tab);

        var btnReset = new CButton();
        btnReset.getStyleClass().add("circle-button");
        btnReset.setText(Translator.translate("option.reset"));
        btnReset.setOnMouseClicked(a -> {
            var opt = CMsgBox.msg(Alert.AlertType.CONFIRMATION, Translator.translate("ask.ask"), Translator.translate("ask.sure"))
                    .setButtons(CMsgBox.ResultType.YES, CMsgBox.ResultType.NO)
                    .executeForResult();

            if (opt.isEmpty() || opt.get().result() != CMsgBox.ResultType.YES)
                return;

            Configurator.getConfigurator().reset();
            close();
        });

        AnchorPane.setBottomAnchor(btnReset, 20.0);
        AnchorPane.setRightAnchor(btnReset, 10.0);

        c.getChildren().add(btnReset);

        btnSave = new CButton();
        btnSave.getStyleClass().add("circle-button");
        btnSave.setText(Translator.translate("option.save"));
        btnSave.setVisible(false);
        btnSave.setOnMouseClicked(x -> {
            if (changeAccount){
                Configurator.getConfigurator().setDefaultAccount(Account.fromUsername(txtAccount.getText() == null || txtAccount.getText().isBlank() ? "IAMUSER" : txtAccount.getText()).setOnline(chkOnline.isSelected()));
            }

            Configurator.getConfig().setDefaultMinRAM(fMinRAM.getValue());
            Configurator.getConfig().setDefaultMaxRAM(fMaxRAM.getValue());
            Configurator.save();

            btnSave.setVisible(false);
        });

        AnchorPane.setBottomAnchor(btnSave, 20.0);
        AnchorPane.setRightAnchor(btnSave, 100.0);

        c.getChildren().add(btnSave);

        tab.setContent(c);
    }

    private void reload(){
        try{
            Config c = Configurator.getConfig();

            if (c.getDefaultJava() != null)
                cbJava.setValue(c.getDefaultJava().toIdentifier());
            else
                cbJava.setValue("...");

            if (c.getUser() != null){
                txtAccount.setText(c.getUser().getUsername());
                chkOnline.setSelected(c.getUser().isOnline());
            }
            else{
                txtAccount.setText(null);
                chkOnline.setSelected(false);
            }

            fMinRAM.setValue(c.getDefaultMinRAM());
            fMaxRAM.setValue(c.getDefaultMaxRAM());
            sldRAM.setValue(c.getDefaultMaxRAM());
            chkOldReleases.setSelected(c.isShowOldReleases());
            chkShowSnaps.setSelected(c.isShowSnapshots());
            chkPlaceDock.setSelected(c.shouldPlaceNewProfileToDock());
            txtGamePath.setText(c.getGamePath().toString());
            cbLanguage.setValue(c.getLanguage().getDisplayLanguage(c.getLanguage()));
            chkLogMode.setSelected(c.getLogMode());
            chkHideAfter.setSelected(c.hideAfter());
            chkAutoUpdate.setSelected(c.isEnabledAutoUpdate());
            chkDebugLogMode.setSelected(c.getDebugLogMode());
            chkGamelog.setSelected(c.delGameLogs());

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

    }
}
