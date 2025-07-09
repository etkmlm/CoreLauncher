package com.laeben.corelauncher.ui.controller.browser;

import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.minecraft.modding.entity.LoaderType;
import com.laeben.corelauncher.minecraft.modding.entity.ResourcePreferences;
import com.laeben.corelauncher.minecraft.modding.entity.ResourceType;
import com.laeben.corelauncher.minecraft.modding.modrinth.Modrinth;
import com.laeben.corelauncher.minecraft.modding.modrinth.entity.*;

import java.util.List;

public class ModrinthSearch implements Search<Index> {
    private ModrinthSearchRequest request;
    public FacetBuilder builder;

    private final Profile profile;

    private int totalPages;

    public ModrinthSearch(Profile profile){
        this.profile = profile;
    }

    public void setMainType(ResourceType type){
        builder.add(Facet.get("project_type", type.getName()).setId("type"));
        if (profile != null){
            builder.setLoader(profile.getLoaderIdentifier(type));
            if (profile.getLoader().getType().isNative())
                builder.add(Facet.not("categories", List.of("modded")).setId("modded"));
            else
                builder.remove("modded");
        }
        else{
            builder.setLoader(null);
            builder.remove("modded");
        }
    }

    @Override
    public void setPageIndex(int index) {
        request.offset = (index - 1) * 50;
    }

    public void setSortOrder(boolean asc){

    }

    public void setSortField(Index index){
        request.setIndex(index);
    }

    public void setCategory(String name){
        builder.add(Facet.get("categories", name).setId("category"));
    }

    public void setCategories(List<String> names){
        builder.add(Facet.and("categories", names.stream().toList()).setId("category"));
        //builder.add(Facet.or("categories", names.stream().map(Object::toString).toList()).setId("category"));
    }

    public void setGameVersions(List<String> versions){
        //builder.setGameVersion(null);

        if (profile != null)
            return;

        if (versions != null)
            builder.add(Facet
                    .or("versions", versions)
                    .setId("versions")
            );
        else
            builder.remove("versions");
    }

    @Override
    public void setLoaders(List<LoaderType> loaders) {
        //builder.setLoader(null);
        if (profile != null)
            return;

        if (loaders != null)
            builder.add(Facet
                    .or("categories", loaders.stream().map(LoaderType::getIdentifier).toList())
                    .setId("loaders")
            );
        else
            builder.remove("loaders");
    }

    public void reset(){
        request = new ModrinthSearchRequest();
        request.limit = 50;
        request.facets = builder = new FacetBuilder();

        if (profile != null){
            builder.setGameVersion(profile.getVersionId());
            builder.setLoader(profile.getLoader().getType().getIdentifier());
        }
        else{
            builder.setGameVersion(null);
            builder.setLoader(null);
        }
        // remove multiple game versions
        builder.remove("versions");

        // remove multiple loaders
        builder.remove("loaders");
    }


    public int getTotalPages() {
        return totalPages;
    }

    public List<ResourceCell.Link> search(String query){
        request.setQuery(query);
        ModrinthSearchResponse resp = null;
        try{
            resp = Modrinth.getModrinth().search(request);
        } catch (NoConnectionException | HttpException ignored) {

        }

        if (resp == null)
            return List.of();


        totalPages = (int)Math.ceil(resp.totalHits / 50.0);

        ResourcePreferences prefs;

        if (profile != null)
            prefs = ResourcePreferences.fromProfile(profile);
        else {
            prefs = ResourcePreferences.empty();

            var loader = request.facets.get("loader");
            var loaders = loader == null ? request.facets.get("loaders") : loader;

            if (loaders != null && loaders.values != null && !loaders.values.isEmpty()){
                prefs.includeLoaderTypes(loaders.values.stream().map(LoaderType.TYPES::get).toList());
            }

            var ver = request.facets.get("version");
            var vers = ver == null ? request.facets.get("versions") : ver;

            if (vers != null && vers.values != null && !vers.values.isEmpty()){
                prefs.includeGameVersions(vers.values);
            }
        }

        return resp.hits.stream().map(x -> new ResourceCell.Link(prefs, x)).toList();
    }

    public ModrinthSearchRequest getSearch(){
        return request;
    }
}
