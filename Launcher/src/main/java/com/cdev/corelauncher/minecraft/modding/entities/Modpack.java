package com.cdev.corelauncher.minecraft.modding.entities;

import com.cdev.corelauncher.minecraft.Wrapper;
import com.cdev.corelauncher.minecraft.modding.curseforge.entities.File;
import com.cdev.corelauncher.minecraft.modding.curseforge.entities.Manifest;
import com.cdev.corelauncher.minecraft.modding.curseforge.entities.Resource;

import java.util.ArrayList;
import java.util.List;

public class Modpack extends CResource {
    public transient Wrapper wr;
    public transient String wrId;
    public transient List<Mod> mods;
    public transient List<Resourcepack> resources;
    public transient Manifest manifest;

    public static Modpack fromResource(String vId, String loader, Resource r){
        var pack = fromResource(new Modpack(), vId, loader, r);

        return pack;
    }

    public void applyManifest(Manifest mf){
        this.manifest = mf;

        String loader = mf.minecraft.modLoaders.stream().filter(x -> x.primary).findFirst().get().id;
        String[] ldr = loader.split("-");

        wr = Wrapper.getWrapper(ldr[0]);
        wrId = ldr[1];
    }
    public List<Integer> getFileIds(){
        if (manifest == null)
            return List.of();

        return manifest.files.stream().map(x -> x.fileID).toList();
    }
    public List<Integer> getProjectIds(){
        if (manifest == null)
            return List.of();

        return manifest.files.stream().map(x -> x.projectID).toList();
    }
    public void applyResources(List<CResource> res, List<File> files){
        this.mods = new ArrayList<>();
        this.resources = new ArrayList<>();

        for(var r : res){
            if (manifest != null){
                var file = manifest.files.stream().filter(x -> x.projectID == r.id).findFirst().orElse(null);
                if (file != null){
                    var f = files.stream().filter(x -> x.id == file.fileID).findFirst();
                    f.ifPresent(r::setFile);
                }
            }

            if (r instanceof Mod m){
                m.mpId = id;

                this.mods.add(m);
            }
            else if (r instanceof Resourcepack p){
                p.mpId = id;
                this.resources.add(p);
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Modpack m && m.id == id;
    }
}
