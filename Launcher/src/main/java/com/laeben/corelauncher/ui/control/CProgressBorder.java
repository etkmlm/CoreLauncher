package com.laeben.corelauncher.ui.control;

import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Rectangle;

public class CProgressBorder extends StackPane {
    private final Arc arc;
    private final StackPane progressPane;
    private final Pane childPane;

    private final ObjectProperty<Node> child;
    private final DoubleProperty strokeWidth;
    private final DoubleProperty progress;

    public CProgressBorder(){
        strokeWidth = new SimpleDoubleProperty(4.0);
        progress = new SimpleDoubleProperty(0);
        child = new SimpleObjectProperty<>(new Rectangle());

        arc = new Arc();
        arc.setStartAngle(90);
        arc.setType(ArcType.ROUND);

        final var widthProp = widthProperty();
        final var heightProp = heightProperty();

        arc.radiusXProperty().bind(widthProp);
        arc.radiusYProperty().bind(heightProp);
        arc.layoutXProperty().bind(widthProp.divide(2));
        arc.layoutYProperty().bind(heightProp.divide(2));

        progressPane = new StackPane();
        progressPane.getStyleClass().add("progress");
        progressPane.setClip(arc);
        getChildren().add(progressPane);

        childPane = new StackPane();
        childPane.paddingProperty().bind(Bindings.createObjectBinding(() -> new Insets(getProgress() == 0 ? 0 : getStrokeWidth()), strokeWidthProperty(), progressProperty()));
        getChildren().add(childPane);

        child.addListener((observable, oldValue, newValue) -> {
            if (newValue == null) childPane.getChildren().clear();
            else childPane.getChildren().setAll(newValue);
        });

        progress.addListener((observable, oldValue, newValue) -> {
            arc.setLength(-newValue.doubleValue() * 360.0);
        });
    }

    public DoubleProperty strokeWidthProperty(){
        return strokeWidth;
    }
    public double getStrokeWidth(){
        return strokeWidth.getValue();
    }
    public void setStrokeWidth(double value){
        this.strokeWidth.setValue(value);
    }

    public DoubleProperty progressProperty(){
        return progress;
    }
    public double getProgress(){
        return progress.getValue();
    }
    public void setProgress(double value){
        this.progress.setValue(value);
    }

    public ObjectProperty<Node> childProperty(){
        return child;
    }
    public Node getChild(){
        return child.getValue();
    }
    public void setChild(Node value){
        this.child.setValue(value);
    }
}
