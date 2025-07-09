package com.laeben.corelauncher.ui.controller.browser;

import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.minecraft.modding.curseforge.CurseForge;
import com.laeben.corelauncher.minecraft.modding.curseforge.entity.CurseForgeLoader;
import com.laeben.corelauncher.minecraft.modding.curseforge.entity.ModsSearchSortField;
import com.laeben.corelauncher.minecraft.modding.curseforge.entity.CurseForgeSearchRequest;
import com.laeben.corelauncher.minecraft.modding.curseforge.entity.CurseForgeSearchResponse;
import com.laeben.corelauncher.minecraft.modding.entity.LoaderType;
import com.laeben.corelauncher.minecraft.modding.entity.ModResource;
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
            request.modLoaderType = CurseForgeLoader.Type.fromLoaderType(profile.getLoader().getType());
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

    public void setLoaderType(CurseForgeLoader.Type ty){
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
        request.gameVersions = gameVersions == null ? null : Collections.unmodifiableList(gameVersions);
    }

    @Override
    public void setLoaders(List<LoaderType> loaders) {
        //request.modLoaderType = null;
        request.modLoaderTypes = loaders == null ? null : loaders.stream().map(CurseForgeLoader.Type::fromLoaderType).toList();
    }

    public void setMainType(ResourceType type){
        request.classId = type.getId();

        if (profile == null){
            setLoaderType(null);
            return;
        }

        if (profile.getLoader().getType().isNative() && !type.isGlobal())
            setLoaderType(CurseForgeLoader.Type.NONE);
        else
            setLoaderType(CurseForgeLoader.Type.fromLoaderType(ModResource.getGlobalSafeLoaders(type, profile.getLoader().getType())));
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
                prefs.includeLoaderTypes(request.modLoaderTypes.stream().map(CurseForgeLoader.Type::toLoaderType).toList());
            else if (request.modLoaderType != null)
                prefs.includeLoaderTypes(List.of(CurseForgeLoader.Type.toLoaderType(request.modLoaderType)));
        }

        return sData.data.stream().map(x -> new ResourceCell.Link(prefs, x)).toList();
    }
}
