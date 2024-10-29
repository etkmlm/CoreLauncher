package com.laeben.corelauncher.api.shortcut.windows.shell.item;

import com.laeben.corelauncher.api.shortcut.windows.entity.GUID;
import com.laeben.corelauncher.api.shortcut.windows.util.ByteWriter;

import java.io.IOException;

public class RootFolderShellItem implements ShellItem{

    public static class RootFolder{
        public static final String MY_COMPUTER_FOLDER = "20D04FE0-3AEA-1069-A2D8-08002B30309D";
    }

    public static class SortIndex {
        public static final int INTERNET_EXPLORER = 0x00; // 0x68
        public static final int LIBRARIES = 0x42;
        public static final int USERS = 0x44;
        public static final int DOCUMENTS = 0x48;
        public static final int COMPUTER = 0x50;
        public static final int NETWORK = 0x58;
        public static final int RECYCLE_BIN = 0x60;
        public static final int UNKNOWN = 0x70;
        public static final int GAMES = 0x80;
    }

    private static final int indicator = 0x1f;
    private int index;
    private final GUID guid;

    RootFolderShellItem(GUID guid){
        this.guid = guid;
    }

    public static RootFolderShellItem create(String guid){
        return new RootFolderShellItem(GUID.fromText(guid));
    }

    public RootFolderShellItem setSortIndex(int index){
        this.index = index;

        return this;
    }

    @Override
    public int serialize(ByteWriter writer) throws IOException {
        short size = 20;

        writer.writeShort(size);
        writer.write((byte) indicator);
        writer.write((byte) index);
        writer.write(guid.getBytes());

        return size;
    }
}