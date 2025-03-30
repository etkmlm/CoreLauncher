package com.laeben.corelauncher.api.nbt.util;

import com.laeben.corelauncher.api.entity.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ByteReader {
    private final ByteArrayInputStream stream;

    public ByteReader(byte[] bytes){
        stream = new ByteArrayInputStream(bytes);
    }

    public double readDouble(int len){
        try {
            var bytes = stream.readNBytes(len);
            var wrap = ByteBuffer.wrap(bytes);
            return len == 8 ? wrap.getDouble() : wrap.getFloat();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public long readNum(int len){
        return switch (len){
            case 1 -> read();
            case 2 -> readShort();
            case 4 -> readInt();
            default -> readLong();
        };
    }

    public byte[] read(int len){
        try{
            return stream.readNBytes(len);
        }
        catch (Exception e){
            Logger.getLogger().log(e);
            return new byte[1];
        }
        /*try{
            var bytes = stream.readNBytes(len);
            var wrap = ByteBuffer.wrap(bytes);

            return switch (len){
                case 1 -> bytes[0];
                case 2 -> wrap.getShort();
                case 4 -> wrap.getInt();
                default -> wrap.getLong();
            };
            //return len == 1 ? bytes[0] : (len == 2 ? wrap.getShort() : (len == 4 ? wrap.getInt() : wrap.getLong()));
        }catch (Exception e){
            e.printStackTrace();
            return 0;
        }*/
    }

    public byte read(){
        return read(1)[0];
    }

    public short readShort(){
        var buff = read(2);
        return (short) (((buff[0] & 0xff) << 8) | (buff[1] & 0xff));
    }

    public int readInt(){
        var buff = read(4);
        return ((buff[0] & 0xff) << 24) |
        ((buff[1] & 0xff) << 16) |
        ((buff[2] & 0xff) << 8) |
        (buff[3] & 0xff);
    }

    public long readLong(){
        var buff = read(8);
        return ((long) (buff[0] & 0xff) << 56) |
        ((long) (buff[1] & 0xff) << 48) |
        ((long) (buff[2] & 0xff) << 40) |
        ((long) (buff[3] & 0xff) << 32) |
        ((long) (buff[4] & 0xff) << 24) |
        ((buff[5] & 0xff) << 16) |
        ((buff[6] & 0xff) << 8) |
                (buff[7] & 0xff);
    }

    public String readString(int len) {
        try{
            return new String(stream.readNBytes(len), StandardCharsets.UTF_8);
        }catch (IOException e){
            Logger.getLogger().log(e);
            return null;
        }
    }

    public boolean check(){
        return stream.available() > 0;
    }
}
