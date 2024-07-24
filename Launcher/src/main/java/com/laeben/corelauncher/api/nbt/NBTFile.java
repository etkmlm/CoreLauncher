package com.laeben.corelauncher.api.nbt;

import com.laeben.corelauncher.api.nbt.entity.NBTCompound;
import com.laeben.corelauncher.api.nbt.util.ByteReader;
import com.laeben.corelauncher.api.entity.Logger;

import java.io.ByteArrayOutputStream;

public class NBTFile extends NBTCompound {
    private ByteReader input;
    private ByteArrayOutputStream output;

    public NBTFile(){
        output = new ByteArrayOutputStream();
    }

    public NBTFile(byte[] bytes){
        input = new ByteReader(bytes);

        try{
            read();
        }
        catch (Exception e){
            Logger.getLogger().logHyph("ERRNBT");
            Logger.getLogger().log(e);
        }
    }

    public void read(){
        readItems(input);
    }
}
