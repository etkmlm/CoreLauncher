package com.laeben.corelauncher.api;

import com.laeben.core.entity.Path;
import com.laeben.core.util.events.BaseEvent;
import com.laeben.core.util.events.KeyEvent;
import com.laeben.corelauncher.api.entity.FDObject;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.util.EventHandler;
import com.laeben.corelauncher.api.entity.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FloatDock {
    public static final String KEY = "fdock";

    public static final String PLACE = "place";
    public static final String REPLACE = "replace";
    public static final String REMOVE = "remove";
    public static final String REMOVE_ALL = "removeAll";
    public static final String UPDATE = "update";
    public static final String MOVEMENT = "movement";

    public static class FDConfig{
        private List<FDObject> objects;

        public List<FDObject> getObjects(){
            if (objects == null)
                objects = new ArrayList<>();

            return objects;
        }
    }

    private Path dockFile;
    private FDConfig config;
    private static FloatDock instance;

    private final EventHandler<BaseEvent> handler;

    public FloatDock(){
        dockFile = dockFile();

        handler = new EventHandler<>();

        Configurator.getConfigurator().getHandler().addHandler(KEY, (a) -> {
            if (!a.getKey().equals(Configurator.GAME_PATH_CHANGE))
                return;
            dockFile = dockFile();

            reload();
        }, false);

        Profiler.getProfiler().getHandler().addHandler(KEY, a -> {
            var po = (Profile)a.getOldValue();
            if (a.getKey().equals(Profiler.PROFILE_UPDATE)){
                var p = (Profile)a.getNewValue();
                for (var i : getObjects()){
                    if (i.contains(p)){
                        if (po != null && !po.getName().equals(p.getName()) && i.isSingle())
                            i.name = p.getName();
                        save();
                        handler.execute(new KeyEvent(UPDATE).setSource(i));
                    }
                }
            }
            else if (a.getKey().equals(Profiler.PROFILE_DELETE)){
                for (var i : getObjects().stream().toList()){
                    if (i.contains(po)){
                        if (i.isSingle())
                            remove(i);
                        else
                            removeFromGroup(i, po);
                    }
                }
            }
            else if (a.getKey().equals(EventHandler.RELOAD))
                reload();

        }, false);

        instance = this;
    }

    public void reload(){
        try{
            config = Configurator.getProfileGson().fromJson(dockFile.read(), FDConfig.class);

            if (config == null)
                config = new FDConfig();

            handler.execute(new KeyEvent(EventHandler.RELOAD));
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }
    }

    public void removeFromGroup(FDObject group, Profile profile){
        if (profile == null)
            return;
        group.remove(profile);
        save();
        handler.execute(new KeyEvent(UPDATE).setSource(group));
    }

    public void reorderGroup(FDObject group, Profile p1, int index) {
        group.getProfiles().removeIf(a -> a.getName().equals(p1.getName()));
        group.getProfiles().add(index, p1);
        save();
        handler.execute(new KeyEvent(UPDATE).setSource(group));
    }

    public void renameGroup(FDObject group, String name){
        if (getObjects().stream().anyMatch(a -> a.type == FDObject.FDType.GROUP && a.getName().equals(name)))
            return;
        group.name = name;
        save();
        handler.execute(new KeyEvent(UPDATE).setSource(group));
    }

    public void moveToGroup(FDObject group, List<FDObject> profiles){
        for (var p : profiles){
            group.add(p.getProfiles().get(0));
            remove(p);
        }
        save();
        handler.execute(new KeyEvent(UPDATE).setSource(group));
        //remove(profile);
    }

    public void addToGroup(FDObject group, List<Profile> profiles){
        for (var p : profiles){
            group.add(p);
        }
        save();
        handler.execute(new KeyEvent(UPDATE).setSource(group));
    }

    public EventHandler<BaseEvent> getHandler(){
        return handler;
    }

    public static FloatDock getDock(){
        return instance;
    }

    public void save(){
        try{
            String json = Configurator.getProfileGson().toJson(config);
            dockFile.write(json);
        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }
    }

    public Path dockFile(){
        return Configurator.getConfig().getLauncherPath().to("dock.json");
    }

    public List<FDObject> getObjects(){
        return config.getObjects();
    }

    public Optional<FDObject> findObject(Profile p){
        return getObjects().stream().filter(a -> a.isSingle() && a.getProfiles().stream().anyMatch(b -> p.getName().equals(b.getName()))).findFirst();
    }
    public Optional<FDObject> findObject(String name){
        return getObjects().stream().filter(a -> name.equals(a.getName())).findFirst();
    }

    public FDObject contains(FDObject obj){
        return getObjects().stream().filter(a -> obj.isSingle() ? (a.isSingle() && obj.primary().getName().equals(a.getName())) : obj.getName().equals(a.getName())).findFirst().orElse(null);
    }

    public String generateName(final String identifier){
        int num = 1;
        while (true){
            int n2 = num;
            if (getObjects().stream().anyMatch(a -> a.getName().equals(identifier + n2)))
                num++;
            else
                break;
        }

        return identifier + num;
    }

    public void place(FDObject obj, boolean movement){
        var c = contains(obj);
        if (c == null)
            getObjects().add(obj);
        else {
            c.layoutX = obj.layoutX;
            c.layoutY = obj.layoutY;
            obj = c;
        }

        save();
        handler.execute(new KeyEvent(c != null ? (!movement ? REPLACE : MOVEMENT) : PLACE).setSource(obj));
    }

    public void remove(FDObject obj){
        getObjects().remove(obj);
        save();
        handler.execute(new KeyEvent(REMOVE).setSource(obj));
    }

    public void removeAll(List<FDObject> objects){
        getObjects().removeAll(objects);
        save();
        handler.execute(new KeyEvent(REMOVE_ALL).setSource(objects));
    }

}
