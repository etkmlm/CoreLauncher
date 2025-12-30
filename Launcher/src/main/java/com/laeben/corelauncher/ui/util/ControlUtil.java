package com.laeben.corelauncher.ui.util;

import javafx.event.EventTarget;
import javafx.scene.Node;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;

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

    /**
     * Finds the TextField parent from the given event target. Otherwise, returns null.
     */
    public static TextField getTextFieldParent(EventTarget n){
        if (n == null) return null;
        if (n instanceof Text t)
            return getTextFieldParent(t.getParent());

        return n instanceof Pane p && p.getParent() != null && p.getParent() instanceof TextField tf ? tf : null;
    }
}
