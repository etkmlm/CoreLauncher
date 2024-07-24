package com.laeben.corelauncher.ui.entity;

import java.util.function.Consumer;

public interface CLSelectable {
    boolean isSelected();
    void setSelectionListener(Consumer<Boolean> consumer);
    void setSelected(boolean v);
}
