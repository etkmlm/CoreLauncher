package com.laeben.corelauncher.ui.controller.page;

import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.core.util.events.ChangeEvent;
import com.laeben.corelauncher.api.entity.Java;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.ui.controller.HandlerController;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.ui.controller.cell.CJava;
import com.laeben.corelauncher.ui.control.CButton;
import com.laeben.corelauncher.ui.control.CField;
import com.laeben.corelauncher.ui.control.CList;
import com.laeben.corelauncher.ui.control.CMsgBox;
import com.laeben.corelauncher.ui.dialog.DJavaSelector;
import com.laeben.corelauncher.ui.entity.EventFilter;
import com.laeben.corelauncher.util.java.JavaManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Duration;

import java.util.Locale;

public class JavaPage extends HandlerController {
    public static final String KEY = "pgjava";


    public JavaPage() {
        super(KEY);

        registerHandler(JavaManager.getManager().getHandler(), a -> {
            if (!(a instanceof ChangeEvent ce))
                return;
            switch (a.getKey()){
                case JavaManager.ADD -> {
                    var f = (Java) ce.getNewValue();
                    pList.getItems().add(f);
                }
                case JavaManager.DELETE -> pList.getItems().remove((Java) ce.getOldValue());
                //case "update" -> pList.reload(false);
            }
        }, true);
    }

    @FXML
    private CField txtSearch;
    @FXML
    private CList<Java> pList;
    @FXML
    private CButton btnAdd;

    @Override
    public void preInit() {
        pList.setFilterFactory(a -> a.input().getName().toLowerCase(Locale.getDefault()).contains(a.query().toLowerCase(Locale.getDefault())));
        pList.setCellFactory(CJava::new);
        pList.setSelectionEnabled(false);

        txtSearch.setFocusedAnimation(Duration.millis(200));
        txtSearch.textProperty().addListener(a -> pList.filter(txtSearch.getText()));

        btnAdd.setOnMouseClicked(a -> {
            var r = new DJavaSelector().action();
            if (r.isEmpty())
                return;
            var j = r.get();
            if (j.isLocal()){
                JavaManager.getManager().addCustomJava(j.local());
            }
            else {
                new Thread(() -> {
                    try {
                        JavaManager.getManager().downloadAndInclude(null, j.info());
                    }
                    catch (NoConnectionException | StopException e){
                        Main.getMain().announceLater(e, Duration.seconds(2));
                    }
                }).start();
            }
        });

        pList.getItems().setAll(JavaManager.getManager().getAllJavaVersions());
        pList.load();
    }

    private void deleteSelectedJavaEntities(){
        var h = CMsgBox.msg(Alert.AlertType.CONFIRMATION, Translator.translate("ask.ask"), Translator.translate("ask.sure"))
                .setButtons(CMsgBox.ResultType.YES, CMsgBox.ResultType.NO).executeForResult();
        if (!(h.isPresent() && h.get().result() == CMsgBox.ResultType.YES))
            return;

        pList.getSelectedItems().forEach(x -> JavaManager.getManager().deleteJava(x));

        pList.getItems().removeAll(pList.getSelectedItems());
    }

    @Override
    public void init(){
        addRegisteredEventFilter(EventFilter.window(getStage(), KeyEvent.ANY, e -> {
            if (e.getTarget().equals(txtSearch))
                return;

            if (pList.onKeyEvent(e))
                return;

            if (e.getCode() == KeyCode.DELETE)
                deleteSelectedJavaEntities();
        }));

        txtSearch.setOnKeyPressed(a -> {
            if (a.getCode() == KeyCode.ESCAPE)
                rootNode.requestFocus();
        });
    }
}
