package com.laeben.corelauncher.ui.tutorial.entity;

import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.laeben.core.entity.TranslationBundle;
import com.laeben.corelauncher.CoreLauncherFX;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.Translator;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;

import java.lang.reflect.Type;
import java.util.Locale;

public class Step {
    public static class ClassFactory implements JsonSerializer<Class>, JsonDeserializer<Class> {
        @Override
        public Class deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            try {
                return CoreLauncherFX.class.getClassLoader().loadClass(jsonElement.getAsString());
            } catch (ClassNotFoundException e) {
                return null;
            }
        }

        @Override
        public JsonElement serialize(Class aClass, Type type, JsonSerializationContext jsonSerializationContext) {
            return new JsonPrimitive(aClass.getName());
        }
    }

    private TranslationBundle description;
    private boolean endStep, firstStep;
    private KeyCode keyCode;
    private MouseButton mouseButton;
    private String focus;

    @JsonAdapter(ClassFactory.class)
    private Class controller;

    private boolean isDialogController;

    public static Step of(String description){
        var step = new Step();
        var bundle = new TranslationBundle();
        bundle.put(Configurator.getConfig().getLanguage().getLanguage().toLowerCase(), description);
        step.description = bundle;
        return step;
    }

    public static Step ofTranslated(String key){
        return of(Translator.translate(key));
    }

    public Step last(){
        endStep = true;
        firstStep = false;
        return this;
    }

    public Step first(){
        firstStep = true;
        endStep = false;
        return this;
    }

    public Step withKeyCode(KeyCode keyCode){
        this.keyCode = keyCode;
        return this;
    }

    public Step withMouseButton(MouseButton button){
        this.mouseButton = button;
        return this;
    }

    public Step withFocusSelector(String selector){
        this.focus = selector;
        return this;
    }

    public Step toDialog(Class controller){
        isDialogController = true;
        this.controller = controller;
        return this;
    }

    public Step toController(Class controller){
        isDialogController = false;
        this.controller = controller;
        return this;
    }

    public KeyCode getKeyCode() {
        return keyCode;
    }

    public MouseButton getMouseButton() {
        return mouseButton;
    }

    public String getFocusSelector() {
        return focus;
    }

    public String getDescription(Locale locale) {
        return description == null ? null : description.get(locale);
    }

    public boolean isEndStep() {
        return endStep;
    }

    public boolean isFirstStep() {
        return firstStep;
    }

    public <T> Class<T> getToController() {
        return controller;
    }

    public boolean isDialogController() {
        return isDialogController;
    }
}
