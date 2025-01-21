package com.laeben.corelauncher.api.shortcut.windows.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

public class ByteWriter implements Closeable {

    public interface Serializable{
        int serialize(ByteWriter writer) throws IOException;
    }

    private final OutputStream stream;

    public ByteWriter(OutputStream stream){
        this.stream = stream;
    }

    public int writeShort(int s) throws IOException {
        byte first = (byte)(s & 0xFF);
        byte second = (byte)((s >> 8) & 0xFF);
        stream.write(first);
        stream.write(second);

        return 2;
    }

    private void writeLittleEndian(long num, int size) throws IOException {
        for (int k = 0; k < size; k += 8){
            stream.write((byte)((num >>> k) & 0xFF));
        }
    }

    public int writeInt(int i) throws IOException {
        writeLittleEndian(i, 32);

        return 4;
    }

    public int writeHex(String hex) throws IOException {
        var bytes = hex.split(" ");
        for (var b : bytes)
            write((byte) HexFormat.fromHexDigits(b));
        return bytes.length;
    }

    public int writeLong(long l) throws IOException {
        writeLittleEndian(l, 64);

        return 8;
    }

    public int writeStringWithLength(String str) throws IOException {
        writeShort((short) str.length());
        var bytes = str.getBytes(StandardCharsets.UTF_16LE);
        stream.write(bytes);

        return 2 + bytes.length;
    }

    public static byte[] getString16(String str) {
        return (str + "\0").getBytes(StandardCharsets.UTF_16LE);
    }

    public static byte[] getString(String str) {
        return (str + "\0").getBytes(StandardCharsets.UTF_8);
    }

    public int write(byte[] bytes) throws IOException {
        stream.write(bytes);

        return bytes.length;
    }

    public int write(byte b) throws IOException {
        stream.write(b);

        return 1;
    }

    public OutputStream getStream(){
        return stream;
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}