package com.cdev.corelauncher.minecraft.wrappers;

import com.cdev.corelauncher.minecraft.Wrapper;
import javafx.scene.image.Image;

public class Vanilla extends Wrapper {
    @Override
    public Image getIcon(){
        var str = Vanilla.class.getResourceAsStream("/com/cdev/corelauncher/images/vanilla.png");
        return str == null ? null : new Image(str);
    }
}
