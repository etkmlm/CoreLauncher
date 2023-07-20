package com.laeben.corelauncher.minecraft.modding.entities;

import com.laeben.corelauncher.minecraft.modding.curseforge.entities.ClassType;
import com.laeben.corelauncher.minecraft.modding.curseforge.entities.File;
import com.laeben.corelauncher.minecraft.modding.curseforge.entities.Resource;
import com.google.gson.annotations.Expose;
import javafx.scene.image.Image;

import java.util.Date;
import java.util.List;
import java.util.Objects;

public class CResource {
    public int id;
    public int classId;
    public String name;
    @Expose(serialize = false)
    public String desc;
    public String logoUrl;
    public List<String> authors;
    public String fileUrl;
    public String fileName;
    public transient List<File.Dependency> dependencies;
    public transient List<File.Module> modules;
    @Expose(serialize = false)
    public Date createDate;

    protected static <T extends CResource> T fromResource(T p, String versionId, String loader, Resource r){
        p.id = r.id;
        p.classId = r.classId;
        p.name = r.name;
        if (r.authors != null)
            p.authors = r.authors.stream().map(x -> x.name).toList();
        p.createDate = r.dateCreated;
        p.desc = r.summary;
        if (r.logo != null)
            p.logoUrl = r.logo.url;

        var files = r.searchGame(versionId, loader);
        if (files.size() == 0)
            return p;
        p.setFile(files.get(0));
        return p;
    }

    public Image getIcon(){
        if (logoUrl == null || logoUrl.isBlank())
            return null;
        return logoUrl.startsWith("/") ? new Image(Objects.requireNonNull(CResource.class.getResourceAsStream(logoUrl))) : new Image(logoUrl, true);
    }

    public static <T extends CResource> T fromResourceGeneric(String vId, String loader, Resource r){
        if (r.classId == ClassType.MOD.getId())
            return (T) Mod.fromResource(vId, loader, r);
        else if (r.classId == ClassType.MODPACK.getId())
            return (T) Modpack.fromResource(vId, loader, r);
        else if (r.classId == ClassType.RESOURCE.getId())
            return (T) Resourcepack.fromResource(vId, loader, r);
        else if (r.classId == ClassType.WORLD.getId())
            return (T) World.fromResource(vId, loader, r);
        else
            return (T) fromResource(new CResource(), vId, loader, r);
    }
    public void setFile(File file){
        fileUrl = file.downloadUrl != null ? file.downloadUrl : "https://www.curseforge.com/api/v1/mods/" + id + "/files/" + file.id + "/download";
        fileName = file.fileName;
        dependencies = file.getDependencies();
        modules = file.getModules();
    }
}
