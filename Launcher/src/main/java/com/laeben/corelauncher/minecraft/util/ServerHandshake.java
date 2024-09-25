package com.laeben.corelauncher.minecraft.util;

import com.google.gson.JsonObject;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.util.GsonUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public class ServerHandshake {
    public record Response(int players, int maxPlayers){

    }

    private static final int NUMBER = 127;
    private static final int INDICATOR = 128;


    public static int writeVarInt(int num, ByteBuffer buff){
        int w = 0;
        while(true){
            w++;

            if ((num & 255) <= 127){
                buff.put((byte) num);
                break;
            }

            buff.put((byte) ((num & NUMBER) | INDICATOR));

            num >>>=7;
        }

        return w;
    }

    public static int readVarInt(InputStream buff) throws IOException {
        int pos = 0;
        int num = 0;
        while(true){
            int b = buff.read();
            num |= (b & NUMBER) << (pos * 7);

            if ((b & INDICATOR) == 0)break;

            pos++;
        }

        return num;
    }

    public static int readVarInt(ByteBuffer buff) {
        int pos = 0;
        int num = 0;
        while(true){
            int b = buff.get();
            num |= (b & NUMBER) << (pos * 7);

            if ((b & INDICATOR) == 0)break;

            pos++;
        }

        return num;
    }

    public static Response shake(String ip, int port){
        byte[] ipBytes = ip.getBytes();
        byte size = (byte) (1 + 1 + 1 + ipBytes.length + 2 + 1);
        var buffer = ByteBuffer.allocate(1 + size);
        buffer.put(size);

        buffer.put((byte) 0); // packet id 0 protocol
        buffer.put((byte) 47); // protocol version 1.8
        buffer.put((byte) ipBytes.length);
        buffer.put(ipBytes);
        buffer.putShort((short) port);
        buffer.put((byte) 1); // status indicator
        var request = buffer.array();

        ByteBuffer responseBytes = null;
        try(var sock = new Socket(ip, port)){
            sock.getOutputStream().write(request);
            sock.getOutputStream().write(new byte[]{
                    1, //size of the data
                    0 // data - packet id 0
            });
            int allSize = readVarInt(sock.getInputStream());
            responseBytes = ByteBuffer.wrap(sock.getInputStream().readNBytes(allSize));
        } catch (IOException e) {
            Logger.getLogger().log(e);
        }

        if (responseBytes == null)
            return null;

        byte protocol = responseBytes.get();
        var gfd = readVarInt(responseBytes);
        byte[] response = new byte[gfd];
        responseBytes.get(response);
        var obj = GsonUtil.empty().fromJson(new String(response), JsonObject.class);

        if (!obj.has("players"))
            return null;

        var players = obj.get("players").getAsJsonObject();
        int online = players.get("online").getAsInt();
        int max = players.get("max").getAsInt();

        return new Response(online, max);
    }
}
