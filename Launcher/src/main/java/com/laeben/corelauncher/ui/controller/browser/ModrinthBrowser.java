package com.laeben.corelauncher.ui.controller.browser;

import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.corelauncher.data.Translator;
import com.laeben.corelauncher.minecraft.modding.entities.ResourceType;
import com.laeben.corelauncher.minecraft.modding.modrinth.Modrinth;
import com.laeben.corelauncher.minecraft.modding.modrinth.entities.*;
import com.laeben.corelauncher.utils.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModrinthBrowser extends ModBrowser{

    private SearchRinth searchRinth;
    private FacetBuilder builder;

    private List<RinthCategory> categories;

    private static final Map<Index, String> sTranslates = new HashMap<>(){{
       put(Index.RELEVANCE, "mods.sort.relevance");
       put(Index.NEWEST, "mods.sort.newest");
       put(Index.FOLLOWS, "mods.sort.follows");
       put(Index.UPDATED, "mods.sort.lastUpdated");
       put(Index.DOWNLOADS, "mods.sort.totalDownload");
    }};

    @Override
    public void init() {
        cbMainCategories.getItems().clear();
        cbMainCategories.getItems().addAll(
                Translator.translate("mods.type.modpack"),
                Translator.translate("mods.type.mod"),
                Translator.translate("mods.type.resource"),
                Translator.translate("mods.type.shader"));

        cstSort.setPercentWidth(0);
        cstCat.setPercentWidth(22);
        cstSortBy.setPercentWidth(22);
        cstMainCat.setPercentWidth(30);

        cbCategories.valueProperty().addListener(a -> {
            var value = cbCategories.getValue();

            if (value == null || value.isEmpty())
                return;

            if (value.equals("...")){
                builder.remove("category");
                return;
            }

            int index = cbCategories.getItems().indexOf(value);
            var category = categories.get(index);
            builder.add(Facet.get("categories", category.name).setId("category"));
        });

        var sorts = Arrays.stream(Index.class.getEnumConstants()).toList();

        cbSortBy.getItems().clear();
        cbSortBy.getItems().addAll(sorts.stream().map(x -> Translator.translate(sTranslates.get(x))).toList());
        cbSortBy.valueProperty().addListener(a -> {
            var value = cbSortBy.getValue();

            if (value == null || value.isEmpty())
                return;

            int i = cbSortBy.getItems().indexOf(value);
            var index = sorts.get(i);
            searchRinth.index = index.getId();
        });

        txtQuery.textProperty().addListener(a -> searchRinth.setQuery(txtQuery.getText()));
    }

    @Override
    public <T extends BMod> T cellFactory() {
        return (T) new MBMod();
    }

    @Override
    public void reset() {
        searchRinth = new SearchRinth();
        searchRinth.limit = 50;
        searchRinth.facets = builder = new FacetBuilder();

        builder.setGameVersion(profile.getVersionId());
        builder.setLoader(profile.getWrapper().getIdentifier());

        search();
    }

    @Override
    public void search() {
        resources.clear();
        SearchResponseRinth resp = null;
        try{
            resp = Modrinth.getModrinth().search(searchRinth);
        } catch (NoConnectionException | HttpException ignored) {

        }

        if (resp == null)
            return;

        resources.addAll(resp.hits.stream().map(x -> new LModLink<>(profile, x)).toList());
    }

    @Override
    public void onMainCategorySelected(int index) {
        var type = switch (index){
            case 0 -> ResourceType.MODPACK;
            case 2 -> ResourceType.RESOURCE;
            case 3 -> ResourceType.SHADER;
            default -> ResourceType.MOD;
        };

        categories = Modrinth.getModrinth().getCategories(type.getName());
        cbCategories.getItems().clear();
        cbCategories.getItems().add("...");
        cbCategories.getItems().addAll(categories.stream().map(x -> StringUtils.toUpperFirst(x.name)).toList());

        builder.add(Facet.get("project_type", type.getName()).setId("type"));

        builder.setLoader(type.isGlobal() ? null : profile.getWrapper().getIdentifier());
    }
}
