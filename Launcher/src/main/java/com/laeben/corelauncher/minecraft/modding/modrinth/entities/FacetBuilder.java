package com.laeben.corelauncher.minecraft.modding.modrinth.entities;

import java.util.ArrayList;
import java.util.List;

public class FacetBuilder {
    private final List<Facet> facets;

    private String loader;
    private String gameVersion;

    public FacetBuilder(){
        facets = new ArrayList<>();
    }

    public FacetBuilder setLoader(String loader){
        this.loader = loader;
        return this;
    }

    public FacetBuilder setGameVersion(String vId){
        this.gameVersion = vId;
        return this;
    }

    public FacetBuilder add(Facet f){
        var a = facets.stream().filter(x -> x.id != null && x.id.equals(f.id)).findFirst();
        a.ifPresent(facets::remove);

        facets.add(f);

        return this;
    }

    public void remove(String id){
        facets.removeIf(x -> id.equals(x.id));
    }

    public List<Facet> build(){

        if (loader != null){
            if (facets.stream().noneMatch(x -> x.key.equals("categories") && x.values.contains(loader)))
                facets.add(Facet.get("categories", loader).setId("loader"));
        }
        else
            facets.removeIf(x -> x.id != null && x.id.equals("loader"));

        if (gameVersion != null){
            if (facets.stream().noneMatch(x -> x.key.equals("versions") && x.values.contains(gameVersion)))
                facets.add(Facet.get("versions", gameVersion).setId("version"));
        }
        else
            facets.removeIf(x -> x.id != null && x.id.equals("version"));

        return facets.stream().toList();
    }
}
