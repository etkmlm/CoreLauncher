package com.laeben.corelauncher.ui.controller.browser;

import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.corelauncher.data.Translator;
import com.laeben.corelauncher.minecraft.modding.curseforge.CurseForge;
import com.laeben.corelauncher.minecraft.modding.curseforge.entities.*;
import com.laeben.corelauncher.minecraft.modding.entities.ResourceType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ForgeBrowser extends ModBrowser {


    private static final Map<ModsSearchSortField, String> sTranslates = new HashMap<>(){{
       put(ModsSearchSortField.AUTHOR, "mods.sort.author");
       put(ModsSearchSortField.CATEGORY, "mods.sort.category");
       put(ModsSearchSortField.NAME, "mods.sort.name");
       put(ModsSearchSortField.FEATURED, "mods.sort.featured");
       put(ModsSearchSortField.GAME_VERSION, "mods.sort.gameVersion");
       put(ModsSearchSortField.LAST_UPDATED, "mods.sort.lastUpdated");
       put(ModsSearchSortField.POPULARITY, "mods.sort.popularity");
       put(ModsSearchSortField.TOTAL_DOWNLOADS, "mods.sort.totalDownload");
    }};

    private List<ForgeCategory> categories;

    private SearchForge searchForge;
    //private SearchResponseForge.Pagination pagination;

    public void init(){
        cbMainCategories.getItems().clear();
        cbMainCategories.getItems().addAll(
                Translator.translate("mods.type.modpack"),
                Translator.translate("mods.type.mod"),
                Translator.translate("mods.type.resource"),
                Translator.translate("mods.type.world"));

        cbCategories.valueProperty().addListener(a -> {
            var value = cbCategories.getValue();

            if (value == null || value.isEmpty())
                return;

            var category = categories.stream().filter(x -> x.name.equals(value)).findFirst();
            searchForge.categoryId = category.map(category1 -> category1.id).orElse(-1);
        });

        var sorts = ModsSearchSortField.class.getEnumConstants();
        cbSortBy.getItems().clear();
        var fx = Arrays.stream(sorts).filter(x -> x != ModsSearchSortField.NONE).toList();
        cbSortBy.getItems().addAll(fx.stream().map(x -> Translator.translate(sTranslates.get(x))).toList());
        cbSortBy.valueProperty().addListener(a -> {
            var value = cbSortBy.getValue();

            if (value == null || value.isEmpty())
                return;

            int index = cbSortBy.getItems().indexOf(value);

            searchForge.sortField = fx.get(index);
        });

        cbSort.getItems().clear();
        cbSort.getItems().addAll(Translator.translate("ascending"), Translator.translate("descending"));
        cbSort.valueProperty().addListener(a -> {
            var value = cbSort.getValue();

            if (value == null || value.isEmpty())
                return;

            int index = cbSort.getItems().indexOf(value);

            searchForge.setSortOrder(index == 0);
        });

        txtQuery.textProperty().addListener(a -> searchForge.setSearchFilter(txtQuery.getText()));
    }

    @Override
    public <T extends BMod> T cellFactory() {
        return (T) new FBMod();
    }

    @Override
    public void reset(){
        searchForge = new SearchForge();
        searchForge.classId = ResourceType.MOD.getId();
        searchForge.gameVersion = profile.getVersionId();
        searchForge.modLoaderType = profile.getWrapper().getType();
        searchForge.sortField = ModsSearchSortField.POPULARITY;
        searchForge.sortOrder = "desc";

        search();
    }

    @Override
    public void search(){
        resources.clear();
        SearchResponseForge sData = null;
        try{
            sData = CurseForge.getForge().search(searchForge);
        } catch (NoConnectionException | HttpException ignored) {

        }


        if (sData == null)
            return;

        resources.addAll(sData.data.stream().map(x -> new LModLink(profile, x)).toList());
        //pagination = sData.pagination;
    }

    @Override
    public void onMainCategorySelected(int index) {
        var type = switch (index){
            case 0 -> ResourceType.MODPACK;
            case 2 -> ResourceType.RESOURCE;
            case 3 -> ResourceType.WORLD;
            default -> ResourceType.MOD;
        };

        if (type == ResourceType.RESOURCE || type == ResourceType.WORLD){
            if (profile.getWrapper().getType() == CurseWrapper.Type.ANY)
                searchForge.modLoaderType = null;
            else
                searchForge.modLoaderType = profile.getWrapper().getType();
        }
        else
            searchForge.modLoaderType = profile.getWrapper().getType();

        categories = CurseForge.getForge().getCategories().stream().filter(x -> x.classId == type.getId()).toList();
        cbCategories.getItems().add("...");
        cbCategories.getItems().addAll(categories.stream().map(x -> x.name).toList());


        searchForge.classId = type.getId();
    }
}
