package com.laeben.corelauncher.ui.controller.browser;

import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.minecraft.modding.curseforge.CurseForge;
import com.laeben.corelauncher.minecraft.modding.curseforge.entity.CurseWrapper;
import com.laeben.corelauncher.minecraft.modding.curseforge.entity.ModsSearchSortField;
import com.laeben.corelauncher.minecraft.modding.curseforge.entity.SearchForge;
import com.laeben.corelauncher.minecraft.modding.curseforge.entity.SearchResponseForge;
import com.laeben.corelauncher.minecraft.modding.entity.ResourceType;

import java.util.List;

public class ForgeSearch implements Search<ModsSearchSortField> {
    private SearchForge searchForge;

    private final Profile profile;

    private int totalPages;
    private int pageSize;

    public ForgeSearch(Profile profile){
        this.profile = profile;
    }

    public void reset(){
        searchForge = new SearchForge();
        searchForge.classId = ResourceType.MOD.getId();
        searchForge.gameVersion = profile.getVersionId();
        searchForge.modLoaderType = CurseWrapper.Type.fromLoaderType(profile.getWrapper().getType());
        searchForge.sortField = ModsSearchSortField.POPULARITY;
        searchForge.sortOrder = "desc";
    }

    public void setCategory(int id){
        searchForge.categoryId = id;
    }

    public void setLoaderType(CurseWrapper.Type ty){
        searchForge.modLoaderType = ty;
    }

    @Override
    public void setPageIndex(int index) {
        searchForge.index = (index - 1) * pageSize;
    }

    public void setMainType(ResourceType type){
        searchForge.classId = type.getId();

        if (profile.getWrapper().getType().isNative() && !type.isGlobal())
            setLoaderType(CurseWrapper.Type.NONE);
        else
            setLoaderType(CurseWrapper.Type.fromLoaderType(profile.getLoaderType(type)));
    }

    public void setSortOrder(boolean asc){
        searchForge.setSortOrder(asc);
    }

    public void setCategories(List<Object> cats){
        searchForge.categoryIds = "[" + String.join(",", cats.toArray(String[]::new)) + "]";
    }

    public void setSortField(ModsSearchSortField field){
        searchForge.sortField = field;
    }

    public int getTotalPages(){
        return totalPages;
    }

    public List<ResourceCell.Link> search(String query){
        searchForge.setSearchFilter(query);
        SearchResponseForge sData = null;
        try{
            sData = CurseForge.getForge().search(searchForge);
        } catch (NoConnectionException | HttpException ignored) {

        }

        if (sData == null)
            return List.of();


        pageSize = sData.pagination.pageSize;
        totalPages = (int)Math.ceil((double)sData.pagination.totalCount / pageSize);
        return sData.data.stream().map(x -> new ResourceCell.Link(profile, x)).toList();
    }
}
