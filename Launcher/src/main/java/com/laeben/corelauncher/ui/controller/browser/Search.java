package com.laeben.corelauncher.ui.controller.browser;

import com.laeben.corelauncher.minecraft.modding.entity.ResourceType;

import java.util.List;

public interface Search<T extends Enum> {
    void setMainType(ResourceType type);
    void setSortOrder(boolean asc);
    void setSortField(T field);
    void setCategories(List<Object> cats);
    void setPageIndex(int index);
    void reset();
    int getTotalPages();
    List<ResourceCell.Link> search(String query);
}
