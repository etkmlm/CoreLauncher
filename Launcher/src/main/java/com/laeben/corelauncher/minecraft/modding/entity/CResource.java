package com.laeben.corelauncher.minecraft.modding.entity;

import com.google.gson.annotations.Expose;
import com.laeben.corelauncher.minecraft.modding.curseforge.entity.ForgeFile;
import com.laeben.corelauncher.minecraft.modding.curseforge.entity.ResourceForge;
import com.laeben.corelauncher.minecraft.modding.modrinth.entity.ResourceRinth;
import com.laeben.corelauncher.minecraft.modding.modrinth.entity.RinthFile;
import com.laeben.corelauncher.minecraft.modding.modrinth.entity.Version;
import com.laeben.corelauncher.util.ImageUtil;
import com.laeben.corelauncher.util.entity.ImageTask;

import java.util.Date;
import java.util.List;

public class CResource implements Comparable<CResource> {
    public ModSource.Type source;

    @Expose
    public String desc;
    @Expose
    public List<String> authors;
    @Expose
    public Date createDate;
    @Expose
    public Date fileDate;

    public Object id;
    public String name;
    public String logoUrl;

    /* These fields are null when isMeta = true */
    public Object fileId;
    public String fileUrl;
    public String fileName;

    public transient List<CResource> dependencies;
    public transient List<ForgeFile.Module> forgeModules;

    protected transient boolean isMeta = true;
    /**/

    public CResource(){

    }

    public CResource(ModSource.Type type){
        setSource(type);
    }

    public int getIntId(){
        return id == null ? 0 : (id instanceof Double ? (int)(double)id : (int)id);
    }

    public Object getId(){
        return id;
    }

    public static CResource asDependency(Object id){
        var res = new CResource();
        res.id = id;

        return res;
    }

    public static CResource asDependencyFile(Object id){
        var res = new CResource();
        res.fileId = id;

        return res;
    }

    public boolean isForge(){
        return (source == null && (id instanceof Integer || id instanceof Double)) || (source == ModSource.Type.CURSEFORGE);
    }
    public boolean isModrinth(){
        return (source == null && id instanceof String) || source == ModSource.Type.MODRINTH;
    }

    public ResourceType getType(){
        return ResourceType.MOD;
    }

    protected static <T extends CResource> T fromForgeResource(T p, String versionId, String loader, ResourceForge r, ForgeFile file){
        p.id = r.id;
        p.name = r.name;
        if (r.authors != null)
            p.authors = r.authors.stream().map(x -> x.name).toList();
        p.createDate = r.dateCreated;
        p.desc = r.summary;
        p.setSource(ModSource.Type.CURSEFORGE);

        if (r.logo != null)
            p.logoUrl = r.logo.url;

        p.isMeta = versionId == null;

        if (!p.isMeta){
            CResource f;
            if (file == null){
                var files = r.searchGame(versionId, loader);
                if (files.isEmpty())
                    return p;

                f = fromForgeFile(files.get(0), p.getIntId());
            }
            else
                f = fromForgeFile(file, p.getIntId());

            p.setFile(f);
        }

        return p;
    }
    protected static <T extends CResource> T fromRinthResource(T p, ResourceRinth r, Version v){
        p.id = r.getId();
        p.name = r.title;
        p.authors = r.getAuthors();
        p.desc = r.description;
        p.logoUrl = r.icon;
        p.createDate = r.getCreationDate();

        p.isMeta = v == null;

        if (!p.isMeta){
            p.dependencies = v.getDependencies().stream()
                    .filter(a -> a.isRequired() || a.isEmbedded())
                    .map(x -> x.versionId != null ? CResource.asDependencyFile(x.versionId) : CResource.asDependency(x.projectId))
                    .filter(x -> x.id != null || x.fileId != null)
                    .toList();

            p.setFile(CResource.fromRinthFile(v.getFile(), v.getPublished()));
        }

        p.setSource(ModSource.Type.MODRINTH);

        return p;
    }

