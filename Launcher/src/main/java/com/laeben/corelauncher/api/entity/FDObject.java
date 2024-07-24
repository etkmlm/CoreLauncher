package com.laeben.corelauncher.api.entity;

import java.util.ArrayList;
import java.util.List;

public class FDObject {

    public enum FDType{
        SINGLE, GROUP
    }

    public double layoutX;
    public double layoutY;
    public FDType type;
    public String name;
    public List<Profile> profiles;

    public List<Profile> getProfiles(){
        if (profiles == null)
            profiles = new ArrayList<>();

        return profiles;
    }

    public boolean contains(Profile p){
        return getProfiles().stream().anyMatch(a -> a.getName().equals(p.getName()));
    }
    public void remove(Profile p){
        getProfiles().removeIf(a -> a.getName().equals(p.getName()));
    }
    public void add(Profile p){
        if (!contains(p))
            getProfiles().add(p);
    }

    public static FDObject createSingle(Profile p, double x, double y){
        return create(List.of(p), x, y, null, FDType.SINGLE);
    }

    public static FDObject createGroup(List<Profile> p, double x, double y, String name){
        return create(new ArrayList<>(p), x, y, name, FDType.GROUP);
    }

    public static FDObject create(List<Profile> ps, double x, double y, String name, FDObject.FDType type){
        var obj = new FDObject();
        obj.profiles = ps;
        obj.type = type;
        obj.layoutX = x;
        obj.layoutY = y;
        obj.name = name;
        return obj;
    }

    public String getName(){
        if (name != null)
            return name;
        if (isSingle())
            name = getProfiles().stream().findFirst().orElse(Profile.empty()).getName();
        return name;
    }

    public boolean isSingle(){
        return type == FDType.SINGLE;
    }
    public Profile primary(){
        return getProfiles().stream().findFirst().orElse(Profile.empty());
    }
}
