package com.cdev.corelauncher.data.entities;

import com.cdev.corelauncher.utils.entities.Path;
import javafx.scene.paint.Color;

public class Background {

    private String color;
    private Path image;

    public void setBackgroundColor(Color color) {
        this.color = color.toString();
    }

    public void setBackgroundImage(Path image) {
        this.image = image;
    }

    public Color getBackgroundColor(){
        return Color.valueOf(color);
    }

    public Path getBackgroundImage() {
        return this.image;
    }

}
