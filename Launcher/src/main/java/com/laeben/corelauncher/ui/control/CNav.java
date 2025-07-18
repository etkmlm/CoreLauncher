package com.laeben.corelauncher.ui.control;

import javafx.animation.ScaleTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class CNav extends VBox {
    private final ScaleTransition transition;

    private final BooleanProperty enabled;
    private final BooleanProperty animationEnabled;


    public CNav(){
        enabled = new SimpleBooleanProperty(false);
        animationEnabled = new SimpleBooleanProperty(false);

        transition = new ScaleTransition();
        transition.setFromY(0);
        transition.setToY(1);
        transition.setNode(this);
        transition.setDuration(Duration.millis(100));

        enabled.addListener(a -> {
            this.setVisible(isEnabled());
            this.setManaged(isEnabled());

            if (isEnabled() && animationEnabled.get())
                transition.playFromStart();
        });

        pad(2, 5, 2, 5);
        setAlignment(Pos.CENTER);
        setEnabled(false);
        getStyleClass().add("cnav");
        setStyle("-fx-background-radius: 8px;-fx-background-color: #00808010;");
    }

    public Node getItem(int row, int col){
        return ((HBox)getChildren().get(row)).getChildren().get(col);
    }

    private HBox generateHBox(){
        var box = new HBox();
        box.setSpacing(10);
        HBox.setHgrow(box, Priority.ALWAYS);
        box.setAlignment(Pos.CENTER);

        return box;
    }

    public HBox generateRow(){
        var box = generateHBox();
        getChildren().add(box);
        return box;
    }

    public void pad(double t, double r, double b, double l){
        setPadding(new Insets(t, r, b, l));
    }

    private HBox getBox(int row){
        return getChildren().size() < row + 1 ? generateRow() : (HBox) getChildren().get(row);
    }

    private CShapefulButton getButton(String text, EventHandler<?super MouseEvent> click){
        var btn = new CShapefulButton();
        btn.setText(text);
        btn.getStyleClass().add("cnav-button");
        btn.setOnMouseClicked(click);
        //btn.enableTransparentAnimation();
        //btn.setStyle("-fx-background-color: transparent;-fx-font-size: 11.5pt");
        return btn;
    }

    public void addItem(String text, String shape, EventHandler<? super MouseEvent> onClick, int row){
        var btn = getButton(text, onClick);
        if (shape != null)
            btn.setStyle("-shape: " + shape);
        getBox(row).getChildren().add(btn);
    }
    public void setItem(String text, String shape, EventHandler<? super MouseEvent> onClick, int row, int col){
        var btn = getButton(text, onClick);
        if (shape != null)
            btn.setStyle("-shape: " + shape);
        getBox(row).getChildren().set(col, btn);
    }
    public void delItem(int row, int col){
        getBox(row).getChildren().remove(col);
    }

    public <T> CStatusLabel<T> addLabel(T value, int row){
        var field = new CStatusLabel<T>();
        field.setValue(value);
        getBox(row).getChildren().add(field);
        return field;
    }

    public BooleanProperty enabledProperty(){
        return enabled;
    }
    public BooleanProperty animationEnabledProperty(){
        return animationEnabled;
    }

    public void setAnimationEnabled(boolean a){
        animationEnabled.set(a);
    }

    public boolean isEnabled(){
        return enabled.get();
    }

    public void setEnabled(boolean e){
        enabled.set(e);
    }
}
