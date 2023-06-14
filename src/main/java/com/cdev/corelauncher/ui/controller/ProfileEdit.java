package com.cdev.corelauncher.ui.controller;

import com.cdev.corelauncher.data.Configurator;
import com.cdev.corelauncher.data.Profiler;
import com.cdev.corelauncher.data.entities.Profile;
import com.cdev.corelauncher.minecraft.Launcher;
import com.cdev.corelauncher.ui.utils.ControlUtils;
import com.cdev.corelauncher.ui.utils.FXManager;
import com.cdev.corelauncher.utils.JavaMan;
import com.cdev.corelauncher.utils.Logger;
import com.cdev.corelauncher.utils.entities.Java;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.util.StringConverter;
import org.controlsfx.control.SearchableComboBox;

import java.util.stream.Collectors;

public class ProfileEdit {

    @FXML
    private ImageView img;
    @FXML
    private TextField txtName;

    @FXML
    private RadioButton rbVanilla;
    @FXML
    private RadioButton rbForge;
    @FXML
    private RadioButton rbFabric;

    @FXML
    private ComboBox<String> cbWrapperVersion;
    @FXML
    private SearchableComboBox<String> cbGameVersion;
    /*@FXML
    private TextField txtSearchGameVersion;
    @FXML
    private Label lblGameVersion;*/
    @FXML
    private ChoiceBox<String> cbJavaVersion;
    @FXML
    private ChoiceBox<String> cbAccounts;
    @FXML
    private Button btnJavaManager;
    @FXML
    private Button btnSave;
    @FXML
    private Button btnAccManager;
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

    private final ToggleGroup wrapperGroup;
    private final ObservableList<String> versions;
    private final ObservableList<String> javaVersions;
    private final ObservableList<String> accounts;
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
                .applyStage("profileedit", p == null ? "Add Profile" : "Edit Profile");

        var pEdit = ((ProfileEdit) stage.getLScene().getController()).setProfile(p);

        stage.showStage();

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
        versions = FXCollections.observableList(Launcher.getLauncher().getAllVersions().stream().filter(x -> (x.type.equals("snapshot") && showSnap) || ((x.type.equals("old_beta") || x.type.equals("old_alpha")) && showOld) || x.type.equals("release")).map(x -> x.id).toList());
        javaVersions = FXCollections.observableList(JavaMan.getManager().getAllJavaVersions().stream().map(Java::toIdentifier).collect(Collectors.toList()));
        javaVersions.add(0, "...");

        accounts = FXCollections.observableArrayList();
        accounts.add(0, "...");

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
        ControlUtils.setTextFieldContext(txtArgs);
        ControlUtils.setTextFieldContext(txtName);

        txtMinRAM.setContextMenu(null);
        txtMaxRAM.setContextMenu(null);

        cbGameVersion.valueProperty().addListener(x -> {
            String value = cbGameVersion.getValue();

            if (value == null || value.equals(""))
                return;

            tempProfile.setVersionId(value);
        });
        cbGameVersion.setItems(versions);

        cbJavaVersion.valueProperty().addListener(x -> {
            var java = tryGetSelectedJava();

            tempProfile.setJava(java);
        });
        cbJavaVersion.setItems(javaVersions);

        cbAccounts.setItems(accounts);

        btnAccManager.setOnMouseClicked((a) -> {

        });

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

            //fMinRAM.setValue((int) sldRAM.getValue());
            fMaxRAM.setValue((int) sldRAM.getValue());
        });

        btnJavaManager.setOnMouseClicked((a) -> JavaManager.openManager());

        rbVanilla.setToggleGroup(wrapperGroup);
        rbForge.setToggleGroup(wrapperGroup);
        rbFabric.setToggleGroup(wrapperGroup);
        wrapperGroup.selectToggle(rbVanilla);

        txtMaxRAM.setValueFactory(fMaxRAM);
        txtMinRAM.setValueFactory(fMinRAM);

        txtMaxRAM.setOnScroll(ControlUtils::scroller);
        txtMinRAM.setOnScroll(ControlUtils::scroller);

        btnSave.setOnMouseClicked(a -> {
            tempProfile
                    .rename(txtName.getText())
                    .setJvmArgs(txtArgs.getText().split(" "))
                    .setMinRAM(fMinRAM.getValue())
                    .setMaxRAM(fMaxRAM.getValue());

            if (profile == null){
                Profile p = Profiler.getProfiler().createAndSetProfile(txtName.getText(), b -> b.cloneFrom(tempProfile));
                setProfile(p);
            }
            else {
                Profiler.getProfiler().setProfile(profile.getName(), b -> b.cloneFrom(tempProfile));
            }
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
            if (tempProfile.getUser() != null)
                cbAccounts.setValue(tempProfile.getUser().getUsername());
            else
                cbAccounts.setValue("...");
            if (tempProfile.getJvmArgs() != null)
                txtArgs.setText(String.join(" ", tempProfile.getJvmArgs()));
            fMinRAM.setValue(tempProfile.getMinRAM());
            fMaxRAM.setValue(tempProfile.getMaxRAM());
            sldRAM.setValue(tempProfile.getMaxRAM());
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }
    }
}
