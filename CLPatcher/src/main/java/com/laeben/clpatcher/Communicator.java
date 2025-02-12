package com.laeben.clpatcher;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Communicator {
    private static final int STATUS_CODE = 101;

    private static final PrintStream print = new PrintStream(new FileOutputStream(FileDescriptor.out));
    private static final byte[] IDENTIFIER = "clstatus".getBytes();


    public enum InGameType{
        IDLING, SINGLEPLAYER, MULTIPLAYER
    }

    private static void send(InGameType type, ByteBuffer buff){
        int size = 4 + 4;
        byte[] data = null;
        if (buff != null){
            data = buff.array();
            size += data.length;
        }
        ByteBuffer buffer = ByteBuffer.allocate(IDENTIFIER.length + size);

        buffer.put(IDENTIFIER);
        buffer.putInt(STATUS_CODE);

        // inner data
        buffer.putInt(type.ordinal());
        if (data != null)
            buffer.put(data);
        print.println(new String(buffer.array(), StandardCharsets.UTF_8));
    }

    public static void send(InGameType type, String message){
        if (message == null){
            send(type, (ByteBuffer) null);
            return;
        }
        byte[] bytes = message.getBytes();
        send(type, ByteBuffer.allocate(4 + bytes.length).putInt(bytes.length).put(bytes));
    }
}
