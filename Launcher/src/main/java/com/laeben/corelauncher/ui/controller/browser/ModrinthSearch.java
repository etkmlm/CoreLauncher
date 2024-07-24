package com.laeben.corelauncher.ui.controller.browser;

import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.minecraft.modding.entity.ResourceType;
import com.laeben.corelauncher.minecraft.modding.modrinth.Modrinth;
import com.laeben.corelauncher.minecraft.modding.modrinth.entity.*;

import java.util.List;

public class ModrinthSearch implements Search<Index> {
    private SearchRinth searchRinth;
    public FacetBuilder builder;

    private final Profile profile;

    private int totalPages;

    public ModrinthSearch(Profile profile){
        this.profile = profile;
    }

    public void setMainType(ResourceType type){
        builder.add(Facet.get("project_type", type.getName()).setId("type"));
        builder.setLoader(profile.getWrapperIdentifier(type));
        if (profile.getWrapper().getType().isNative())
            builder.add(Facet.not("categories", List.of("modded")).setId("modded"));
        else
            builder.remove("modded");
    }

    @Override
    public void setPageIndex(int index) {
        searchRinth.offset = (index - 1) * 50;
    }

    public void setSortOrder(boolean asc){

    }

    public void setSortField(Index index){
        searchRinth.setIndex(index);
    }

    public void setCategory(String name){
        builder.add(Facet.get("categories", name).setId("category"));
    }

    public void setCategories(List<Object> names){
        builder.add(Facet.and("categories", names.stream().map(Object::toString).toList()).setId("category"));
        //builder.add(Facet.or("categories", names.stream().map(Object::toString).toList()).setId("category"));
    }

    public void reset(){
        searchRinth = new SearchRinth();
        searchRinth.limit = 50;
        searchRinth.facets = builder = new FacetBuilder();

        builder.setGameVersion(profile.getVersionId());
        builder.setLoader(profile.getWrapper().getType().getIdentifier());
    }


    public int getTotalPages() {
        return totalPages;
    }

    public List<ResourceCell.Link> search(String query){
        searchRinth.setQuery(query);
        SearchResponseRinth resp = null;
        try{
            resp = Modrinth.getModrinth().search(searchRinth);
        } catch (NoConnectionException | HttpException ignored) {

        }

        if (resp == null)
            return List.of();


        totalPages = (int)Math.ceil(resp.totalHits / 50.0);
        return resp.hits.stream().map(x -> new ResourceCell.Link(profile, x)).toList();
    }

    public SearchRinth getSearch(){
        return searchRinth;
    }
}
