package com.laeben.corelauncher.ui.entity;

import java.util.function.Consumer;

public interface CLSelectable {
    boolean isSelected();
    default boolean isSelectable(){
        return true;
    }
    void setSelectionListener(Consumer<Boolean> consumer);
    void setSelected(boolean v);
}
