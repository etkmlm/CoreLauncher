package com.laeben.corelauncher.ui.control;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Label;

import java.util.function.Function;
import java.util.function.Predicate;

public class CStatusLabel<T> extends Label {

    private T defaultValue;
    private final ObjectProperty<T> value;
    private Function<T, String> factory;
    private Predicate<T> isEmptyFactory;
    private boolean empty;

    public CStatusLabel(){
        value = new SimpleObjectProperty<>();

        setValue(null);
    }

    public void setTextFactory(Function<T, String> factory){
        this.factory = factory;
    }
    public void setIsEmpty(Predicate<T> isEmpty){
        this.isEmptyFactory = isEmpty;
    }
    public void setValue(T val){
        empty = true;

        if (factory == null)
            return;
        value.set(val);
        if (val != null && !(isEmptyFactory == null || isEmptyFactory.test(val))){
            setText(factory.apply(val));
            setManaged(true);
            setVisible(true);
            empty = false;
        }
        else{
            setManaged(false);
            setVisible(false);
        }

    }
    public void setDefaultValue(T val){
        defaultValue = val;
    }
    public T getDefaultValue(){
        return defaultValue;
    }

    public boolean isEmpty(){
        return empty;
    }

    public T getValue(){
        return value.get() == null ? defaultValue : value.get();
    }

    public ObjectProperty<T> valueProperty(){
        return value;
    }
}
