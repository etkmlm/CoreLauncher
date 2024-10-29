package com.laeben.corelauncher.api.shortcut.windows.shell;

import com.laeben.corelauncher.api.shortcut.windows.entity.VolumeID;
import com.laeben.corelauncher.api.shortcut.windows.util.ByteWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ShellLinkInfo implements ByteWriter.Serializable{

    public static final int VOLUME_ID_AND_LOCAL_BASE_PATH = 1;
    public static final int COMMON_NETWORK_RELATIVE_LINK_AND_PATH_SUFFIX = 2;


    private final int headerSize;
    private int flags;
    private VolumeID volumeID;
    private String localBasePath;
    private String commonPathSuffix;


    private ShellLinkInfo(){
        headerSize = 28;

        localBasePath = "";
        commonPathSuffix = "";
    }

    public static ShellLinkInfo create(){
        return new ShellLinkInfo();
    }

    public ShellLinkInfo setFlags(int flags) {
        this.flags = flags;

        return this;
    }

    public ShellLinkInfo setVolumeID(VolumeID volumeID) {
        this.volumeID = volumeID;

        return this;
    }

    public ShellLinkInfo setLocalBasePath(String localBasePath) {
        this.localBasePath = localBasePath;

        return this;
    }

    public ShellLinkInfo setCommonPathSuffix(String commonPathSuffix) {
        this.commonPathSuffix = commonPathSuffix;

        return this;
    }

    @Override
    public int serialize(ByteWriter writer) throws IOException {
        int size = headerSize;

        byte[] volume;

        try(var stream = new ByteArrayOutputStream();
            var tempWriter = new ByteWriter(stream)){

            volumeID.serialize(tempWriter);

            volume = stream.toByteArray();
        }

        int off1 = size;
        size += volume.length;

        int off2 = size;
        byte[] localBasePath = ByteWriter.getString(this.localBasePath);
        size += localBasePath.length;

        int off3 = size;
        byte[] commonPathSuffix = ByteWriter.getString(this.commonPathSuffix);
        size += commonPathSuffix.length;

        writer.writeInt(size);
        writer.writeInt(headerSize);
        writer.writeInt(flags);
        writer.writeInt(off1);
        writer.writeInt(off2);
        writer.writeInt(0);
        writer.writeInt(off3);
        writer.write(volume);
        writer.write(localBasePath);
        writer.write(commonPathSuffix);

        return size;
    }
}
