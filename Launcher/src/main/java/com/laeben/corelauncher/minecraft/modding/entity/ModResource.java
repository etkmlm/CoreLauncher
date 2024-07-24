package com.laeben.corelauncher.minecraft.modding.entity;

import java.util.Date;
import java.util.List;

public interface ModResource {

    Object getId();
    String getName();
    String getDescription();
    ResourceType getResourceType();
    List<String> getCategories();
    String getIcon();
    String getURL();
    List<String> getAuthors();
    List<String> getGameVersions();
    ModSource.Type getSourceType();
    Date getCreationDate();

    //List<CResource> getResourceWithDependencies(Profile p);
    //List<CResource> getAllVersions(Profile p);
}
