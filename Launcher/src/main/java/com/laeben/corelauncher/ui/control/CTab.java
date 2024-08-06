package com.laeben.corelauncher.ui.control;

import com.laeben.corelauncher.api.ui.Controller;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

public class CTab extends Tab {
    private Controller controller;

    public void setController(Controller c){
        this.controller = c;
    }

    public Controller getController(){
        return controller;
    }

    public void dispose(){
        if (controller != null)
            controller.dispose();
    }
}
