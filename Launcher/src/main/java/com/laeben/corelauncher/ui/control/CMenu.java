package com.laeben.corelauncher.ui.control;

import com.laeben.corelauncher.api.ui.entity.PopupNode;
import javafx.animation.ScaleTransition;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;
import javafx.util.Duration;

import java.util.function.Consumer;

public class CMenu extends VBox implements PopupNode {

    private final ScaleTransition trns;
    private CButton button;
    private Popup parentPopup;
    private final Popup popup;
    private final VBox menu;

    private boolean menuOpen;

    public CMenu(){
        popup = new Popup();
        popup.setAutoHide(true);
        menu = new VBox();
        menu.setFillWidth(true);
        //menu.setScaleY(0);
        menu.setScaleX(0);
        popup.setWidth(100);
        popup.setHeight(100);
        popup.getContent().add(menu);

        trns = new ScaleTransition();
        //trns.setFromY(0);
        trns.setFromX(0);
        //trns.setToY(1);
        trns.setToX(1);
        trns.setNode(menu);
        trns.setDuration(Duration.millis(500));
        popup.setOnHidden(a -> menuOpen = false);

        menu.setStyle("-fx-min-width: 120px;-fx-background-color: #333840aa;-fx-background-radius: 10px;");
        menuOpen = false;
    }

    public void setButton(CButton button){
        this.button = button;
        getChildren().removeIf(x -> x instanceof HBox);
        var container = new HBox();
        container.setAlignment(Pos.CENTER_RIGHT);
        container.getChildren().add(button);
        getChildren().add(container);
    }

    public void clear(){
        menu.getChildren().clear();
    }

    public void addItem(Image icon, String text, Consumer<MouseEvent> onClick){
        addItem(icon, text, onClick, true);
    }

    public CButton addItem(Image icon, String text, Consumer<MouseEvent> onClick, boolean hidePopup){
        var hbox = new HBox();
        hbox.setAlignment(Pos.CENTER);

        var btn = new CButton();
        btn.setText(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnMouseClicked(x -> {
            hide();
            onClick.accept(x);
            if (hidePopup)
                usePopup(PopupWindow::hide);
        });
        btn.setStyle("-fx-background-color:transparent;-fx-font-size: 12.5pt;-fx-pref-height: 30px;");

        if (icon != null){
            var img = new ImageView();
            var rect = new Rectangle(32, 32);
            rect.setArcHeight(10);
            rect.setArcWidth(10);
            img.setFitHeight(32);
            img.setFitWidth(32);
            img.setImage(icon);
            img.setClip(rect);
            hbox.setAlignment(Pos.CENTER_LEFT);
            hbox.getChildren().add(new Rectangle(8, 0));
            hbox.getChildren().add(img);
        }

        hbox.getChildren().add(btn);
        menu.getChildren().add(hbox);

        return btn;
    }

    public boolean isMenuOpen(){
        return menuOpen;
    }

    public void show(){
        var b = button.localToScreen(button.getBoundsInLocal());
        popup.show(button, b.getMaxX() - b.getWidth() * 1.5, b.getMaxY() - b.getHeight() * 2);
        trns.playFromStart();

        menuOpen = true;
    }

    public void hide(){
        trns.playFromStart();
        trns.jumpTo(Duration.ZERO);
        trns.stop();
        popup.hide();

        menuOpen = false;
    }

    @Override
    public Popup getPopup() {
        return parentPopup;
    }

    @Override
    public void setPopup(Popup p) {
        parentPopup = p;
    }

    @Override
    public void usePopup(Consumer<Popup> m) {
        if (parentPopup != null)
            m.accept(parentPopup);
    }
}
