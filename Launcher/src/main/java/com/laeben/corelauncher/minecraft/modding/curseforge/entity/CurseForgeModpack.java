package com.laeben.corelauncher.minecraft.modding.curseforge.entity;

import com.laeben.corelauncher.minecraft.Loader;
import com.laeben.corelauncher.minecraft.modding.entity.*;
import com.laeben.corelauncher.minecraft.modding.entity.resource.*;

import java.util.ArrayList;
import java.util.List;

public class CurseForgeModpack {
    private transient Manifest manifest;
    private transient String versionId;
    private final Modpack mp;

    public CurseForgeModpack(Modpack mp) {
        this.mp = mp;
    }

    public Modpack getPack(){
        return mp;
    }

    public void applyManifest(Manifest mf){
        this.manifest = mf;

        String loader = mf.minecraft.modLoaders.stream().filter(x -> x.primary).findFirst().get().id;
        String[] ldr = loader.split("-");

        mp.targetVersionId = mf.minecraft.version;
        mp.wr = Loader.getLoader(ldr[0]);
        mp.wrId = ldr[1];
    }
    public List<Integer> getFileIds(){
        if (manifest == null)
            return List.of();

        return manifest.files.stream().map(x -> x.fileID).toList();
    }
    public List<Object> getProjectIds(){
        if (manifest == null)
            return List.of();

        return manifest.files.stream().map(x -> (Object) x.projectID).toList();
    }
    public void applyResources(List<CResource> res, List<CurseForgeFile> files){
        mp.mods = new ArrayList<>();
        mp.resources = new ArrayList<>();
        mp.shaders = new ArrayList<>();

        for(var r : res){
            if (manifest != null){
                var file = manifest.files.stream().filter(x -> x.projectID == r.getIntId()).findFirst().orElse(null);
                if (file != null){
                    var f = files.stream().filter(x -> x.id == file.fileID).findFirst();
                    f.ifPresent(x -> r.setFile(CResource.fromForgeFile(x, r.getIntId())));
                }
            }

            if (!(r instanceof ModpackContent mpc))
                continue;
            mpc.setModpackId(mp.id);

            if (r instanceof Mod m)
                mp.mods.add(m);
            else if (r instanceof Resourcepack p)
                mp.resources.add(p);
            else if (r instanceof Shader s)
                mp.shaders.add(s);
        }
    }
}
