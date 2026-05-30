package com.laeben.corelauncher.api.ui.entity;

import com.laeben.corelauncher.api.entity.ImageEntity;
import javafx.scene.paint.Color;

public class UIPreference {
    private String identifier;
    private String customColor;
    private ImageEntity customImage;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getCustomColor() {
        return customColor;
    }

    public void setCustomColor(String customColor) {
        this.customColor = customColor;
    }

    public void setCustomColor(Color customColor) {
        this.customColor = customColor == null ? null : "#" + customColor.toString().substring(2);
    }

    public ImageEntity getCustomImage() {
        return customImage;
    }

    public void setCustomImage(ImageEntity customImage) {
        this.customImage = customImage;
    }
}
