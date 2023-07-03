package com.cdev.corelauncher.data.nbt.entities;

import java.util.Arrays;

public enum NBTTagType {

    END(0), BYTE(1), SHORT(2), INT(4), LONG(8), FLOAT(4), DOUBLE(8), BYTE_ARR(-1), STRING(-1), LIST(-1), COMPOUND(-1), INT_ARR(-1), LONG_ARR(-1);

    private final short payload;

    NBTTagType(int payload){
        this.payload = (short) payload;
    }

    public byte getTypeId(){
        return (byte)ordinal();
    }

    public short getPayload(){
        return payload;
    }

    public static NBTTagType fromTypeId(byte id){
        return Arrays.stream(NBTTagType.values()).filter(x -> x.getTypeId() == id).findFirst().orElse(null);
    }
}