    public static <T extends CResource> T fromForgeResourceGeneric(String vId, String loader, ResourceForge r, ForgeFile file){
        if (r.classId == ResourceType.MOD.getId())
            return (T) Mod.fromForgeResource(vId, loader, r, file);
        else if (r.classId == ResourceType.MODPACK.getId())
            return (T) Modpack.fromForgeResource(vId, loader, r, file);
        else if (r.classId == ResourceType.RESOURCE.getId())
            return (T) Resourcepack.fromForgeResource(vId, loader, r, file);
        else if (r.classId == ResourceType.WORLD.getId())
            return (T) World.fromResource(vId, loader, r);
        else if (r.classId == ResourceType.SHADER.getId())
            return (T) Shader.fromForgeResource(vId, loader, r, file);
        else
            return (T) fromForgeResource(new CResource(), vId, loader, r, file);
    }
    public static <T extends CResource> T fromRinthResourceGeneric(ResourceRinth r, Version v){
        if (r.projectType.equals(ResourceType.MOD.getName()))
            return (T) Mod.fromRinthResource(r, v);
        else if (r.projectType.equals(ResourceType.MODPACK.getName()))
            return (T) Modpack.fromRinthResource(r, v);
        else if (r.projectType.equals(ResourceType.RESOURCE.getName()))
            return (T) Resourcepack.fromRinthResource(r, v);
        else if (r.projectType.equals(ResourceType.SHADER.getName()))
            return (T) Shader.fromRinthResource(r, v);
        else
            return (T) fromRinthResource(new CResource(), r, v);
    }

    public static CResource fromForgeFile(ForgeFile file, int modId){
        var res = new CResource();
        res.fileId = file.id;
        res.fileUrl = file.downloadUrl != null ? file.downloadUrl : "https://www.curseforge.com/api/v1/mods/" + modId + "/files/" + file.id + "/download";
        res.fileName = file.fileName;
        res.forgeModules = file.getModules();
        res.dependencies = file.getDependencies().stream().map(x -> CResource.asDependency(x.modId)).toList();
        res.fileDate = file.fileDate;

        res.setSource(ModSource.Type.CURSEFORGE);

        return res;
    }
    public static CResource fromRinthFile(RinthFile file, Date fileDate){
        var res = new CResource();

        res.fileId = file.id;
        res.fileDate = fileDate;
        res.fileUrl = file.url;
        res.fileName = file.filename;
        res.forgeModules = List.of();

        res.setSource(ModSource.Type.MODRINTH);

        return res;
    }

    public ImageTask getIcon(){
        return getIcon(-1, -1);
    }

    public ImageTask getIcon(double w, double h){
        if (logoUrl == null || logoUrl.isBlank()){
            return ImageTask.NULL;
        }

        if (!logoUrl.startsWith("/")){
            return ImageUtil.getNetworkImage(logoUrl, w, h);
        }
        else
            return ImageTask.fromImage(ImageUtil.getLocalImage(logoUrl, w, h));
    }
    
    /*public Image getIcon(double w, double h){
        if (logoUrl == null || logoUrl.isBlank())
            return null;
        return logoUrl.startsWith("/") ? new Image(Objects.requireNonNull(CResource.class.getResourceAsStream(logoUrl)), w, h, false, false) : new Image(logoUrl, w, h, false, false, true);
    }
    public Image getIcon(){
        if (logoUrl == null || logoUrl.isBlank())
            return null;
        return logoUrl.startsWith("/") ? new Image(Objects.requireNonNull(CResource.class.getResourceAsStream(logoUrl))) : new Image(logoUrl, true);
    }*/

    public void setFile(CResource file){
        fileId = file.id;
        fileDate = file.fileDate;
        fileUrl = file.fileUrl;
        fileName = file.fileName;
        forgeModules = file.forgeModules;
        if (file.dependencies != null)
            dependencies = file.dependencies;
    }

    public void setSource(ModSource.Type type){
        this.source = type;
    }

    public ModSource getSource(){
        return getSourceType().getSource();
    }

    public ModSource.Type getSourceType(){
        return source == null ? ModSource.Type.CURSEFORGE : source;
    }


    public void setMeta(boolean v){
        isMeta = v;
    }
    public boolean isMeta(){
        return isMeta;
    }

    /*@Override
    public boolean equals(Object obj){
        return obj instanceof CResource res && ((res.id != null && (res.id instanceof Double || res.id instanceof Integer ? res.getIntId() == getIntId() : res.id.equals(id))) || res.fileName.equals(fileName));
    }*/

    @Override
    public boolean equals(Object obj){
        return obj instanceof CResource res && res.id != null && (res.fileId == null || fileId == null || res.fileId.equals(fileId)) && res.fileName.equals(fileName) && (res.id instanceof Double || res.id instanceof Integer ? res.getIntId() == getIntId() : res.id.equals(id));
    }

    public boolean isSameResource(Object obj){
        return obj instanceof CResource res && ((res.fileName != null && res.fileName.equals(fileName)) || (res.id != null && ((res.id instanceof Double || res.id instanceof Integer) && (id instanceof Double || id instanceof Integer) ? res.getIntId() == getIntId() : res.id.equals(id))));
    }

    @Override
    public int compareTo(CResource o){
        return fileDate == null || o.fileDate == null ? -1 : fileDate.compareTo(o.fileDate);
    }

    @Override
    public int hashCode(){
        return -1;
    }
}
