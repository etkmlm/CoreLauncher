package com.laeben.corelauncher.discord.entity;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Data {
    public enum OpCode{
        DISPATCH, HEARTBEAT, IDENTIFY, PRESENCE_UPDATE
    }

    private final OpCode code;
    private final String data;

    private Data(OpCode code, String data) {
        this.code = code;
        this.data = data;
    }

    public static Data create(OpCode code, String data) {
        return new Data(code, data);
    }

    public OpCode getCode(){
        return code;
    }

    public String getData(){
        return data;
    }

    public ByteBuffer buff(){
        var db = data.getBytes(StandardCharsets.UTF_8);
        var buff = ByteBuffer.allocate(Integer.BYTES * 2 + db.length);
        buff.putInt(Integer.reverseBytes(code.ordinal()));
        buff.putInt(Integer.reverseBytes(db.length));
        buff.put(db);
        return buff.position(0);
    }
}
