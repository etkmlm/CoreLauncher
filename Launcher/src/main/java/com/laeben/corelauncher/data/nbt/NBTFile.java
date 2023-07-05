package com.laeben.corelauncher.data.nbt;

import com.laeben.corelauncher.data.nbt.entities.NBTCompound;
import com.laeben.corelauncher.data.nbt.util.ByteReader;
import com.laeben.corelauncher.utils.Logger;

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
