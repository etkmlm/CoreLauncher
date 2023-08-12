package com.laeben.corelauncher.minecraft.modding.entities;

import com.google.gson.annotations.Expose;
import com.laeben.corelauncher.minecraft.modding.curseforge.entities.ForgeFile;
import com.laeben.corelauncher.minecraft.modding.curseforge.entities.ResourceForge;
import com.laeben.corelauncher.minecraft.modding.modrinth.entities.ResourceRinth;
import com.laeben.corelauncher.minecraft.modding.modrinth.entities.RinthFile;
import com.laeben.corelauncher.minecraft.modding.modrinth.entities.Version;
import javafx.scene.image.Image;

import java.util.Date;
import java.util.List;
import java.util.Objects;

public class CResource {
    public String source;

    public Object id;
    public String name;
    @Expose(serialize = false)
    public String desc;
    public String logoUrl;
    public List<String> authors;
    public String fileUrl;
    public String fileName;
    public transient List<CResource> dependencies;
    public transient List<ForgeFile.Module> forgeModules;
    @Expose(serialize = false)
    public Date createDate;

    public CResource(SourceType type){
        this.source = type.getId();
    }

    public CResource(){

    }

    public static CResource asDependency(Object id){
        var res = new CResource();
        res.id = id;

        return res;
    }

    public boolean isForge(){
        return source == null || source.equals("curseforge");
    }

    public boolean isModrinth(){
        return source != null && source.equals("modrinth");
    }

    protected static <T extends CResource> T fromForgeResource(T p, String versionId, String loader, ResourceForge r){
        p.id = r.id;
        p.name = r.name;
        if (r.authors != null)
            p.authors = r.authors.stream().map(x -> x.name).toList();
        p.createDate = r.dateCreated;
        p.desc = r.summary;
        p.setSource(SourceType.CURSEFORGE);

        if (r.logo != null)
            p.logoUrl = r.logo.url;

        var files = r.searchGame(versionId, loader);
        if (files.isEmpty())
            return p;

        p.setFile(fromForgeFile(files.get(0), (int)p.id));
        return p;
    }

    public static CResource fromForgeFile(ForgeFile file, int modId){
        var res = new CResource();
        res.fileUrl = file.downloadUrl != null ? file.downloadUrl : "https://www.curseforge.com/api/v1/mods/" + modId + "/files/" + file.id + "/download";
        res.fileName = file.fileName;
        res.forgeModules = file.getModules();
        res.dependencies = file.dependencies.stream().map(x -> CResource.asDependency(x.modId)).toList();

        return res;
    }

    public void setSource(SourceType type){
        this.source = type.getId();
    }

    public static CResource fromRinthFile(RinthFile file){
        var res = new CResource();
        res.fileUrl = file.url;
        res.fileName = file.filename;
        res.forgeModules = List.of();

        return res;
    }

    protected static <T extends CResource> T fromRinthResource(T p, ResourceRinth r, Version v){
        p.id = r.getId();
        p.name = r.title;
        p.authors = List.of(r.team);
        p.desc = r.description;
        p.logoUrl = r.icon;
        p.dependencies = v.getDependencies().stream().map(x -> CResource.asDependency(x.versionId)).filter(x -> x.id != null).toList();
        p.source = SourceType.MODRINTH.getId();

        p.setFile(CResource.fromRinthFile(v.getFile()));

        return p;
    }

    public Image getIcon(){
        if (logoUrl == null || logoUrl.isBlank())
            return null;
        return logoUrl.startsWith("/") ? new Image(Objects.requireNonNull(CResource.class.getResourceAsStream(logoUrl))) : new Image(logoUrl, true);
    }

    public static <T extends CResource> T fromForgeResourceGeneric(String vId, String loader, ResourceForge r){
        if (r.classId == ResourceType.MOD.getId())
            return (T) Mod.fromForgeResource(vId, loader, r);
        else if (r.classId == ResourceType.MODPACK.getId())
            return (T) Modpack.fromForgeResource(vId, loader, r);
        else if (r.classId == ResourceType.RESOURCE.getId())
            return (T) Resourcepack.fromForgeResource(vId, loader, r);
        else if (r.classId == ResourceType.WORLD.getId())
            return (T) World.fromResource(vId, loader, r);
        else
            return (T) fromForgeResource(new CResource(), vId, loader, r);
    }

    public static <T extends CResource> T fromRinthResourceGeneric(ResourceRinth r, Version v){
        return switch (r.projectType) {
            case "mod" -> (T) Mod.fromRinthResource(r, v);
            case "modpack" -> (T) Modpack.fromRinthResource(r, v);
            case "resourcepack" -> (T) Resourcepack.fromRinthResource(r, v);
            case "shader" -> (T) Shader.fromRinthResource(r, v);
            default -> (T) fromRinthResource(new CResource(), r, v);
        };
    }
    public void setFile(CResource file){
        fileUrl = file.fileUrl;
        fileName = file.fileName;
        forgeModules = file.forgeModules;
        if (file.dependencies != null)
            dependencies = file.dependencies;
    }

    @Override
    public boolean equals(Object obj){
        return obj instanceof CResource res && ((res.id != null && res.id.equals(id)) || res.fileName.equals(fileName));
    }
}
