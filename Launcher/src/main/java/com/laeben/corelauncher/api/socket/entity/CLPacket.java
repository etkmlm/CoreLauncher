package com.laeben.corelauncher.api.socket.entity;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class CLPacket {
    private final CLPacketType type;
    private final ByteBuffer buffer;

    private CLPacket(ByteBuffer buffer){
        type = CLPacketType.fromNumber(buffer.getInt());
        this.buffer = buffer;
    }

    public static CLPacket fromArrayBuffer(byte[] buffer){
        return new CLPacket(ByteBuffer.wrap(buffer));
    }

    public CLPacketType getType(){
        return type;
    }

    public String readString(){
        int size = buffer.getInt();
        byte[] bytes = new byte[size];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public int readInt(){
        return buffer.getInt();
    }

    public long readLong(){
        return buffer.getLong();
    }
}
