package com.laeben.corelauncher.ui.util;

import javafx.scene.Node;
import javafx.scene.control.Spinner;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;

public class ControlUtil {
    public static void scroller(ScrollEvent e){
        e.consume();

        var factory = ((Spinner<Double>)e.getSource()).getValueFactory();
        if (e.getDeltaY() == 0)
            return;
        factory.increment(e.getDeltaY() > 0 ? 1 : -1);
    }

    public static void setAnchorFill(Node n){
        AnchorPane.setTopAnchor(n, 0.0);
        AnchorPane.setBottomAnchor(n, 0.0);
        AnchorPane.setLeftAnchor(n, 0.0);
        AnchorPane.setRightAnchor(n, 0.0);
    }
}
