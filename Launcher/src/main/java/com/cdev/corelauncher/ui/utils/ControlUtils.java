package com.cdev.corelauncher.ui.utils;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.input.ScrollEvent;

public class ControlUtils {
    public static void setTextFieldContext(TextField field){
        var context = new ContextMenu();

        field.setContextMenu(context);
    }

    public static void scroller(ScrollEvent e){
        var factory = ((Spinner<Double>)e.getSource()).getValueFactory();
        factory.increment(e.getDeltaY() > 0 ? 1 : -1);
    }
}
