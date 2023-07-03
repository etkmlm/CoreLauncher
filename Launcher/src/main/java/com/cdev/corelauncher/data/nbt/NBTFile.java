package com.cdev.corelauncher.data.nbt;

import com.cdev.corelauncher.data.nbt.entities.NBTCompound;
import com.cdev.corelauncher.data.nbt.util.ByteReader;
import com.cdev.corelauncher.utils.entities.Path;

import java.io.ByteArrayOutputStream;

public class NBTFile extends NBTCompound {
    private ByteReader input;
    private ByteArrayOutputStream output;

    public NBTFile(){
        output = new ByteArrayOutputStream();
    }

    public NBTFile(byte[] bytes){
        input = new ByteReader(bytes);

        read();
    }

    public void read(){
        readItems(input);
    }
}
