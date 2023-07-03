package com.cdev.corelauncher.data.nbt.entities;

import com.cdev.corelauncher.data.nbt.util.ByteReader;

import java.util.ArrayList;
import java.util.List;

public class NBTList extends NBTTag{

    private final List<NBTTag> tags;
    public NBTList() {
        super(NBTTagType.LIST);

        tags = new ArrayList<>();
    }

    @Override
    public NBTTag deserialize(ByteReader reader){
        String name = readName(reader);

        byte elTypeId = reader.read();
        int listLen = reader.readInt();

        for (int i = 0; i < listLen; i++)
            tags.add(NBTCompound.readTag(reader, elTypeId, i));


        return setName(name);
    }
}
