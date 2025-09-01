package com.laeben.corelauncher.minecraft.modding.entity;

import java.util.Date;
import java.util.List;

public interface ModResource {

    Object getId();
    String getName();
    String getDescription();
    ResourceType getResourceType();
    String[] getCategories();
    String getIcon();
    String getURL();
    String[] getAuthors();
    String[] getGameVersions();
    LoaderType[] getLoaders(List<String> gameVersions); // we are taking gameVersions because some versions may not be supporting some loaders
    ModSource.Type getSourceType();
    Date getCreationDate();

    static <T> T getGlobalSafeLoaders(ResourceType resType, T loader){
        return resType == null || resType.isGlobal() ? null : loader;
    }

    //List<CResource> getResourceWithDependencies(Profile p);
    //List<CResource> getAllVersions(Profile p);
}
