package com.laeben.corelauncher.ui.controller;

import com.laeben.corelauncher.data.Configurator;
import com.laeben.corelauncher.data.Profiler;
import com.laeben.corelauncher.data.Translator;
import com.laeben.corelauncher.data.entities.Account;
import com.laeben.corelauncher.data.entities.Profile;
import com.laeben.corelauncher.minecraft.Wrapper;
import com.laeben.corelauncher.minecraft.wrappers.Custom;
import com.laeben.corelauncher.minecraft.wrappers.Vanilla;
import com.laeben.corelauncher.minecraft.wrappers.entities.WrapperVersion;
import com.laeben.corelauncher.ui.entities.LStage;
import com.laeben.corelauncher.ui.utils.ControlUtils;
import com.laeben.corelauncher.ui.utils.FXManager;
import com.laeben.corelauncher.utils.JavaMan;
import com.laeben.corelauncher.utils.Logger;
import com.laeben.corelauncher.utils.OSUtils;
import com.laeben.corelauncher.utils.StringUtils;
import com.laeben.corelauncher.utils.entities.Java;
import com.laeben.core.entity.Path;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.util.StringConverter;
import org.controlsfx.control.SearchableComboBox;

import java.util.stream.Collectors;

public class ProfileEdit {
    @FXML
    private TextField txtName;

    @FXML
    private RadioButton vanilla;
    @FXML
    private RadioButton forge;
    @FXML
    private RadioButton fabric;
    @FXML
    private RadioButton quilt;
    @FXML
    private RadioButton optifine;
    @FXML
    private RadioButton custom;

    @FXML
    private ChoiceBox<String> cbWrapperVersion;
    @FXML
    private SearchableComboBox<String> cbGameVersion;
    @FXML
    private ChoiceBox<String> cbJavaVersion;
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
    private AnchorPane root;
    @FXML
    private GridPane pWrapper;
    @FXML
    private TextField txtWrapper;
    @FXML
    private Button btnSelectWrapper;

    private final ToggleGroup wrapperGroup;
    private final ObservableList<String> versions;
    private final ObservableList<String> javaVersions;
    private final ObservableList<String> wrapperVersions;
    private final SpinnerValueFactory.IntegerSpinnerValueFactory fMinRAM;
    private final SpinnerValueFactory.IntegerSpinnerValueFactory fMaxRAM;
    private Profile profile;
    private Profile tempProfile;

    public ProfileEdit setProfile(Profile p){
        profile = p;
        reload();
        return this;
    }

    public static ProfileEdit open(Profile p){
        var stage = FXManager.getManager()
                .applyStage("pedit");

        var pEdit = ((ProfileEdit) stage.getLScene().getController()).setProfile(p);

        stage.show();

        return pEdit;
    }

