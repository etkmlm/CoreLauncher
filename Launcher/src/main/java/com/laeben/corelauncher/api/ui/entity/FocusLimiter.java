package com.laeben.corelauncher.api.ui.entity;

import com.laeben.corelauncher.api.ui.Controller;
import javafx.scene.Node;

public interface FocusLimiter {

    Node getTargetFocusNode();
    void focus();
    void onFocusLimitIgnored(Controller by, Node target);
    default boolean verify(double mX, double mY){
        return FocusLimiter.verify(this, mX, mY);
    }

    static boolean verify(FocusLimiter limiter, double mX, double mY) {
        var node = limiter.getTargetFocusNode();
        var bounds = node.getBoundsInLocal();

        return node.localToScene(bounds).contains(mX, mY);
    }
}
