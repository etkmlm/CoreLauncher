package com.laeben.corelauncher.minecraft.util;

import com.laeben.core.entity.Path;
import com.laeben.corelauncher.api.entity.OS;
import com.laeben.corelauncher.minecraft.entity.Asset;
import com.laeben.corelauncher.minecraft.entity.Library;

import java.util.ArrayList;
import java.util.List;

public class LibraryConcat {
    private final List<Asset> assets;

    private final List<Library> libraries;

    private final Path libDir;

    private LibraryConcat(Path libDir){
        libraries = new ArrayList<>();
        assets = new ArrayList<>();

        this.libDir = libDir;
    }

    public static LibraryConcat begin(Path libDir){
        return new LibraryConcat(libDir);
    }

    public LibraryConcat addLibraries(List<Library> libraries){
        this.libraries.addAll(libraries);
        return this;
    }

    public LibraryConcat build(){
        for (var lib : libraries){
            if (!lib.checkAvailability(OS.getSystemOS()))
                continue;
            var asset = lib.getAsset();
            if (asset == null || asset.base == null)
                continue;
            asset.calculateComparator();


            boolean c = false;
            for (int i = 0; i < assets.size(); i++){
                var a = assets.get(i);
                if (!a.base.equals(asset.base))
                    continue;
                if (a.version > asset.version)
                    c = true;
                else
                    assets.remove(i);
                break;
            }
            if (c)
                continue;

            assets.add(asset);
        }

        return this;
    }

    public List<String> paths(){
        return assets.stream().map(x -> libDir.to(x.path).toString()).distinct().toList();
    }
}
