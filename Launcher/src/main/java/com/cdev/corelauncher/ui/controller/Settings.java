package com.cdev.corelauncher.ui.controller;

import com.cdev.corelauncher.data.Configurator;
import com.cdev.corelauncher.data.Profiler;
import com.cdev.corelauncher.data.Translator;
import com.cdev.corelauncher.data.entities.Config;
import com.cdev.corelauncher.ui.controls.CButton;
import com.cdev.corelauncher.ui.controls.CMsgBox;
import com.cdev.corelauncher.ui.utils.ControlUtils;
import com.cdev.corelauncher.ui.utils.FXManager;
import com.cdev.corelauncher.utils.JavaMan;
import com.cdev.corelauncher.utils.Logger;
import com.cdev.corelauncher.utils.entities.Java;
import com.cdev.corelauncher.utils.entities.Path;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.util.StringConverter;

import java.util.stream.Collectors;

public class Settings {

    @FXML
    public RadioButton rbPurple;
    @FXML
    public RadioButton rbDark;
    @FXML
    public RadioButton rbBlue;
    @FXML
    public TextField txtCustomBackground;
    @FXML
    public CButton btnSelectBackground;
    @FXML
    public ChoiceBox<String> cbLanguage;
    @FXML
    public TextField txtGamePath;
    @FXML
    public CButton btnSelectGamePath;
    @FXML
    public ChoiceBox<String> cbAccount;
    @FXML
    public CButton btnAccounts;
    @FXML
    public ChoiceBox<String> cbJava;
    @FXML
    public CButton btnJavaMan;
    @FXML
    public Spinner<Integer> txtMinRAM;
    @FXML
    public Spinner<Integer> txtMaxRAM;
    @FXML
    public Slider sldRAM;
    @FXML
    public CheckBox chkOldReleases;
    @FXML
    public CheckBox chkShowSnaps;
    public CheckBox chkLogMode;
    @FXML
    public CButton btnSaveRAM;

    private final ToggleGroup tgTheme;
    private final SpinnerValueFactory.IntegerSpinnerValueFactory fMinRAM;
    private final SpinnerValueFactory.IntegerSpinnerValueFactory fMaxRAM;
    private final ObservableList<String> languages;
    private final ObservableList<String> javas;
    private final ObservableList<String> accounts;

    public Settings(){
        tgTheme = new ToggleGroup();

        fMinRAM = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Integer.MAX_VALUE);
        fMaxRAM = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Integer.MAX_VALUE);
        fMinRAM.setAmountToStepBy(512);
        fMaxRAM.setAmountToStepBy(512);

        languages = FXCollections.observableList(Translator.getTranslator().getAllLanguages().stream().map(x -> x.getDisplayLanguage(x)).toList());
        javas = FXCollections.observableList(JavaMan.getManager().getAllJavaVersions().stream().map(Java::toIdentifier).collect(Collectors.toList()));
        javas.add(0, "...");
        accounts = FXCollections.observableArrayList();

        Configurator.getConfigurator().getHandler().addHandler("settings", x -> {
            if (x.getKey().equals("gamePathChange")){
                var path = (Path)x.getNewValue();
                txtGamePath.setText(path.toString());
            }
        });

        JavaMan.getManager().getHandler().addHandler("settings", (a) -> {
            switch (a.getKey()){
                case "addJava" -> {
                    var java = (Java)a.getNewValue();
                    javas.add(java.toIdentifier());
                }
                case "delJava" -> {
                    var java = (Java)a.getOldValue();
                    javas.remove(java.toIdentifier());
                    if (java.toIdentifier().equals(cbJava.getValue()))
                        cbJava.setValue("...");
                }
            }
        });
    }

    @FXML
    public void initialize(){
        rbPurple.setToggleGroup(tgTheme);
        rbDark.setToggleGroup(tgTheme);
        rbBlue.setToggleGroup(tgTheme);

        sldRAM.setLabelFormatter(new StringConverter<Double>() {
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
            String val = cbLanguage.getValue();
            var all = Translator.getTranslator().getAllLanguages();
            var l = all.stream().filter(y -> y.getDisplayLanguage(y).equals(val)).findFirst().orElse(null);
            if (l == null || l.equals(Configurator.getConfig().getLanguage()))
                return;

            Translator.getTranslator().setLanguage(l);
            Configurator.getConfigurator().setLanguage(l);
            FXManager.getManager().restart();
        });

        cbJava.valueProperty().addListener(x -> {
            var java = tryGetSelectedJava();

            Configurator.getConfig().setDefaultJava(java);
            Configurator.save();
        });

        btnJavaMan.setOnMouseClicked((a) -> JavaManager.openManager());

        btnSaveRAM.setOnMouseClicked(x -> {
            Configurator.getConfig().setDefaultMinRAM(fMinRAM.getValue());
            Configurator.getConfig().setDefaultMaxRAM(fMaxRAM.getValue());
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

        btnSelectGamePath.setOnMouseClicked(x -> {
            DirectoryChooser chooser = new DirectoryChooser();
            var file = chooser.showDialog(btnSelectGamePath.getScene().getWindow());
            if (file == null)
                return;

            var path = Path.begin(file.toPath());
            var oldPath = Configurator.getConfig().getGamePath();

            Configurator.getConfigurator().setGamePath(path);
            var op = CMsgBox.msg(Alert.AlertType.CONFIRMATION, Translator.translate("ask.ask"), Translator.translate("settings.ask.gamePath")).setButtons(ButtonType.YES, ButtonType.CANCEL).showAndWait();

            if (op.isPresent() && op.get() == ButtonType.YES)
                Profiler.getProfiler().moveProfiles(oldPath);

            Profiler.getProfiler().reload();
        });

        txtMaxRAM.setValueFactory(fMaxRAM);
        txtMinRAM.setValueFactory(fMinRAM);

        txtMaxRAM.setOnScroll(ControlUtils::scroller);
        txtMinRAM.setOnScroll(ControlUtils::scroller);

        cbAccount.setItems(accounts);
        cbJava.setItems(javas);
        cbLanguage.setItems(languages);

        reload();
    }

    private void reload(){
        try{
            Config c = Configurator.getConfig();

            if (c.getDefaultJava() != null)
                cbJava.setValue(c.getDefaultJava().getName() + " - " + c.getDefaultJava().majorVersion);
            else
                cbJava.setValue("...");

            if (c.getUser() != null)
                cbAccount.setValue(c.getUser().getUsername());

            fMinRAM.setValue(c.getDefaultMinRAM());
            fMaxRAM.setValue(c.getDefaultMaxRAM());
            sldRAM.setValue(c.getDefaultMaxRAM());
            chkOldReleases.setSelected(c.isShowOldReleases());
            chkShowSnaps.setSelected(c.isShowSnapshots());
            txtGamePath.setText(c.getGamePath().toString());
            cbLanguage.setValue(c.getLanguage().getDisplayLanguage(c.getLanguage()));
            chkLogMode.setSelected(c.getLogMode());

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
        return JavaMan.getManager().tryGet(new Java(name));
    }
}
