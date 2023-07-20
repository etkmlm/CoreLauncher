package com.laeben.corelauncher.minecraft.modding.curseforge.entities;

import com.laeben.corelauncher.minecraft.Wrapper;

import java.time.temporal.ChronoField;
import java.util.*;
import java.util.stream.Collectors;

public class Resource {

    public int id;
    public String name;
    public String summary;
    public int downloadCount;
    public int primaryCategoryId;
    public int classId;
    public int mainFileId;
    public List<Category> categories;
    public List<Author> authors;
    public Image logo;
    public List<Image> screenshots;
    public List<File> latestFiles;
    public Date dateCreated;
    public Date dateModified;
    public Date dateReleased;

    public List<File> searchGame(String versionId, String loader){
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
}
