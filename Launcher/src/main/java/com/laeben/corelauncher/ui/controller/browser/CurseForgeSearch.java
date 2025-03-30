package com.laeben.corelauncher.ui.controller.browser;

import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.minecraft.modding.curseforge.CurseForge;
import com.laeben.corelauncher.minecraft.modding.curseforge.entity.CurseForgeWrapper;
import com.laeben.corelauncher.minecraft.modding.curseforge.entity.ModsSearchSortField;
import com.laeben.corelauncher.minecraft.modding.curseforge.entity.CurseForgeSearchRequest;
import com.laeben.corelauncher.minecraft.modding.curseforge.entity.CurseForgeSearchResponse;
import com.laeben.corelauncher.minecraft.modding.entity.LoaderType;
import com.laeben.corelauncher.minecraft.modding.entity.ResourcePreferences;
import com.laeben.corelauncher.minecraft.modding.entity.ResourceType;

import java.util.Collections;
import java.util.List;

public class CurseForgeSearch implements Search<ModsSearchSortField> {
    private CurseForgeSearchRequest request;

    private final Profile profile;

    private int totalPages;
    private int pageSize;

    public CurseForgeSearch(Profile profile){
        this.profile = profile;
    }

    public void reset(){
        request = new CurseForgeSearchRequest();
        request.classId = ResourceType.MOD.getId();
        if (profile != null){
            request.gameVersion = profile.getVersionId();
            request.modLoaderType = CurseForgeWrapper.Type.fromLoaderType(profile.getWrapper().getType());
        }
        else{
            request.gameVersion = null;
            request.modLoaderType = null;
        }
        request.gameVersions = null;
        request.modLoaderTypes = null;
        request.sortField = ModsSearchSortField.POPULARITY;
        request.sortOrder = "desc";
    }

    public void setCategory(int id){
        request.categoryId = id;
    }

    public void setLoaderType(CurseForgeWrapper.Type ty){
        request.modLoaderType = ty;
        request.modLoaderTypes = null;
    }

    @Override
    public void setPageIndex(int index) {
        request.index = (index - 1) * pageSize;
    }

    @Override
    public void setGameVersions(List<String> gameVersions) {
        //request.gameVersion = null;
        request.gameVersions = gameVersions == null ? null : gameVersions.stream().map(x -> "\"" + x + "\"").toList();
    }

    @Override
    public void setLoaders(List<LoaderType> loaders) {
        //request.modLoaderType = null;
        request.modLoaderTypes = loaders == null ? null : loaders.stream().map(CurseForgeWrapper.Type::fromLoaderType).toList();
    }

    public void setMainType(ResourceType type){
        request.classId = type.getId();

        if (profile == null){
            setLoaderType(null);
            return;
        }

        if (profile.getWrapper().getType().isNative() && !type.isGlobal())
            setLoaderType(CurseForgeWrapper.Type.NONE);
        else
            setLoaderType(CurseForgeWrapper.Type.fromLoaderType(profile.getLoaderType(type)));
    }

    public void setSortOrder(boolean asc){
        request.setSortOrder(asc);
    }

    public void setCategories(List<String> cats){
        //request.categoryIds = "[" + String.join(",", cats) + "]";
        request.categoryIds = cats;
    }

    public void setSortField(ModsSearchSortField field){
        request.sortField = field;
    }

    public int getTotalPages(){
        return totalPages;
    }

    public List<ResourceCell.Link> search(String query){
        request.setSearchFilter(query);
        CurseForgeSearchResponse sData = null;
        try{
            sData = CurseForge.getForge().search(request);
        } catch (NoConnectionException | HttpException ignored) {

        }

        if (sData == null)
            return List.of();


        pageSize = sData.pagination.pageSize;
        totalPages = (int)Math.ceil((double)sData.pagination.totalCount / pageSize);
        ResourcePreferences prefs;

        if (profile != null)
            prefs = ResourcePreferences.fromProfile(profile);
        else {
            prefs = ResourcePreferences.empty();
            if (request.gameVersions != null)
                prefs.includeGameVersions(request.gameVersions);
            else if (request.gameVersion != null)
                prefs.includeGameVersions(List.of(request.gameVersion));
            if (request.modLoaderTypes != null)
                prefs.includeLoaders(request.modLoaderTypes.stream().map(CurseForgeWrapper.Type::toLoaderType).toList());
            else if (request.modLoaderType != null)
                prefs.includeLoaders(List.of(CurseForgeWrapper.Type.toLoaderType(request.modLoaderType)));
        }

        return sData.data.stream().map(x -> new ResourceCell.Link(prefs, x)).toList();
    }
}
