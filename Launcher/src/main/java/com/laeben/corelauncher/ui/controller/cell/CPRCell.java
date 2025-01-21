package com.laeben.corelauncher.ui.controller.cell;

import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.corelauncher.api.ui.entity.Announcement;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.minecraft.modding.Modder;
import com.laeben.corelauncher.minecraft.modding.entity.CResource;
import com.laeben.corelauncher.ui.control.CMsgBox;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.ui.control.CButton;
import com.laeben.corelauncher.ui.control.CView;
import com.laeben.corelauncher.api.ui.UI;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class CPRCell<T extends CResource> extends CCell<T> {

    private T item;

    private final Profile profile;

    public CPRCell(Profile profile){
        super("layout/cells/ccell.fxml");

        this.profile = profile;

        node.setOnMouseEntered(a -> fade.playFromStart());

        node.setOnMouseExited(a -> {
            fade.play();
            fade.jumpTo(Duration.ZERO);
            fade.stop();
        });

        btnRemove = new CButton();
        btnRemove.setText("—");
        btnRemove.setStyle("-fx-background-color: transparent; -fx-font-size: 13pt");
        btnRemove.enableTransparentAnimation();

        btnUpdate = new CButton();
        btnUpdate.setText("⟳");
        btnUpdate.setStyle("-fx-background-color: transparent");
        btnUpdate.enableTransparentAnimation();

        box.getChildren().addAll(btnUpdate, new Rectangle(10, 0), btnRemove);

    }

    @FXML
    private CView image;
    @FXML
    private Label lblName;

    @FXML
    private HBox box;

    private final CButton btnRemove;
    private final CButton btnUpdate;

    @FXML
    public void initialize(){

    }

    @Override
    public CCell setItem(T item) {
        this.item = item;

        image.setImageAsync(item.getIcon(32, 32));

        lblName.setText(item.name);

        lblName.setTooltip(new Tooltip(item.fileName));
        //btnUpdate.setTooltip(new Tooltip(Translator.translate("update")));

        btnUpdate.setOnMouseClicked(a -> new Thread(() -> {
            try {
                UI.runAsync(() -> Main.getMain().getAnnouncer().announce(new Announcement(Translator.getTranslator().getTranslate("announce.info.update.title"), Translator.translateFormat("announce.info.update.search.single", item.name), Announcement.AnnouncementType.INFO), Duration.seconds(2)));
                var upResources = Modder.getModder().getUpdate(profile, item);
                if (upResources != null){
                    Modder.getModder().includeAll(profile, upResources);
                    UI.runAsync(() -> Main.getMain().getAnnouncer().announce(new Announcement(Translator.getTranslator().getTranslate("announce.info.update.title"), Translator.translateFormat("announce.info.update.ok.single", item.name, item.fileName), Announcement.AnnouncementType.INFO), Duration.seconds(2)));
                }
                else{
                    UI.runAsync(() -> Main.getMain().getAnnouncer().announce(new Announcement(Translator.getTranslator().getTranslate("announce.info.update.title"), Translator.translateFormat("announce.info.update.mpcontent", item.name), Announcement.AnnouncementType.ERROR), Duration.seconds(4)));
                }

            } catch (NoConnectionException | HttpException | StopException ignored) {

            }
        }).start());
        btnRemove.setOnMouseClicked(a -> {
            var x = CMsgBox
                    .msg(Alert.AlertType.CONFIRMATION, Translator.translate("ask.ask"), Translator.translate("ask.sure"))
                    .setButtons(CMsgBox.ResultType.YES, CMsgBox.ResultType.NO)
                    .executeForResult();
            if (x.isEmpty() || x.get().result() != CMsgBox.ResultType.YES)
                return;
            Modder.getModder().remove(profile, item);
        });

        super.getChildren().clear();
        super.getChildren().add(node);

        return this;
    }

    @Override
    public T getItem() {
        return item;
    }
}
