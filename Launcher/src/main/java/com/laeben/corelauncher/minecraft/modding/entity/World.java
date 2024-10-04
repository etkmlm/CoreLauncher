package com.laeben.corelauncher.minecraft.modding.entity;

import com.laeben.corelauncher.api.nbt.NBTFile;
import com.laeben.corelauncher.minecraft.modding.curseforge.entity.ResourceForge;
import com.laeben.core.entity.Path;

import java.io.IOException;

public class World extends CResource{
    public enum Difficulty{
        PEACEFUL, EASY, NORMAL, HARD
    }

    public enum GameType{
        SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR
    }

    public record Coordinate(int x, int y, int z){
        @Override
        public String toString(){
            return x + " " + y + " " + z;
        }
    }

    @Override
    public ResourceType getType(){
        return ResourceType.WORLD;
    }

    public String levelName;
    public String dirName;
    public Difficulty difficulty;
    public GameType gameType;
    public boolean isHardcore;
    public Coordinate worldSpawn;
    public String gameVersion;
    public boolean allowCommands;
    public Path iconPath;
    public long seed;

    public static World fromResource(String vId, String loader, ResourceForge r){
        var pack = fromForgeResource(new World(), vId, loader, r, null);
        if (!pack.forgeModules.isEmpty())
            pack.name = pack.forgeModules.stream().findFirst().get().name;
        return pack;
    }

    public Path getWorldIcon(){
        return iconPath;
    }

    public static NBTFile openNBT(Path path){
        byte[] gzip;
        try {
            gzip = path.openAsGzip();
        } catch (IOException e) {
            return null;
        }
        var nbt = new NBTFile(gzip);

        return nbt.first() == null ? null : nbt;
    }

    public static World fromGzip(World world, Path path){
        if (world == null)
            world = new World();

        var nbt = openNBT(path);
        if (nbt == null)
            return world;

        world.dirName = path.parent().getName();
        world.iconPath = path.parent().to("icon.png");

        var data = nbt.first().asCompound().firstForName("Data").asCompound();

        world.levelName = data.firstForName("LevelName").value().toString();

        var diff = data.firstForName("Difficulty");
        world.difficulty = diff == null ? Difficulty.NORMAL : Difficulty.values()[diff.intValue()];
        int spawnX = data.firstForName("SpawnX").intValue();
        int spawnY = data.firstForName("SpawnY").intValue();
        int spawnZ = data.firstForName("SpawnZ").intValue();
        world.worldSpawn = new Coordinate(spawnX, spawnY, spawnZ);

        var ver = data.firstForName("Version");
        world.gameVersion = ver == null ? null : ver.asCompound().firstForName("Name").stringValue();
        world.gameType = GameType.values()[data.firstForName("GameType").intValue()];

        var allowCmd = data.firstForName("allowCommands");
        world.allowCommands = allowCmd != null && allowCmd.intValue() == 1;

        var hc = data.firstForName("hardcore");
        world.isHardcore = hc != null && hc.intValue() == 1;
        try{
            var seedOld = data.firstForName("RandomSeed");
            world.seed = seedOld != null ? seedOld.longValue() : data.firstForName("WorldGenSettings").asCompound().firstForName("seed").longValue();
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
}
