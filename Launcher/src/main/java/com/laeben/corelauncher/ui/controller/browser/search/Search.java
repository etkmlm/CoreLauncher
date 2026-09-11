package com.laeben.corelauncher.ui.controller.browser.search;

import com.laeben.corelauncher.minecraft.modding.entity.LoaderType;
import com.laeben.corelauncher.minecraft.modding.entity.ModSide;
import com.laeben.corelauncher.minecraft.modding.entity.ResourceType;
import com.laeben.corelauncher.ui.controller.browser.cell.ResourceCellItem;

import java.util.List;

public interface Search<T extends Enum> {
    void setMainType(ResourceType type);
    void setSortOrder(boolean asc);
    void setSortField(T field);
    void setCategories(List<String> cats);
    void setPageIndex(int index);
    void setGameVersions(List<String> gameVersions);
    void setSides(List<ModSide> sides);
    void setLoaders(List<LoaderType> loaders);
    void reset();
    int getTotalPages();
    List<ResourceCellItem> search(String query);
}
