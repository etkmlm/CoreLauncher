package com.laeben.corelauncher.ui.control;

import javafx.css.PseudoClass;

public class CSwitchableButton extends CButton {
    private static final PseudoClass SWITCH_ON = PseudoClass.getPseudoClass("switchedOn");
    public CSwitchableButton() {
        getStyleClass().add("cswitch-button");


    }

    public void sw(boolean value){
        if (value){
            getStyleClass().add("cswitch-button-on");
            getStyleClass().remove("cswitch-button-off");
        }
        else{
            getStyleClass().remove("cswitch-button-on");
            getStyleClass().add("cswitch-button-off");
        }
    }
}
