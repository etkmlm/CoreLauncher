package com.laeben.corelauncher.ui.controller.cell;

import com.laeben.core.util.events.ValueEvent;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.ui.control.*;
import com.laeben.corelauncher.ui.entity.CLSelectable;
import com.laeben.corelauncher.util.ImageCacheManager;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class CPLCell extends CCell<Profile> implements CLSelectable {
    public static final String EXECUTOR_ROOT = "root";
    public static final String EXECUTOR_MENU = "menu";
    public static final String PLAY = "play";


    private final CMenu menu;
    private CList list;

    private final BooleanProperty selected;
    private boolean selectionDisabled = false;

    public CPLCell() {
        super("layout/cells/ccell.fxml");

        selected = new SimpleBooleanProperty();

        menu = new CMenu();

        box.getChildren().addAll(btnPlay, new Rectangle(15, 0), menu);
    }

    private Profile item;

    @FXML
    public HBox root;

    @FXML
    public CView image;
    @FXML
    public Label lblName;

    @FXML
    public HBox box;

    public CButton btnPlay;
    public CButton btnMenu;

    private Predicate<ValueEvent> onClick;

    @FXML
    public void initialize(){
        lblName.setStyle("-fx-text-fill: white;-fx-font-size: 13pt;");

        root.setOnMouseClicked(a -> {
            if ((list.selectionModeProperty().get() || a.isControlDown()) && !selectionDisabled){
                list.selectionModeProperty().set(true);
                setSelected(!isSelected());
                return;
            }
            if (onClick !=null)
                onClick.test((ValueEvent) new ValueEvent(EXECUTOR_ROOT, null).setSource(this));
        });

        root.setOnMouseEntered(a -> {
            if (!isSelected())
                fade.playFromStart();
        });

        root.setOnMouseExited(a -> {
            if (!isSelected()){
                fade.play();
                fade.jumpTo(Duration.ZERO);
                fade.stop();
            }
        });

        btnPlay = new CButton();
        btnPlay.setPrefWidth(72);
        btnPlay.setPrefHeight(72);
        btnPlay.setText("⯈");
        btnPlay.enableTransparentAnimation();
        btnPlay.setStyle("-fx-background-color: transparent; -fx-font-size: 24pt;");

        btnMenu = new CButton();
        btnMenu.setPrefWidth(72);
        btnMenu.setPrefHeight(72);
        btnMenu.setText("...");
        //btnMenu.setPadding(new Insets(0, 0, 5, 0));
        btnMenu.setStyle("-fx-font-size: 18pt; -fx-padding: 0 0 10px 0; -fx-background-color: transparent;");
        btnMenu.enableTransparentAnimation();
        btnMenu.setOnMouseClicked(a -> menu.show());

        box.setAlignment(Pos.CENTER);

        image.setFitWidth(64);
        image.setFitHeight(64);
        image.setCornerRadius(64, 64, 8);

        lblName.maxWidthProperty().bind(root.widthProperty().subtract(300));
        lblName.setStyle("-fx-font-size: 18pt;-fx-font-weight: 600");
    }

    public CPLCell setOnClick(Predicate<ValueEvent> onClick){
        this.onClick = onClick;

        return this;
    }

    public CPLCell includePlayButton(boolean value){
        btnPlay.setVisible(value);
        btnPlay.setManaged(value);
        return this;
    }

    public CPLCell setSelectionDisabled(boolean va){
        selectionDisabled = va;
        return this;
    }

    @Override
    public CCell setItem(Profile item) {
        /*if (this.item == null){
            box.getChildren().remove(btnAction);
            //box.getChildren().remove(btnPlay);
            if (!box.getChildren().contains(menu))
                box.getChildren().add(menu);
        }*/

        this.item = item;

        btnPlay.setOnMouseClicked(a -> {
            Main.getMain().selectProfile(item);
            if (onClick != null)
                onClick.test((ValueEvent) new ValueEvent(PLAY, null).setSource(this));
        });
        CDockObject.generateProfileMenu(menu, item, btnMenu, a -> onClick == null || onClick.test((ValueEvent) new ValueEvent(EXECUTOR_MENU, a).setSource(this)));

        if (!selectionDisabled){
            var btnSelect = menu.addItem(null, Translator.translate("option.select"), a -> {
                if (isSelected())
                    setSelected(false);
                else{
                    list.setSelectionMode(true);
                    setSelected(true);
                }

            }, false);
            selected.addListener(a -> {
                if (isSelected())
                    btnSelect.setText(Translator.translate("option.deselect"));
                else
                    btnSelect.setText(Translator.translate("option.select"));
            });
        }


        image.setImage(ImageCacheManager.getImage(item, 64));
        lblName.setText(item.getName());
        lblName.setTooltip(new Tooltip(item.getName()));

        super.getChildren().clear();
        super.getChildren().add(node);

        return this;
    }

    @Override
    public Profile getItem() {
        return item;
    }

    @Override
    public CCell setList(CList list) {
        this.list = list;

        return this;
    }

    @Override
    public boolean isSelected() {
        return selected.get();
    }

    @Override
    public void setSelectionListener(Consumer<Boolean> consumer) {
        selected.addListener(a -> consumer.accept(selected.get()));
    }

    @Override
    public void setSelected(boolean v) {
        selected.set(v);
        if (root == null)
            return;
        root.setOpacity(v ? 0.5 : 1);
    }
}
