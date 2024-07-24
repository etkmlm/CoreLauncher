package com.laeben.corelauncher.api.nbt.entity;

import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.nbt.util.ByteReader;
import com.laeben.corelauncher.util.entity.LogType;

import java.util.ArrayList;
import java.util.List;

public class NBTCompound extends NBTTag {
    private final List<NBTTag> tags;
    public NBTCompound() {
        super(NBTTagType.COMPOUND);

        tags = new ArrayList<>();
    }

    public static NBTTag readTag(ByteReader reader, byte id, Integer listOrder){
        var type = NBTTagType.fromTypeId(id);
        if (listOrder == null)
            listOrder = -1;
        if (type == NBTTagType.COMPOUND)
            return new NBTCompound().setOrder(listOrder).deserialize(reader);
        else if (type.toString().endsWith("ARR"))
            return new NBTArray(type).setOrder(listOrder).deserialize(reader);
        else if (type == NBTTagType.LIST)
            return new NBTList().setOrder(listOrder).deserialize(reader);
        else if (type == NBTTagType.STRING)
            return new NBTString().setOrder(listOrder).deserialize(reader);
        else
            return new NBTTag(type).setOrder(listOrder).deserialize(reader);
    }

    public void add(NBTTag tag){
        tags.add(tag);
    }

    protected void readItems(ByteReader reader){
        try {
            while (reader.check())
                tags.add(readTag(reader, reader.read(), null));
        } catch (Exception e) {
            Logger.getLogger().logDebug(LogType.ERROR, e.getMessage());
        }
    }

    public NBTTag first(){
        return tags.stream().findFirst().orElse(null);
    }

    public NBTTag firstForName(String name){
        return tags.stream().filter(x -> (x.name() == null && name == null) || (x.name() != null && x.name().equals(name))).findFirst().orElse(null);
    }

    @Override
    public NBTTag deserialize(ByteReader reader) {
        String name = readName(reader);

        byte read;
        while ((read = reader.read()) != 0)
            tags.add(readTag(reader, read, null));

        return setName(name);
    }
}
