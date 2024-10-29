package com.laeben.corelauncher.api.shortcut.windows.shell;

import com.laeben.corelauncher.api.shortcut.windows.util.ByteWriter;

import java.io.IOException;
import java.util.Base64;

public class ShellLinkHeader implements ByteWriter.Serializable {

    public static class Flags{
        public static final int HAS_LINK_TARGET_ID_LIST = 0x00000001;
        public static final int HAS_LINK_INFO = 0x00000002;
        public static final int HAS_NAME = 0x00000004;
        public static final int HAS_RELATIVE_PATH = 0x00000008;
        public static final int HAS_WORKING_DIR = 0x00000010;
        public static final int HAS_ARGUMENTS = 0x000000020;
        public static final int HAS_ICON_LOCATION = 0x00000040;
        public static final int IS_UNICODE = 0x00000080;
        public static final int FORCE_NO_LINK_INFO = 0x00000100;
        public static final int HAS_EXP_STRING = 0x00000200;
        public static final int RUN_IN_SEPARATE_PROCESS = 0x00000400;
    }

    public static class ConsoleWindow{
        public static final int SW_NORMAL = 1;
        public static final int SW_MINIMIZED = 2;
        public static final int SW_MAXIMIZED = 3;
    }

    private final byte[] clsid;

    private int linkFlags;
    private int attributes;
    private int creationDate;
    private int accessTime;
    private int writeTime;
    private int fileSize;
    private int index;
    private int showCmd;
    private int hotkey;

    private static final short r1 = 0;
    private static final int r2 = 0;
    private static final int r3 = 0;

    public ShellLinkHeader(){
        clsid = Base64.getDecoder().decode("ARQCAAAAAADAAAAAAAAARg==");
    }

    public ShellLinkHeader setLinkFlags(int linkFlags) {
        this.linkFlags = linkFlags;

        return this;
    }

    public ShellLinkHeader setAttributes(int attributes) {
        this.attributes = attributes;

        return this;
    }

    public ShellLinkHeader setCreationDate(int creationDate) {
        this.creationDate = creationDate;

        return this;
    }

    public ShellLinkHeader setAccessTime(int accessTime) {
        this.accessTime = accessTime;

        return this;
    }

    public ShellLinkHeader setWriteTime(int writeTime) {
        this.writeTime = writeTime;

        return this;
    }

    public ShellLinkHeader setFileSize(int fileSize) {
        this.fileSize = fileSize;

        return this;
    }

    public ShellLinkHeader setIndex(int index) {
        this.index = index;

        return this;
    }

    public ShellLinkHeader setShowCmd(int showCmd) {
        this.showCmd = showCmd;

        return this;
    }

    public ShellLinkHeader setHotkey(int hotkey) {
        this.hotkey = hotkey;

        return this;
    }

    @Override
    public int serialize(ByteWriter writer) throws IOException {
        int size = 76;

        writer.writeInt(size);
        writer.write(clsid);
        writer.writeInt(linkFlags);
        writer.writeInt(attributes);
        writer.writeLong(creationDate);
        writer.writeLong(accessTime);
        writer.writeLong(writeTime);
        writer.writeInt(fileSize);
        writer.writeInt(index);
        writer.writeInt(showCmd);
        writer.writeShort(hotkey);
        writer.writeShort(r1);
        writer.writeInt(r2);
        writer.writeInt(r3);

        return size;
    }
}