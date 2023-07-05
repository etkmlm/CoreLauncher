package com.laeben.corelauncher.data.nbt.util;

import com.laeben.corelauncher.utils.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

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

    public long read(int len){
        try{
            long r = 0;
            var bytes = stream.readNBytes(len);
            for (int i = len - 1; i >= 0; i--){
                var b = bytes[i];
                r |= ((long)b & 0xff) << (len - i - 1) * 8;
            }

            return r;
        }
        catch (Exception e){
            Logger.getLogger().log(e);
            return 0;
        }
        /*try{
            var bytes = stream.readNBytes(len);
            var wrap = ByteBuffer.wrap(bytes);

            return len == 1 ? bytes[0] : (len == 2 ? wrap.getShort() : (len == 4 ? wrap.getInt() : wrap.getLong()));
        }catch (Exception e){
            e.printStackTrace();
            return 0;
        }*/
    }

    public byte read(){
        return (byte)read(1);
    }

    public short readShort(){
        return (short) read(2);
    }

    public int readInt(){
        return (int) read(4);
    }

    public long readLong(){
        return read(8);
    }

    public String readString(int len) {
        try{
            return new String(stream.readNBytes(len));
        }catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }

    public boolean check(){
        return stream.available() > 0;
    }
}
