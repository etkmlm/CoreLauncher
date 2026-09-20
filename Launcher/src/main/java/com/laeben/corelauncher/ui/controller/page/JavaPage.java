package com.laeben.corelauncher.ui.controller.page;

import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.concurrency.TaskRecord;
import com.laeben.corelauncher.api.concurrency.Tasker;
import com.laeben.corelauncher.api.entity.Java;
import com.laeben.corelauncher.ui.control.*;
import com.laeben.corelauncher.ui.controller.HandlerController;
import com.laeben.corelauncher.ui.controller.java.cell.JavaCell;
import com.laeben.corelauncher.ui.dialog.DJavaSelector;
import com.laeben.corelauncher.ui.entity.EventFilter;
import com.laeben.corelauncher.util.java.JavaManager;
import com.laeben.corelauncher.util.java.event.JavaContext;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Duration;
import org.apache.commons.lang3.Strings;

public class JavaPage extends HandlerController {
    public static final String KEY = "pgjava";

    private final Tasker tasker;

    public JavaPage() {
        super(KEY);

        tasker = new Tasker();

        registerUIHandler(tasker.getHandler(), a -> {
            final var record = a.<TaskRecord>getSource();
            btnCancel.setVisible(!tasker.isEmpty());
        }, true);

        registerUIHandler(JavaManager.getManager().getHandler(), a -> {
            if (a.inContext(JavaContext.ADD)){
                var f = a.<Java>getSource();
                if (pList.getItems().stream().noneMatch(x -> f.equals(x.getJava()))) pList.getItems().add(new JavaCell.Item(f, tasker));
            }
            else if (a.inContext(JavaContext.DELETE)){
                var f = a.<Java>getSource();
                pList.getItems().removeIf(x -> f.equals(x.getJava()));
            }
        }, true);
    }

    @FXML
    private CField txtSearch;
    @FXML
    private CVirtualList<JavaCell.Item> pList;
    @FXML
    private CButton btnAdd;
    @FXML
    private CButton btnCancel;

    private JavaCell.Item pushNewEmptyCell(){
        var item = new JavaCell.Item(tasker);

        pList.getItems().add(item);

        return item;
    }

    @Override
    public void preInit() {
        pList.setFilterFactory(a -> Strings.CI.contains(a.input().getName(), a.query()));
        pList.setCellFactory(JavaCell::new);
        pList.setSelectionEnabled(false);

        txtSearch.setFocusedAnimation(Duration.millis(200));
        txtSearch.textProperty().addListener(a -> pList.filter(txtSearch.getText()));

        btnAdd.setOnMouseClicked(a -> {
            var r = new DJavaSelector(getStage()).action();
            if (r.isEmpty())
                return;
            var j = r.get();
            if (j.isLocal()){
                JavaManager.getManager().addCustomJava(j.local());
            }
            else {
                pushNewEmptyCell().handleJavaDownload(j.info());
            }
        });
        btnCancel.setOnMouseClicked(a -> tasker.stopAll());

        pList.getItems().setAll(JavaManager.getManager().getAllJavaVersions().stream().map(x -> new JavaCell.Item(x, tasker)).toList());
    }

    private void deleteSelectedJavaEntities(){
        var h = showMsg(Alert.AlertType.CONFIRMATION, Translator.translate("ask.ask"), Translator.translate("ask.sure"))
                .setButtons(CMsgBox.ResultType.YES, CMsgBox.ResultType.NO).executeForResult();
        if (!(h.isPresent() && h.get().result() == CMsgBox.ResultType.YES))
            return;

        pList.getSelectedItems().forEach(x -> {
            if (x.isEmpty()) x.cancel();
            else JavaManager.getManager().deleteJava(x.getJava());
        });

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
