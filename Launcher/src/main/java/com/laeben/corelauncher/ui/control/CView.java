package com.laeben.corelauncher.ui.control;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;

import java.io.IOException;
import java.net.URL;

public class CView extends ImageView {

    private Rectangle clipRect;

    public CView(){

    }

    public void setCornerRadius(double w, double h, double radius){
        clipRect = new Rectangle(w, h);
        clipRect.setArcHeight(radius);
        clipRect.setArcWidth(radius);

        setClip(clipRect);
    }

    public void setImage(URL url){
        try {
            super.setImage(new Image(url.openStream(), getFitWidth(), getFitHeight(), false, false));
        } catch (IOException ignored) {

        }
    }
}