    public ProfileEdit(){
        wrapperGroup = new ToggleGroup();
        fMinRAM = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Integer.MAX_VALUE);
        fMaxRAM = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Integer.MAX_VALUE);
        fMinRAM.setAmountToStepBy(512);
        fMaxRAM.setAmountToStepBy(512);

        var showOld = Configurator.getConfig().isShowOldReleases();
        var showSnap = Configurator.getConfig().isShowSnapshots();
        javaVersions = FXCollections.observableList(JavaMan.getManager().getAllJavaVersions().stream().map(Java::toIdentifier).collect(Collectors.toList()));
        javaVersions.add(0, "...");
        versions = FXCollections.observableList(Vanilla.getVanilla().getAllVersions().stream().filter(x -> x.type == null || (x.type.equals("snapshot") && showSnap) || ((x.type.equals("old_beta") || x.type.equals("old_alpha")) && showOld) || x.type.equals("release")).map(x -> x.id).toList());
        wrapperVersions = FXCollections.observableArrayList();
        wrapperVersions.add("...");

        JavaMan.getManager().getHandler().addHandler("pedit", (a) -> {
            switch (a.getKey()){
                case "addJava" -> {
                    var java = (Java)a.getNewValue();
                    javaVersions.add(java.toIdentifier());
                }
                case "delJava" -> {
                    var java = (Java)a.getOldValue();
                    javaVersions.remove(java.toIdentifier());
                    if (java.toIdentifier().equals(cbJavaVersion.getValue()))
                        cbJavaVersion.setValue("...");
                }
            }
        });
    }

    @FXML
    public void initialize(){
        cbGameVersion.valueProperty().addListener(x -> {
            String value = cbGameVersion.getValue();

            if (value == null || value.isEmpty())
                return;

            tempProfile.setVersionId(value);

            refreshWrapperVersions();
        });
        cbGameVersion.setItems(versions);

        cbJavaVersion.valueProperty().addListener(x -> {
            var java = tryGetSelectedJava();

            tempProfile.setJava(java);
        });
        cbJavaVersion.setItems(javaVersions);

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

            //fMinRAM.setValue((int) sldRAM.getValue());
            fMaxRAM.setValue((int) sldRAM.getValue());
        });

        btnJavaManager.setOnMouseClicked((a) -> JavaManager.openManager());

        vanilla.setToggleGroup(wrapperGroup);
        forge.setToggleGroup(wrapperGroup);
        fabric.setToggleGroup(wrapperGroup);
        quilt.setToggleGroup(wrapperGroup);
        optifine.setToggleGroup(wrapperGroup);
        custom.setToggleGroup(wrapperGroup);
        wrapperGroup.selectToggle(vanilla);

        wrapperGroup.selectedToggleProperty().addListener(a -> {
            var wrapper = Wrapper.getWrapper(((RadioButton)wrapperGroup.getSelectedToggle()).getId());
            tempProfile.setWrapper(wrapper);
            if (cbGameVersion.getValue() != null && !cbGameVersion.getValue().isEmpty())
                refreshWrapperVersions();
        });

        cbWrapperVersion.setItems(wrapperVersions);
        cbWrapperVersion.valueProperty().addListener(a -> {
            if (tempProfile.getVersionId() == null)
                return;

            String value = cbWrapperVersion.getValue();

            if (value == null || value.isEmpty())
                return;

            tempProfile.setWrapperVersion(value);
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
                OSUtils.openFolder(path.toFile().toPath());
        });

        txtMaxRAM.setValueFactory(fMaxRAM);
        txtMinRAM.setValueFactory(fMinRAM);

        txtMaxRAM.valueProperty().addListener(a -> {
            if (fMaxRAM.getValue() > 32 * 1024)
                return;
            sldRAM.setValue(fMaxRAM.getValue());
        });

        txtMaxRAM.setOnScroll(ControlUtils::scroller);
        txtMinRAM.setOnScroll(ControlUtils::scroller);

        btnSave.setOnMouseClicked(a -> {
            String name = StringUtils.pure(txtName.getText());

            if (name == null || name.isEmpty() || name.isBlank())
                return;

            if (name.endsWith("."))
                name = StringUtils.trimEnd(name, '.');

            tempProfile
                    .rename(name)
                    .setJvmArgs(txtArgs.getText().split(" "))
                    .setMinRAM(fMinRAM.getValue())
                    .setMaxRAM(fMaxRAM.getValue());
            if (txtAccount.getText() == null || txtAccount.getText().isEmpty() || txtAccount.getText().isBlank())
                tempProfile.setCustomUser(null);
            else
                tempProfile.setCustomUser(Account.fromUsername(txtAccount.getText()).setOnline(chkAccOnline.isSelected()));

            if ((tempProfile.getWrapper() != null && !(tempProfile.getWrapper() instanceof Vanilla) && (tempProfile.getWrapperVersion() == null || tempProfile.getWrapperVersion().isEmpty() || tempProfile.getWrapperVersion().isBlank() || tempProfile.getWrapperVersion().equals("..."))))
                return;

            try{
                if (profile == null){
                    var p = Profiler.getProfiler().createAndSetProfile(txtName.getText(), b -> b.cloneFrom(tempProfile));
                    setProfile(p);
                }
                else {
                    Profiler.getProfiler().setProfile(profile.getName(), b -> b.cloneFrom(tempProfile));
                }
            }
            catch (Exception e){
                Logger.getLogger().log(e);
            }

            FXManager.getManager().closeStage((LStage)btnSave.getScene().getWindow());
        });

        reload();
    }

    private Java tryGetSelectedJava(){
        String text = cbJavaVersion.getValue();
        if (text.equals("..."))
            return null;
        String name = text.split(" - ")[0];
        return JavaMan.getManager().tryGet(new Java(name));
    }

    private void refreshWrapperVersions(){
        wrapperVersions.clear();
        wrapperVersions.add("...");

        String versionId = tempProfile.getVersionId();
        var wr = tempProfile.getWrapper();
        if (wr instanceof Custom){
            pWrapper.setVisible(true);
            cbWrapperVersion.setVisible(false);
        }
        else{
            pWrapper.setVisible(false);
            cbWrapperVersion.setVisible(true);
            if (!(wr instanceof Vanilla))
                wrapperVersions.addAll(tempProfile.getWrapper().getVersions(versionId).stream().map(x -> ((WrapperVersion)x).getWrapperVersion()).toList());
        }
    }

    private void reload(){
        if (profile == null)
            tempProfile = Profile.empty();
        else
            tempProfile = Profile.empty().cloneFrom(profile);

        try{
            txtName.setText(tempProfile.getName());
            cbGameVersion.setValue(tempProfile.getVersionId());
            var j = tempProfile.getJava();
            if (j != null)
                cbJavaVersion.setValue(j.toIdentifier());
            else
                cbJavaVersion.setValue("...");
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
            var wrapperToggle = wrapperGroup.getToggles().stream().filter(x -> ((RadioButton)x).getId().equals(wr.getIdentifier())).findFirst().orElse(vanilla);
            wrapperGroup.selectToggle(wrapperToggle);

            refreshWrapperVersions();
            if (wr instanceof Custom c){
                txtWrapper.setText(c.getPath(tempProfile.getWrapperVersion()).toString());
            }
            else if (!(wr instanceof Vanilla) && tempProfile.getWrapperVersion() != null){
                cbWrapperVersion.setValue(tempProfile.getWrapperVersion());
            }
            fMinRAM.setValue(tempProfile.getMinRAM());
            fMaxRAM.setValue(tempProfile.getMaxRAM());
            sldRAM.setValue(tempProfile.getMaxRAM());
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }
    }
}
