package com.laeben.corelauncher.api.shortcut.windows.shell.item;

import com.laeben.corelauncher.api.shortcut.windows.util.ByteWriter;

import java.io.IOException;
import java.util.Arrays;

public class VolumeShellItem implements ShellItem{
    private final String letter;

    private final byte[] fillBlank;

    private VolumeShellItem(String letter){
        this.letter = letter;

        fillBlank = new byte[18];
        Arrays.fill(fillBlank, (byte) 0);
    }

    public static VolumeShellItem create(String letter){
        return new VolumeShellItem(letter);
    }

    @Override
    public int serialize(ByteWriter writer) throws IOException {

        var str = ByteWriter.getString("/" + letter + ":\\");

        int size = 2 + fillBlank.length + str.length;

        writer.writeShort(size);
        writer.write(str);
        writer.write(fillBlank);

        return size;
    }
}