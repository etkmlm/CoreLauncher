package com.laeben.corelauncher.api.shortcut.windows.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HexFormat;

public class HexWriter extends ByteWriter{

    private final ByteArrayOutputStream str;

    public HexWriter() {
        super(new ByteArrayOutputStream());

        str = (ByteArrayOutputStream) super.getStream();
    }

    public String getHexString(){
        var n = str.toByteArray();
        var builder = new StringBuilder();
        for (var b : n)
            builder.append(HexFormat.of().toHexDigits(b)).append(" ");

        return builder.toString();
    }

    public static String getHexStringFromByteWriter(Serializable w) throws IOException {
        String t;
        try(var hex = new HexWriter()){
            w.serialize(hex);
            t = hex.getHexString();
        }

        return t;
    }
}