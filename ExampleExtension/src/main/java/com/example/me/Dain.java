package com.example.me;

import com.laeben.core.util.events.KeyEvent;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.minecraft.Launcher;
import com.laeben.corelauncher.wrap.entity.Extension;

import java.util.ResourceBundle;

public class Dain {
    public void init(Extension ext){
        System.out.println("Example extension initialization!");
        Launcher.getLauncher().getHandler().addHandler("exext", a -> {
            if (a instanceof KeyEvent e && e.getKey().startsWith(Launcher.SESSION_START)){
                System.out.println("START!!!");
            }
        }, false);
        Translator.registerSource(a -> ResourceBundle.getBundle("Test", a));
        ext.registerListener(new TestListener());
    }
}
