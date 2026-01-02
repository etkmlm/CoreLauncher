package com.laeben.corelauncher.ui.controller.browser;

import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.minecraft.modding.entity.LoaderType;
import com.laeben.corelauncher.minecraft.modding.entity.ModSide;
import com.laeben.corelauncher.minecraft.modding.entity.ResourcePreferences;
import com.laeben.corelauncher.minecraft.modding.entity.ResourceType;
import com.laeben.corelauncher.minecraft.modding.modrinth.Modrinth;
import com.laeben.corelauncher.minecraft.modding.modrinth.entity.*;

import java.util.List;

public class ModrinthSearch implements Search<Index> {
    private ModrinthSearchRequest request;

    private final Profile profile;

    private int totalPages;

    public ModrinthSearch(Profile profile){
        this.profile = profile;
    }

    public void setMainType(ResourceType type){
        request.builder.add(Facet.get("project_type", type.getName()).setId("type"));
        if (profile != null){
            request.builder.setLoader(profile.getLoaderIdentifier(type));
            if (profile.getLoader().getType().isNative())
                request.builder.add(Facet.not("categories", List.of("modded")).setId("modded"));
            else
                request.builder.remove("modded");
        }
        else{
            request.builder.setLoader(null);
            request.builder.remove("modded");
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
        request.builder.add(Facet.get("categories", name).setId("category"));
    }

    public void setCategories(List<String> names){
        request.builder.add(Facet.and("categories", names.stream().toList()).setId("category"));
        //request.builder.add(Facet.or("categories", names.stream().map(Object::toString).toList()).setId("category"));
    }

    public void setGameVersions(List<String> versions){
        //request.builder.setGameVersion(null);

        if (profile != null)
            return;

        if (versions != null)
            request.builder.add(Facet
                    .or("versions", versions)
                    .setId("versions")
            );
        else
            request.builder.remove("versions");
    }

    private static final List<String> SELECTED_SIDE = List.of("optional", "required");
    private static final List<String> UNSELECTED_SIDE = List.of("optional", "unsupported");
    private static final List<String> BOTH_SIDES = List.of("required");

    @Override
    public void setSides(List<ModSide> sides) {
        if (sides == null || sides.isEmpty()){
            request.builder.remove("client_side");
            request.builder.remove("server_side");
            return;
        }

        boolean isClient = sides.contains(ModSide.CLIENT);
        boolean isServer = sides.contains(ModSide.SERVER);
        request.builder.add(Facet.or("client_side", isClient && isServer ? BOTH_SIDES : (isClient ? SELECTED_SIDE : UNSELECTED_SIDE)).setId("client_side"));
        request.builder.add(Facet.or("server_side", isClient && isServer ? BOTH_SIDES : (isServer ? SELECTED_SIDE : UNSELECTED_SIDE)).setId("server_side"));
    }

    @Override
    public void setLoaders(List<LoaderType> loaders) {
        //request.builder.setLoader(null);
        if (profile != null)
            return;

        if (loaders != null)
            request.builder.add(Facet
                    .or("categories", loaders.stream().map(LoaderType::getIdentifier).toList())
                    .setId("loaders")
            );
        else
            request.builder.remove("loaders");
    }

    public void reset(){
        request = new ModrinthSearchRequest();
        request.limit = 50;

        if (profile != null){
            request.builder.setGameVersion(profile.getVersionId());
            request.builder.setLoader(profile.getLoader().getType().getIdentifier());
        }
        else{
            request.builder.setGameVersion(null);
            request.builder.setLoader(null);
        }
        // remove multiple game versions
        request.builder.remove("versions");

        // remove multiple loaders
        request.builder.remove("loaders");
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

            var loader = request.builder.get("loader");
            var loaders = loader == null ? request.builder.get("loaders") : loader;

            if (loaders != null && loaders.values != null && !loaders.values.isEmpty()){
                prefs.includeLoaderTypes(loaders.values.stream().map(LoaderType.TYPES::get).toList());
            }

            var ver = request.builder.get("version");
            var vers = ver == null ? request.builder.get("versions") : ver;

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
