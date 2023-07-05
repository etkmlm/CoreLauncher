package com.laeben.corelauncher.minecraft.modding.curseforge.entities;

public class Search {
    public String searchFilter;
    public String gameVersion;
    public int classId;
    public int categoryId;
    public int index;
    public ModsSearchSortField sortField;
    public String sortOrder;
    public CurseWrapper.Type modLoaderType;

    public Search setSortOrder(boolean desc){
        sortOrder = desc ? "desc" : "asc";

        return this;
    }

    public void setSearchFilter(String filter){
        searchFilter = filter.replace(" ", "$20");
    }
}
