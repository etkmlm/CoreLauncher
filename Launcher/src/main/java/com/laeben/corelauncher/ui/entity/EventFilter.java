package com.laeben.corelauncher.ui.entity;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Window;

public class EventFilter<T extends Event>{
    private final EventType<T> eventType;
    private final EventHandler<T> eventHandler;
    private Window wnd;
    private Scene scene;
    private Node node;

    public EventFilter(EventType<T> eventType, EventHandler<T> eventHandler) {
        this.eventType = eventType;
        this.eventHandler = eventHandler;
    }

    public EventFilter<T> setWindow(Window wnd) {
        this.wnd = wnd;
        this.scene = null;
        this.node = null;

        return this;
    }

    public EventFilter<T> setScene(Scene scene) {
        this.scene = scene;
        this.node = null;
        this.wnd = null;

        return this;
    }

    public EventFilter<T> setNode(Node node) {
        this.node = node;
        this.scene = null;
        this.wnd = null;

        return this;
    }

    public void add(){
        if (wnd != null)
            wnd.addEventFilter(eventType, eventHandler);

        if (node != null)
            node.addEventFilter(eventType, eventHandler);

        if (scene != null)
            scene.addEventFilter(eventType, eventHandler);
    }

    public void remove(){
        if (wnd != null)
            wnd.removeEventFilter(eventType, eventHandler);

        if (node != null)
            node.removeEventFilter(eventType, eventHandler);
    }

    public static <T extends Event> EventFilter window(Window wnd, EventType<T> type, EventHandler<T> handler) {
        return new EventFilter(type, handler).setWindow(wnd);
    }

    public static <T extends Event> EventFilter node(Node nd, EventType<T> type, EventHandler<T> handler) {
        return new EventFilter(type, handler).setNode(nd);
    }

    public static <T extends Event> EventFilter scene(Scene sc, EventType<T> type, EventHandler<T> handler) {
        return new EventFilter(type, handler).setScene(sc);
    }

    public boolean checkOwner(Object obj){
        if (wnd != null)
            return wnd.equals(obj);
        if (node != null)
            return node.equals(obj);
        if (scene != null)
            return scene.equals(obj);

        return false;
    }
}