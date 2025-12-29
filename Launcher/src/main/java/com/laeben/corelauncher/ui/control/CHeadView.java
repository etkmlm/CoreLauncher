package com.laeben.corelauncher.ui.control;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

public class CHeadView extends StackPane {

    private final DoubleProperty fitWidthProperty = new SimpleDoubleProperty();
    private final DoubleProperty fitHeightProperty = new SimpleDoubleProperty();

    private final ObjectProperty<Image> headProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<Image> decorationProperty = new SimpleObjectProperty<>();

    private CView headView;
    private CView decorationView;

    private Rectangle clipRect;

    private void putView(int desiredIndex, Image img){
        var view = new CView();
        view.setPreserveRatio(true);
        view.setImage(img);
        view.setFitWidth(getFitWidth());
        view.setFitHeight(getFitHeight());
        if (getChildren().size() <= desiredIndex)
            getChildren().add(view);
        else
            getChildren().set(desiredIndex, view);
    }

    public void setCornerRadius(double w, double h, double radius){
        clipRect = new Rectangle(w, h);
        clipRect.setArcHeight(radius);
        clipRect.setArcWidth(radius);

        setClip(clipRect);
    }

    public CHeadView(){
        headProperty.addListener((a, b, c) -> {
            putView(0, c);
        });
        decorationProperty.addListener((a, b, c) -> {
            putView(1, c);
        });
    }

    public DoubleProperty fitWidthProperty(){
        return fitWidthProperty;
    }

    public DoubleProperty fitHeightProperty(){
        return fitHeightProperty;
    }

    public ObjectProperty<Image> headProperty(){
        return headProperty;
    }

    public ObjectProperty<Image> decorationProperty(){
        return decorationProperty;
    }

    public double getFitWidth(){
        return fitWidthProperty.get();
    }
    public double getFitHeight(){
        return fitHeightProperty.get();
    }

    public void setFitWidth(double fitWidth){
        fitWidthProperty.set(fitWidth);
    }
    public void setFitHeight(double fitHeight){
        fitHeightProperty.set(fitHeight);
    }

    public Image getHead(){
        return headProperty.get();
    }
    public Image getDecoration(){
        return decorationProperty.get();
    }
    public void setHead(Image head){
        headProperty.set(head);
    }
    public void setDecoration(Image decoration){
        decorationProperty.set(decoration);
    }
}
