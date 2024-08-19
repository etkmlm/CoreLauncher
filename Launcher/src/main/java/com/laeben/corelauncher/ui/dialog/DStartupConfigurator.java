package com.laeben.corelauncher.ui.dialog;

import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.Account;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.ui.control.CButton;
import com.laeben.corelauncher.ui.control.CCombo;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.*;

import java.util.Locale;

public class DStartupConfigurator extends CDialog<Boolean>{
    @FXML
    private CButton btnClose;
    @FXML
    private CButton btnApply;
    @FXML
    private Label lblLanguage;
    @FXML
    private Label lblTitle;
    @FXML
    private Label lblUsername;
    @FXML
    private CCombo<Locale> cbLanguage;
    @FXML
    private CheckBox chkOnline;
    @FXML
    private TextField txtUsername;

    private Locale selectedLanguage;

    public DStartupConfigurator() {
        super("layout/dialog/startup.fxml", false);

        getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        btnClose.enableTransparentAnimation();
        btnClose.setOnMouseClicked(a -> {
            setResult(true);
            close();
        });

        cbLanguage.setCursor(Cursor.DEFAULT);
        cbLanguage.getItems().setAll(Translator.getTranslator().getAllLanguages());
        cbLanguage.setValueFactory(a -> a.getDisplayLanguage(a));
        cbLanguage.setValue(selectedLanguage = Configurator.getConfig().getLanguage());
        cbLanguage.requestFocus();

        cbLanguage.setOnItemChanged(a -> {
            try{
                selectedLanguage = a;
                Translator.getTranslator().setLanguage(selectedLanguage);
                reloadLanguage();
            }
            catch (Exception e){
                Logger.getLogger().log(e);
            }
        });

        btnApply.enableTransparentAnimation();
        btnApply.setOnMouseClicked(a -> {
            Configurator.getConfigurator().setDefaultAccount(Account.fromUsername(txtUsername.getText() == null || txtUsername.getText().isBlank() ? "IAMUSER" : txtUsername.getText()).setOnline(chkOnline.isSelected()));
            Configurator.save();
            setResult(true);
            close();
            Configurator.getConfigurator().setLanguage(selectedLanguage);
        });
    }

    private void reloadLanguage(){
        lblTitle.setText(Translator.translate("welcome"));
        lblLanguage.setText(Translator.translate("settings.language"));
        lblUsername.setText(Translator.translate("account"));
        chkOnline.setText(Translator.translate("online"));
    }

    public boolean execute(){
        var a = super.action();
        return a.isPresent() && a.get();
    }
}
