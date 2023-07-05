package com.laeben.corelauncher.minecraft.modding.entities;

import com.laeben.corelauncher.data.nbt.NBTFile;
import com.laeben.corelauncher.minecraft.modding.curseforge.entities.Resource;
import com.laeben.core.entity.Path;

public class World extends CResource{
    public enum Difficulty{
        PEACEFUL, EASY, NORMAL, HARD;
    }

    public enum GameType{
        SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR;
    }

    public record Coordinate(int x, int y, int z){
        @Override
        public String toString(){
            return x + " " + y + " " + z;
        }
    }

    public String levelName;
    public Difficulty difficulty;
    public GameType gameType;
    public boolean isHardcore;
    public Coordinate worldSpawn;
    public String gameVersion;
    public long seed;

    public static World fromResource(String vId, String loader, Resource r){
        var pack = fromResource(new World(), vId, loader, r);
        if (pack.modules.size() != 0)
            pack.name = pack.modules.stream().findFirst().get().name;
        return pack;
    }

    public static World fromGzip(World world, Path path){
        if (world == null)
            world = new World();

        byte[] gzip = path.openAsGzip();
        var nbt = new NBTFile(gzip);
        if (nbt.first() == null)
            return world;
        var data = nbt.first().asCompound().firstForName("Data").asCompound();

        world.levelName = data.firstForName("LevelName").value().toString();
        world.difficulty = Difficulty.values()[data.firstForName("Difficulty").intValue()];
        int spawnX = data.firstForName("SpawnX").intValue();
        int spawnY = data.firstForName("SpawnY").intValue();
        int spawnZ = data.firstForName("SpawnZ").intValue();
        world.worldSpawn = new Coordinate(spawnX, spawnY, spawnZ);
        world.gameVersion = data.firstForName("Version").asCompound().firstForName("Name").stringValue();
        world.gameType = GameType.values()[data.firstForName("GameType").intValue()];
        try{
            world.isHardcore = data.firstForName("hardcore").intValue() == 1;
            world.seed = data.firstForName("WorldGenSettings").asCompound().firstForName("seed").longValue();
        }
        catch (Exception ignored){

        }
        return world;
    }

    public String getIdentifier(){
        return levelName == null ? (name == null ? "" : name) : levelName;
    }

    @Override
    public boolean equals(Object o){
        return o instanceof World w && getIdentifier().equals(w.getIdentifier());
    }

    @Override
    public int hashCode(){
        return -1;
    }
}
