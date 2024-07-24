package com.laeben.corelauncher.api.ui.entity;

import javafx.stage.Popup;

import java.util.function.Consumer;

public interface PopupNode {
    Popup getPopup();
    void setPopup(Popup p);
    void usePopup(Consumer<Popup> m);
}
