package com.laeben.corelauncher.ui.controller.page;

import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.ui.UI;
import com.laeben.corelauncher.api.util.OSUtil;
import com.laeben.corelauncher.ui.control.CButton;
import com.laeben.corelauncher.ui.control.CField;
import com.laeben.corelauncher.ui.control.CList;
import com.laeben.corelauncher.ui.control.CMsgBox;
import com.laeben.corelauncher.ui.controller.HandlerController;
import com.laeben.corelauncher.ui.controller.cell.CECell;
import com.laeben.corelauncher.util.EventHandler;
import com.laeben.corelauncher.wrap.ExtensionWrapper;
import com.laeben.corelauncher.wrap.entity.Extension;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;

import java.util.Locale;

public class ExtensionsPage extends HandlerController {
    public static final String KEY = "pgext";

    private boolean pendingRestart = false;

    public ExtensionsPage() {
        super(KEY);

        registerHandler(ExtensionWrapper.getWrapper().getHandler(), a -> {
            if (a.getKey().equals(ExtensionWrapper.EXTENSION_ADD))
                pList.getItems().add((Extension) a.getSource());
            else if (a.getKey().equals(ExtensionWrapper.EXTENSION_REMOVE)){
                pList.getItems().remove((Extension) a.getSource());
                if (!pendingRestart){
                    var res = CMsgBox.msg(Alert.AlertType.WARNING, Translator.translate("ask.warn"), Translator.translate("extensions.restart"))
                            .setButtons(CMsgBox.ResultType.YES, CMsgBox.ResultType.NO)
                            .executeForResult();
                    if (res.isEmpty())
                        return;
                    if (res.get().result() == CMsgBox.ResultType.YES)
                        UI.shutdown();
                    pendingRestart = true;
                }
            }
            else if (a.getKey().equals(EventHandler.RELOAD))
                reload();
        }, true);
    }

    @FXML
    private CField txtSearch;
    @FXML
    private CList<Extension> pList;
    @FXML
    private CButton btnOpenFolder;

    public void reload(){
        pList.getItems().setAll(ExtensionWrapper.getWrapper().getLoadedExtensions());
    }

    @Override
    public void preInit() {
        pList.setCellFactory(CECell::new);
        pList.setFilterFactory(a -> a.input().getName().toLowerCase(Locale.US).contains(a.query().toLowerCase(Locale.US)));
        pList.setSelectionEnabled(true);

        reload();

        txtSearch.textProperty().addListener(a -> pList.filter(txtSearch.getText()));

        btnOpenFolder.setOnMouseClicked(a -> OSUtil.openFolder(ExtensionWrapper.getWrapper().getPath().toFile().toPath()));
    }
}
