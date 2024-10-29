package com.laeben.corelauncher.api.shortcut.windows.shell.item;

import com.laeben.corelauncher.api.shortcut.windows.util.ByteWriter;

import java.io.IOException;

public class FileEntryShellItem implements ShellItem{

    public static class Indicators{
        public static final int IS_DIRECTORY = 0x01;
        public static final int IS_FILE = 0x02;
        public static final int HAS_UNICODE = 0x04;
        public static final int UNKNOWN = 0x08;
        public static final int HAS_CLASS = 0x80;
    }

    private int indicator;
    private int attributes;
    private String name;

    private FileEntryShellItem(){
        indicator = 0x30;

        name = "";
    }

    public static FileEntryShellItem create(){
        return new FileEntryShellItem();
    }

    public FileEntryShellItem setIndicator(int flags){
        indicator = 0x30 | flags;
        return this;
    }

    public FileEntryShellItem setFileAttributes(int attributes){
        this.attributes = attributes;
        return this;
    }
    public FileEntryShellItem setFileName(String name){
        this.name = name;
        return this;
    }

    @Override
    public int serialize(ByteWriter writer) throws IOException {
        short size = 14;

        var str = ByteWriter.getString(name);
        size += (short) str.length;

        var strLong = ByteWriter.getString16(name);
        final byte[] beef = new byte[] {4, 0, (byte) 0xef, (byte) 0xbe};

        short extSize = (short) (36 + beef.length + strLong.length);

        size += extSize;

        writer.writeShort(size);
        writer.write((byte) indicator);
        writer.write((byte) 0);
        writer.writeInt(0);
        writer.writeInt(0);
        writer.writeShort(attributes);
        writer.write(str);

        // 0xbeef0004
        writer.writeShort(extSize); // ext size
        writer.writeShort(7); // version
        writer.write(beef);
        writer.writeInt(0); // date
        writer.writeInt(0); // date
        writer.writeShort(0x26); // windows vista
        writer.writeShort(0);
        writer.writeLong(0);
        writer.writeLong(0);
        writer.writeShort(0); // long string size
        writer.write(strLong); // long str
        writer.writeShort(14 + str.length); // offset

        return size;
    }
}