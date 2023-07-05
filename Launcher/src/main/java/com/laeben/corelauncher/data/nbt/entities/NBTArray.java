package com.laeben.corelauncher.data.nbt.entities;

import com.laeben.corelauncher.data.nbt.util.ByteReader;

import java.util.ArrayList;
import java.util.List;

public class NBTArray extends NBTTag{

    private final List<Object> items;

    public NBTArray(NBTTagType type) {
        super(type);

        items = new ArrayList<>();
    }

    public <T> List<T> getItems(){
        return items.stream().map(x -> (T)x).toList();
    }

    @Override
    public NBTTag deserialize(ByteReader reader){
        String name = readName(reader);
        int len = reader.readInt();
        int itemLen = getType() == NBTTagType.BYTE_ARR ? 1 : (getType() == NBTTagType.INT_ARR ? 4 : 8);
        for (int i = 0; i < len; i++)
            items.add(reader.read(itemLen));
        return setName(name);
    }


}
