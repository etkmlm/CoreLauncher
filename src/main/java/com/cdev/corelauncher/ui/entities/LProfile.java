package com.cdev.corelauncher.ui.entities;

import com.cdev.corelauncher.data.entities.ChangeEvent;
import com.cdev.corelauncher.data.entities.Profile;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;

import java.util.function.Consumer;

public class LProfile {
    private final BooleanProperty isSelectedProperty;
    private Consumer<ChangeEvent> eventListener;
    private final Profile profile;

    private LProfile(Profile profile){
        this.profile = profile;
        isSelectedProperty = new SimpleBooleanProperty();
        isSelectedProperty.addListener((a, b, c) -> {
            if (eventListener != null)
                eventListener.accept(new ChangeEvent("profileSelect", b, c, this));
        });
    }

    public static LProfile get(Profile p){
        return new LProfile(p);
    }

    public Profile getProfile(){
        return profile;
    }

    public LProfile setEventListener(Consumer<ChangeEvent> e){
        eventListener = e;
        return this;
    }

    public void setSelected(boolean selected){
        isSelectedProperty.set(selected);

        /*if (eventListener != null)
            eventListener.accept(new ChangeEvent("profileSelect", null, selected, this));*/
    }

    public boolean selected(){
        return isSelectedProperty.get();
    }

    public final Observable[] getProperties(){
        return new Observable[] { isSelectedProperty };
    }
}
