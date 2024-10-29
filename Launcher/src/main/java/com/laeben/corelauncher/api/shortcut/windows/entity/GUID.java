package com.laeben.corelauncher.api.shortcut.windows.entity;

import java.nio.ByteBuffer;
import java.util.HexFormat;

public class GUID{
    private final String text;

    private GUID(String text){
        this.text = text;
    }

    public static GUID fromText(String text){
        return new GUID(text);
    }

    private static byte[] convert(String section, boolean flip){
        int len = section.length() / 2;
        byte[] bytes = new byte[len];

        for (int i = 0; i < len; i++){
            byte b = (byte) HexFormat.fromHexDigits(section.substring(i *2, i*2+2));
            bytes[flip ? len - i - 1 : i] = b;
        }
        return bytes;
    }

    public byte[] getBytes(){
        var buffer = ByteBuffer.allocate(16);

        var sections = text.split("-");

        buffer.put(convert(sections[0], true));
        buffer.put(convert(sections[1], true));
        buffer.put(convert(sections[2], true));
        buffer.put(convert(sections[3], false));
        buffer.put(convert(sections[4], false));

        return buffer.array();
    }
}