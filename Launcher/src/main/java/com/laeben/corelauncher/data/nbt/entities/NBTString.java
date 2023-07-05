package com.laeben.corelauncher.data.nbt.entities;

import com.laeben.corelauncher.data.nbt.util.ByteReader;

public class NBTString extends NBTTag {
    public NBTString() {
        super(NBTTagType.STRING);
    }

    public NBTString set(String value){
        setValue(value);
        return this;
    }

    @Override
    public NBTTag deserialize(ByteReader reader){
        String name = readName(reader);
        short len = reader.readShort();
        String val = reader.readString(len);
        return set(val).setName(name);
    }

}
