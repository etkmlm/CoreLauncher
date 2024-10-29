package com.laeben.corelauncher.ui.dialog;

import com.laeben.core.entity.Path;
import com.laeben.corelauncher.CoreLauncher;
import com.laeben.corelauncher.api.entity.Java;
import com.laeben.corelauncher.api.ui.entity.Announcement;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.ui.control.*;
import com.laeben.corelauncher.util.JavaManager;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;

import java.util.Optional;

public class DJavaSelector extends CDialog<DJavaSelector.Result> {

    public record Result(Java local, JavaManager.JavaDownloadInfo info){
        public boolean isLocal(){
            return local != null;
        }
    }

    private final ToggleGroup group;

    private Java selectedJava;

    @FXML
    private CButton btnClose;
    @FXML
    private CButton btnApply;
    @FXML
    private CButton btnSelect;
    @FXML
    private TextField txtPath;
    @FXML
    private CWorker<JavaManager.JavaDownloadInfo, Void> worker;
    @FXML
    private RadioButton rbLocal;
    @FXML
    private RadioButton rbNetwork;
    @FXML
    private Pane pnLocal;
    @FXML
    private Pane pnNetwork;
    @FXML
    private Label lblName;
    @FXML
    private CField txtName;
    @FXML
    private Spinner<Integer> spnMajor;

    public DJavaSelector(){
        super("layout/dialog/javaselector.fxml", true);

        btnClose.enableTransparentAnimation();
        btnClose.setOnMouseClicked(a -> close(null));

        worker.begin().withTask(a -> new Task<>() {
            @Override
            protected JavaManager.JavaDownloadInfo call() throws Exception {
                return JavaManager.getJavaInfo(Java.fromVersion(spnMajor.getValue()), CoreLauncher.OS_64);
            }
        }).onDone(a -> {
            if (a.getValue() != null)
                close(new Result(null, a.getValue()));
            else{
                Main.getMain().announceLater(Translator.translate("error.oops"), Translator.translate("error.java.found"), Announcement.AnnouncementType.ERROR, Duration.seconds(2));
            }
        }).onFailed(a -> {
            if (!Main.getMain().announceLater(a.getError(), Duration.seconds(2)))
                Logger.getLogger().log(a.getError());
        });

        btnApply.enableTransparentAnimation();
        btnApply.setOnMouseClicked(a -> {
            if (selectedJava != null){
                if (txtName.getText() != null && !txtName.getText().isBlank())
                    selectedJava.setName(txtName.getText());
                close(new Result(selectedJava, null));
                return;
            }

            worker.run();
            //Launcher.getLauncher().getHandler().execute(new KeyEvent("jvdown"));
        });

        txtName.setFocusedAnimation(Color.TEAL, Duration.millis(200));
        txtPath.setCursor(Cursor.DEFAULT);
        btnSelect.setOnMouseClicked(a -> {
            var d = new DirectoryChooser().showDialog(btnSelect.getScene().getWindow());
            if (d == null)
                return;
            var path = Path.begin(d.toPath());
            try{
                selectedJava = new Java(path);
                txtPath.setText(path.toString());
            }
            catch (Exception e){
                Logger.getLogger().log(e);
                txtPath.setText(Translator.translate("error.wrongPath"));
            }
        });


        group = new ToggleGroup();
        rbLocal.setToggleGroup(group);
        rbNetwork.setToggleGroup(group);

        group.selectedToggleProperty().addListener(a -> {
            boolean local = rbLocal.isSelected();

            lblName.setVisible(local);
            txtName.setVisible(local);
            
            node.lookupAll(".group-local").forEach(x -> {
                x.setVisible(local);
                x.setManaged(local);
            });
            node.lookupAll(".group-network").forEach(x -> {
                x.setVisible(!local);
                x.setManaged(!local);
            });
        });

        rbNetwork.setSelected(true);
    }

    private void close(Result j){
        setResult(j);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        close();
    }

    @Override
    public Optional<Result> action(){
        getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.YES);

        return super.action();
    }
}
