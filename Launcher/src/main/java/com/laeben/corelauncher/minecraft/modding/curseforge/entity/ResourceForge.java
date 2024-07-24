package com.laeben.corelauncher.minecraft.modding.curseforge.entity;

import com.laeben.corelauncher.minecraft.Wrapper;
import com.laeben.corelauncher.minecraft.modding.entity.*;

import java.time.temporal.ChronoField;
import java.util.*;
import java.util.stream.Collectors;

public class ResourceForge implements ModResource {

    public static class Links{
        public String websiteUrl;
    }

    public int id;
    public Links links;
    public String name;
    public String summary;
    public int classId;
    public List<ForgeCategory> categories;
    public Image logo;
    public List<Author> authors;
    public List<ForgeFile> latestFiles;

    public int downloadCount;
    public int mainFileId;
    public int primaryCategoryId;

    public List<Image> screenshots;

    public Date dateCreated;
    public Date dateModified;
    public Date dateReleased;

    public List<ForgeFile> searchGame(String versionId, String loader){
        if (latestFiles == null)
            return List.of();
        String[] spl = versionId.split("\\.");
        String base = versionId;

        if (spl.length == 3)
            base = spl[0] + "." + spl[1];
        String fBase = base;
        var f = latestFiles.stream()
                .filter(x -> Arrays.stream(x.gameVersions)
                        .anyMatch(y -> (y.equals(versionId) || y.equals(fBase))) && (loader == null || checkLoader(x.gameVersions, loader)))
                .sorted(Comparator.comparingLong(x -> x.fileDate == null ? 0 :  x.fileDate.toInstant().getLong(ChronoField.INSTANT_SECONDS)))
                .collect(Collectors.toList());
        Collections.reverse(f);

        return f.stream().toList();
    }

    private boolean checkLoader(String[] versions, String loader){
        var all = Wrapper.getWrappers();
        if (Arrays.stream(versions).noneMatch(x -> all.contains(x.toLowerCase())))
            return true;

        return Arrays.stream(versions).anyMatch(x -> x.toLowerCase().equals(loader));
    }

    @Override
    public Object getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return summary;
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.fromId(classId);
    }

    @Override
    public List<String> getCategories() {
        return categories.stream().map(a -> a.name).toList();
    }

    @Override
    public String getIcon() {
        return logo != null ? logo.thumbnailUrl : null;
    }

    @Override
    public String getURL() {
        return links != null ? links.websiteUrl : null;
    }

    @Override
    public List<String> getAuthors() {
        return authors != null ? authors.stream().map(a -> a.name).toList() : null;
    }

    @Override
    public List<String> getGameVersions() {
        return latestFiles != null ? Arrays.stream(latestFiles.get(0).gameVersions).toList() : null;
    }

    @Override
    public ModSource.Type getSourceType() {
        return ModSource.Type.CURSEFORGE;
    }

    @Override
    public Date getCreationDate() {
        return dateCreated;
    }

    /*@Override
    public List<CResource> getResourceWithDependencies(Profile profile) {
        ResourceForge resource;

        try {
            resource = CurseForge.getForge().getFullResource(profile.getVersionId(), profile.getWrapper().getType(), this);
        } catch (NoConnectionException | HttpException ignored) {
            resource = null;
        }

        if (resource == null)
            return null;

        var mod = CResource.fromForgeResourceGeneric(profile.getVersionId(), profile.getWrapperIdentifier(resource.getResourceType()), resource, null);

        List<CResource> all = null;

        try{
            all = CurseForge.getForge().getDependencies(List.of(mod), profile);
        } catch (NoConnectionException ignored) {

        }

        if (all == null)
            all = List.of(mod);

        return all;
    }

    @Override
    public List<CResource> getAllVersions(Profile profile){
        ResourceForge resource = null;
        try {
            resource = CurseForge.getForge().getFullResource(profile.getVersionId(), profile.getWrapper().getType(), this);
        } catch (NoConnectionException | HttpException ignored) {

        }

        if (resource == null)
            return null;

        var id = profile.getWrapperIdentifier(resource.getResourceType());
        var files = resource.searchGame(profile.getVersionId(), id);
        return files.stream().map(a -> (CResource)CResource.fromForgeResourceGeneric(profile.getVersionId(), id, this, a)).toList();
    }*/
}
