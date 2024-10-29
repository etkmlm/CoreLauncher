package com.laeben.corelauncher.api.shortcut.windows.entity;

import com.laeben.corelauncher.api.shortcut.windows.util.ByteWriter;

import java.io.IOException;

public class VolumeID implements ByteWriter.Serializable {

    public enum DriveType{
        UNKNOWN, NO_ROOT_DIR, REMOVABLE, FIXED, NETWORK, CD_ROM, RAM
    }

    private DriveType driveType;
    private int serialNumber;
    private final int labelOffset;
    private final int labelOffsetUnicode;
    private String data;

    public VolumeID(){
        data = "";

        labelOffset = 20;
        labelOffsetUnicode = 20;
    }

    public static VolumeID create(DriveType type){
        return new VolumeID().setDriveType(type);
    }

    public VolumeID setDriveType(DriveType driveType) {
        this.driveType = driveType;

        return this;
    }

    public VolumeID setSerialNumber(int serialNumber) {
        this.serialNumber = serialNumber;

        return this;
    }

    public VolumeID setData(String data) {
        this.data = data;

        return this;
    }

    @Override
    public int serialize(ByteWriter writer) throws IOException {
        int size = 20;

        var str = ByteWriter.getString(data);
        size += str.length;

        writer.writeInt(size);
        writer.writeInt(driveType.ordinal());
        writer.writeInt(serialNumber);
        writer.writeInt(labelOffset);
        writer.writeInt(labelOffsetUnicode);
        writer.write(str);

        return size;
    }
}