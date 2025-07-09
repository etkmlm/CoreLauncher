package com.laeben.corelauncher.minecraft.modding.curseforge.entity;

import com.laeben.corelauncher.api.annotation.ReturnsNull;
import com.laeben.corelauncher.minecraft.Loader;
import com.laeben.corelauncher.minecraft.modding.entity.*;
import com.laeben.corelauncher.minecraft.util.VersionUtil;

import java.time.temporal.ChronoField;
import java.util.*;

public class CurseForgeResource implements ModResource {
    private static final Comparator<CurseForgeFile> FILE_COMPARE_BY_DATE = Comparator.comparingLong(x -> x.fileDate == null ? 0 : -x.fileDate.toInstant().getLong(ChronoField.INSTANT_SECONDS));

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

    @ReturnsNull
    public List<CurseForgeFile> searchGame(List<String> versionIds, List<String> loaders){
        if (latestFiles == null)
            return null;

        ArrayList<String> versions = null;

        if (versionIds != null && !versionIds.isEmpty()){
            versions = new ArrayList<>();
            for (var vid : versionIds){
                var spl = vid.split("\\.");
                if (spl.length == 3)
                    versions.add(spl[0] + "." + spl[1]);
            }
            versions.addAll(versionIds);
        }

        var latest = new ArrayList<>(latestFiles);

        for (var file : latestFiles) {
            if (!checkAndFillFile(file, versions, loaders))
                latest.remove(file);
        }

        latest.sort(FILE_COMPARE_BY_DATE);

        return latest.stream().toList();
    }

    @ReturnsNull
    public List<CurseForgeFile> searchGame(String versionId, String loader){
        return searchGame(List.of(versionId), List.of(loader));
    }

    /**
     * Checks and fills the main version and loader if the given file is suitable with the filters.
     *
     * <p>Filters must be given null or contained at least one item.</p>
     *
     * <p>
     * Successful if the given filterLoaderTypes are null.
     * Successful if the file's game versions contain no supported loader.
     * Successful if the file's game versions contain a supported and filter-passed (present in the filterLoaderTypes) version.
     * </p>
     */
    private boolean checkAndFillFile(CurseForgeFile file, List<String> filterVersions, List<String> filterLoaderTypes){
        var all = Loader.getLoaders();

        boolean noSupportedLoaders = true;
        boolean filterLoaderMatch = false;

        boolean filterVersionMatch = false;

        int val = 0;
        String version = null;

        for (var v : file.gameVersions){
            if ((noSupportedLoaders || filterLoaderMatch) && filterVersionMatch)
                break;

            if (v.indexOf('.') > 0){ // get the newest version (only main versions)
                var n = VersionUtil.calculateVersionValue(v);
                if (n > val){
                    val = n;
                    version = v;
                }
            }

            v = v.toLowerCase();

            if (!filterVersionMatch && filterVersions != null && filterVersions.contains(v)){
                filterVersionMatch = true;
                continue;
            }

            if (!filterLoaderMatch && all.contains(v)){
                noSupportedLoaders = false;

                if (filterLoaderTypes != null){
                    for (String loader : filterLoaderTypes) {
                        if (loader.equals(v)) {
                            filterLoaderMatch = true;
                            file.mainLoader = loader;
                            break;
                        }
                    }
                }

                if (file.mainLoader == null)
                    file.mainLoader = v;
            }
        }

        file.mainGameVersion = version;

        return ((noSupportedLoaders || filterLoaderMatch) && filterVersionMatch) || (filterVersions == null && filterLoaderTypes == null);
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
