package com.laeben.corelauncher.ui.entity;

public interface Depend<T> {
    T getDepend();

    void onDependUpdate(T depend);
}
