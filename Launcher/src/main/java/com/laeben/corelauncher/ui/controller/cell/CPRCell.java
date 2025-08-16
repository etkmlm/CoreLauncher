package com.laeben.corelauncher.ui.controller.cell;

import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.core.util.events.ValueEvent;
import com.laeben.corelauncher.api.Profiler;
import com.laeben.corelauncher.api.ui.entity.Announcement;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.minecraft.modding.Modder;
import com.laeben.corelauncher.minecraft.modding.entity.ResourceType;
import com.laeben.corelauncher.minecraft.modding.entity.resource.CResource;
import com.laeben.corelauncher.ui.control.CMsgBox;
import com.laeben.corelauncher.ui.control.CShapefulButton;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.ui.control.CButton;
import com.laeben.corelauncher.ui.control.CView;
import com.laeben.corelauncher.api.ui.UI;
import com.laeben.corelauncher.ui.entity.CLSelectable;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.function.Consumer;
import java.util.function.Predicate;

// Profile Resource Cell
public class CPRCell<T extends CResource> extends CCell<T> implements CLSelectable {

    public static final String UPDATE = "update";
    public static final String REMOVE = "remove";

    private T item;
    private boolean autoChanged;
    private boolean selected;
    private Consumer<Boolean> onSelected;

    private final Profile profile;
    private final boolean selectable;

    public CPRCell(Profile profile, boolean selectable){
        super("layout/cells/ccell.fxml");

        this.profile = profile;
        this.selectable = selectable;

        node.setOnMouseEntered(a -> fade.playFromStart());

        node.setOnMouseExited(a -> {
            fade.play();
            fade.jumpTo(Duration.ZERO);
            fade.stop();
        });

        toggle = new CButton();
        toggle.setStyle("-fx-padding: 0 8px 0 0; -fx-min-width: 24px; -fx-pref-width: 24px; -fx-pref-height: 24px");
        toggle.setOnMouseClicked(a -> {
            a.consume();
            toggleEnableButton();
        });

        btnRemove = new CButton();
        btnRemove.setText("—");
        btnRemove.setStyle("-fx-background-color: transparent; -fx-font-size: 13pt");
        btnRemove.enableTransparentAnimation();

        btnUpdate = new CShapefulButton();
        btnUpdate.setStyle("-fx-background-color: transparent; -shape: -shape-update; -shape-width:12px;-shape-height:12px;-fx-spacing: 0");
        btnUpdate.enableTransparentAnimation();

        box.setAlignment(Pos.CENTER);
        box.setSpacing(4);
        box.getChildren().addAll(toggle, new Rectangle(10, 0), btnUpdate, btnRemove);
    }

    @FXML
    private CView image;
    @FXML
    private Label lblName;

    @FXML
    private HBox box;

    private final CButton toggle;
    private final CButton btnRemove;
    private final CShapefulButton btnUpdate;

    private Predicate<ValueEvent> onAction;


    public void toggleEnableButton(){
        if (item == null)
            return;

        Profiler.setResourceDisabled(profile, item, !item.disabled, true);
        invalidateToggle();
    }

    public static <T extends CResource> boolean toggleUpdate(Profile profile, T item){
        try {
            Main.getMain().announceLater(Translator.getTranslator().getTranslate("announce.info.update.title"), Translator.translateFormat("announce.info.update.search.single", item.name), Announcement.AnnouncementType.INFO, Duration.seconds(2));
            var upResources = Modder.getModder().getUpdate(profile, item);
            if (upResources != null){
                var res = upResources.stream().filter(k -> k.isSameResource(item)).findFirst().orElse(item);
                Modder.getModder().include(profile, upResources);
                Main.getMain().announceLater(Translator.getTranslator().getTranslate("announce.info.update.title"), Translator.translateFormat("announce.info.update.ok.single", res.name, res.fileName), Announcement.AnnouncementType.INFO, Duration.seconds(2));
                return true;
            }
            else{
                Main.getMain().announceLater(Translator.getTranslator().getTranslate("announce.info.update.title"), Translator.translateFormat("announce.info.update.mpcontent", item.name), Announcement.AnnouncementType.ERROR, Duration.seconds(4));
            }

        } catch (NoConnectionException | HttpException | StopException ignored) {
            int m = 0;
        }

        return false;
    }

    @FXML
    public void initialize(){
        setOnMouseClicked(a -> setSelected(!isSelected()));
    }

    public CPRCell<T> setOnAction(Predicate<ValueEvent> onAction){
        this.onAction = onAction;
        return this;
    }

    @Override
    public CCell setItem(T item) {
        this.item = item;

        image.setImageAsync(item.getIcon(32, 32));

        if (item.getType() == ResourceType.WORLD || item.getType() == ResourceType.MODPACK){
            toggle.setVisible(false);
        }
        else{
            toggle.setVisible(true);
            invalidateToggle();
        }

        lblName.setText(item.name);

        lblName.setTooltip(new Tooltip(item.fileName));
        //btnUpdate.setTooltip(new Tooltip(Translator.translate("update")));

        btnUpdate.setOnMouseClicked(a -> {
            a.consume();
            if (onAction == null || onAction.test((ValueEvent) new ValueEvent(UPDATE, item).setSource(this)))
                new Thread(() -> toggleUpdate(profile, item)).start();
        });
        btnRemove.setOnMouseClicked(a -> {
            a.consume();
            var x = CMsgBox
                    .msg(Alert.AlertType.CONFIRMATION, Translator.translate("ask.ask"), Translator.translate("ask.sure"))
                    .setButtons(CMsgBox.ResultType.YES, CMsgBox.ResultType.NO)
                    .executeForResult();
            if (x.isEmpty() || x.get().result() != CMsgBox.ResultType.YES)
                return;
            if (onAction == null || onAction.test((ValueEvent) new ValueEvent(REMOVE, item).setSource(this)))
                Modder.getModder().remove(profile, item);
        });

        super.getChildren().clear();
        super.getChildren().add(node);

        return this;
    }

    public void invalidateToggle(){
        if (item == null || item.disabled){
            toggle.setTooltip(Translator.translate("option.enable"));
            toggle.getStyleClass().add("toggle-off");
            toggle.getStyleClass().remove("toggle-on");
        }
        else{
            toggle.setTooltip(Translator.translate("option.disable"));
            toggle.getStyleClass().add("toggle-on");
            toggle.getStyleClass().remove("toggle-off");
        }
    }

    @Override
    public T getItem() {
        return item;
    }

    @Override
    public boolean isSelectable(){
        return selectable;
    }

    @Override
    public boolean isSelected() {
        return selected;
    }

    @Override
    public void setSelectionListener(Consumer<Boolean> consumer) {
        this.onSelected = consumer;
    }

    @Override
    public void setSelected(boolean v) {
        if (!isSelectable())
            return;

        selected = v;

        setOpacity(selected ? 0.5 : 1);

        if (onSelected != null)
            onSelected.accept(selected);
    }
}
