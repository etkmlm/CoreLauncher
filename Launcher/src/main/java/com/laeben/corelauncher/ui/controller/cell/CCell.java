package com.laeben.corelauncher.ui.controller.cell;

import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.api.ui.UI;
import com.laeben.corelauncher.ui.control.CList;
import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Duration;

public abstract class CCell<T> extends HBox {

    protected final Node node;
    protected final FadeTransition fade;

    protected CCell(String fxml){
        node = UI.getUI().load(CoreLauncherFX.class.getResource(fxml), this);
        HBox.setHgrow(node, Priority.ALWAYS);

        fade = new FadeTransition();
        fade.setFromValue(1);
        fade.setToValue(0.7);
        fade.setDuration(Duration.millis(200));
        fade.setNode(node);
    }

    public abstract CCell setItem(T item);
    public abstract T getItem();

    public Node getNode(){
        return node;
    }

    public CCell setList(CList list){
        return this;
    }

}
