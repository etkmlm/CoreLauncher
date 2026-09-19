package com.laeben.corelauncher.ui.controller.java.cell;

import com.laeben.core.entity.Path;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.core.event.context.EventContext;
import com.laeben.core.event.function.ProgressFunction;
import com.laeben.core.network.event.NetworkProgressContext;
import com.laeben.corelauncher.api.concurrency.TaskRecord;
import com.laeben.corelauncher.api.concurrency.Tasker;
import com.laeben.corelauncher.api.concurrency.property.TaskRecordProperty;
import com.laeben.corelauncher.api.entity.Java;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.ui.UI;
import com.laeben.corelauncher.api.util.OSUtil;
import com.laeben.corelauncher.ui.control.CButton;
import com.laeben.corelauncher.ui.control.CField;
import com.laeben.corelauncher.ui.control.CProgressBorder;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.ui.controller.cell.CVirtualCell;
import com.laeben.corelauncher.ui.util.DisplayUtil;
import com.laeben.corelauncher.util.java.JavaManager;
import com.laeben.corelauncher.util.java.entity.JavaDownloadInfo;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;

public class JavaCell extends CVirtualCell<JavaCell.Item> {
    private static final String INSTALLING_CLASS = "installing";

    public static class Item {
        private final Tasker tasker;
        private final TaskRecordProperty downloadTask;
        private final ObjectProperty<Java> java;
        private final ObjectProperty<JavaDownloadInfo> downloadInfo;

        private ProgressFunction onProgress;

        public Item(Java java, Tasker tasker){
            this.tasker = tasker;
            this.java = new SimpleObjectProperty<>(java);
            this.downloadTask = new TaskRecordProperty();
            this.downloadInfo = new SimpleObjectProperty<>();
            this.onProgress = null;
        }

        public Item(Tasker tasker){
            this(null, tasker);
        }

        public boolean isEmpty() {
            return getJava() == null;
        }

        public Java getJava(){
            return java.get();
        }

        public String getName(){
            return getJava() == null ? (downloadInfo.get() == null ? "" : downloadInfo.get().displayName()) : getJava().getName();
        }

        public void cancel(){
            downloadTask.cancelIfRunning();
        }

        public void handleJavaDownload(JavaDownloadInfo info){
            if (downloadTask.isRunning()) return;

            this.downloadInfo.set(info);

            downloadTask.set(tasker.await(() -> {
                try {
                    java.set(JavaManager.getManager().downloadAndInclude(null, info, onProgress));
                    this.downloadInfo.set(null);
                }
                catch (StopException ignored){}
                catch (NoConnectionException e){
                    Main.getMain().announceLater(e, Duration.seconds(2));
                }
            }));
        }
    }

    private final BooleanProperty editMode;
    private final ChangeListener<JavaDownloadInfo> downInfoListener;
    private final ChangeListener<TaskRecord> downTaskListener;
    private final ChangeListener<Java> javaListener;

    public JavaCell() {
        super("layout/cells/java.fxml");

        editMode = new SimpleBooleanProperty();
        editMode.addListener(a -> {
            if (editMode.get()){
                txtName.setEditable(true);
                txtName.setCursor(Cursor.TEXT);
            }
            else{
                txtName.setEditable(false);
                txtName.setCursor(Cursor.DEFAULT);
                getRoot().requestFocus();
                try {
                    if (JavaManager.getManager().renameCustomJava(getItem().getJava(), txtName.getText()))
                        txtName.setText(getItem().getJava().getName());
                } catch (StopException ignored) {

                } catch (IOException e) {
                    Logger.getLogger().log(e);
                }
            }
        });

        downInfoListener = (observable, oldValue, newValue) -> {
            if (newValue != null)
                UI.runAsync(() -> lblVersion.setText(String.valueOf(newValue.major())));
        };
        downTaskListener = (ob, o, n) -> UI.runAsync(() -> {
            if (n != null && o == null) btnAction.getStyleClass().add(INSTALLING_CLASS);
            else if (n == null) {
                btnAction.getStyleClass().remove(INSTALLING_CLASS);
                if (getItem() != null && getItem().getJava() == null) destroyItem(); // install was failed
            }
        });
        javaListener = (obs, o, n) -> {
            if (n != null) UI.runAsync(this::rebindItem);
        };

        txtPath.setCursor(Cursor.HAND);

        txtName.setEditable(false);
        txtName.setCursor(Cursor.DEFAULT);
        txtName.focusedProperty().addListener(a -> {
            if (txtName.isFocused() && !editMode.get())
                editMode.set(true);

            if (!txtName.isFocused() && editMode.get())
                editMode.set(false);
        });
        txtName.setOnKeyPressed(a -> {
            if (a.getCode() == KeyCode.ENTER)
                editMode.set(false);
        });
        txtName.setFocusedAnimation(Duration.millis(200));

        txtPath.setOnMouseClicked(a -> {
            if (getItem() != null && getItem().getJava() != null) OSUtil.open(getItem().getJava().getPath().toFile());
        });

        btnAction.setOnMouseClicked((a) -> {
            if (getItem() == null) return;

            if (getItem().getJava() != null) JavaManager.getManager().deleteJava(getItem().getJava());
            else getItem().cancel();
        });
    }

    @FXML
    private Label lblVersion;

    @FXML
    private CField txtName;
    @FXML
    private TextField txtPath;
    @FXML
    private CButton btnAction;
    @FXML
    private CProgressBorder progress;
    @FXML
    private Label lblStatus;
    @FXML
    private VBox infoContainer;

    private void onProgress(long current, long total, EventContext context){
        UI.runAsync(() -> {
            progress.setProgress(current * 1.0 / total);
            if (context instanceof Path.ParentItemContext ctx) lblStatus.setText(ctx.getFile().getName());
            else if (context instanceof NetworkProgressContext) lblStatus.setText(DisplayUtil.parseDownloadProgress(current) + " / " + DisplayUtil.parseDownloadProgress(total));
        });
    }

    @Override
    public void unbindItem(Item item){
        item.onProgress = null;
        lblVersion.textProperty().unbind();
        item.downloadInfo.removeListener(downInfoListener);
        item.java.removeListener(javaListener);
        item.downloadTask.addListener(downTaskListener);
    }

    @Override
    public void bindItem(Item item) {
        editMode.set(false);
        progress.setProgress(0);

        if (!getItem().isEmpty()){
            lblStatus.setVisible(false);
            lblStatus.setManaged(false);
            infoContainer.setVisible(true);
            infoContainer.setManaged(true);

            final var java = item.getJava();

            txtName.setText(java.getName());
            txtPath.setText(java.getPath().toString());

            String cls;
            if (java.majorVersion >= 16)
                cls = "red";
            else if (java.majorVersion >= 11)
                cls = "orange";
            else
                cls = "green";

            lblVersion.getStyleClass().setAll("label", cls);
            lblVersion.setText(String.valueOf(java.majorVersion));
        }
        else{
            lblStatus.setVisible(true);
            lblStatus.setManaged(true);
            infoContainer.setVisible(false);
            infoContainer.setManaged(false);

            lblVersion.getStyleClass().setAll("label");

            downInfoListener.changed(null, null, item.downloadInfo.getValue());
            if (item.downloadTask.get() != null) downTaskListener.changed(null, null, item.downloadTask.get());

            item.downloadInfo.addListener(downInfoListener);
            item.java.addListener(javaListener);
            item.downloadTask.addListener(downTaskListener);

            getItem().onProgress = this::onProgress;
        }
    }
}
