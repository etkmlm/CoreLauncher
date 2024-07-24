package com.laeben.corelauncher.api.nbt.entity;

import com.laeben.corelauncher.api.nbt.util.ByteReader;

public class NBTTag {

    private final NBTTagType type;
    private String name;
    private int listOrder = -1;
    private Object value;

    public NBTTag(NBTTagType type){
        this.type = type;
    }

    public NBTTag setName(String name){
        this.name = name;
        return this;
    }

    public NBTTag setOrder(int order){
        listOrder = order;

        return this;
    }

    public NBTTag setValue(Object value){
        this.value = value;
        return this;
    }

    public NBTCompound asCompound(){
        return (NBTCompound) this;
    }

    public String name(){
        return name;
    }

    public Object value(){
        return value;
    }

    public String stringValue(){
        return value.toString();
    }

    public int intValue(){
        return (int)(double)value;
    }

    public double doubleValue(){
        return (double) value;
    }

    public long longValue(){
        return (long) (double)value;
    }

    public NBTTagType getType(){
       return type;
    }

    protected String readName(ByteReader reader){
        String name = String.valueOf(listOrder);
        if (listOrder == -1){
            short lenName = reader.readShort();
            name = reader.readString(lenName);
        }

        return name;
    }

    public NBTTag deserialize(ByteReader reader){
        String name = readName(reader);

        return setName(name).setValue(
                type == NBTTagType.DOUBLE || type == NBTTagType.FLOAT ? reader.readDouble(type.getPayload()) : reader.readNum(type.getPayload())
        );
    }

}
