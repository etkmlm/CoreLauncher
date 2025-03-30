package com.laeben.corelauncher.minecraft.modding.curseforge.entity;

import com.laeben.corelauncher.minecraft.Wrapper;
import com.laeben.corelauncher.minecraft.modding.entity.*;

import java.time.temporal.ChronoField;
import java.util.*;
import java.util.stream.Collectors;

public class CurseForgeResource implements ModResource {

    public static class Links{
        public String websiteUrl;
    }

    public int id;
    public Links links;
    public String name;
    public String summary;
    public int classId;
    public List<CurseForgeCategory> categories;
    public Image logo;
    public List<Author> authors;
    public List<CurseForgeFile> latestFiles;

    public int downloadCount;
    public int mainFileId;
    public int primaryCategoryId;

    public List<Image> screenshots;

    public Date dateCreated;
    public Date dateModified;
    public Date dateReleased;

    public List<CurseForgeFile> searchGame(String versionId, String loader){
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
    public String[] getCategories() {
        return categories.stream().map(a -> a.name).toArray(String[]::new);
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
    public String[] getAuthors() {
        return authors != null ? authors.stream().map(a -> a.name).toArray(String[]::new) : null;
    }

    @Override
    public String[] getGameVersions() {
        if (latestFiles == null)
            return null;
        List<String> versions = new ArrayList<>();
        for (var f : latestFiles){
            for (var x : f.gameVersions){
                if (x.contains(".") && !x.contains("-"))
                    versions.add(x);
            }
        }
        return versions.stream().distinct().toArray(String[]::new);
    }

    @Override
    public LoaderType[] getLoaders() {
        if (latestFiles == null)
            return null;

        var lst = new ArrayList<LoaderType>();
        for (var f : latestFiles){
            if (f.gameVersions == null)
                continue;
            for (var v : f.gameVersions){
                var k = v.toLowerCase(Locale.US);
                if (LoaderType.TYPES.containsKey(k)){
                    lst.add(LoaderType.TYPES.get(k));
                }
            }
        }

        return lst.stream().distinct().toArray(LoaderType[]::new);
    }

    @Override
    public ModSource.Type getSourceType() {
        return ModSource.Type.CURSEFORGE;
    }

    @Override
    public Date getCreationDate() {
        return dateCreated;
    }
}
