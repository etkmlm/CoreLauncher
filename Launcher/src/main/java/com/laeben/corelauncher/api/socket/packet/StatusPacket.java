package com.laeben.corelauncher.api.socket.packet;

import java.io.IOException;

public class StatusPacket {

    public enum InGameType{
        IDLING, SINGLEPLAYER, MULTIPLAYER
    }

    private final CLPacket packet;

    private final InGameType type;

    private String data;

    public StatusPacket(CLPacket packet) throws IOException {
        this.packet = packet;
        type = InGameType.values()[packet.readInt()];
        if (type == InGameType.IDLING)
            return;

        data = packet.readString();
    }

    public InGameType getType(){
        return type;
    }

    public String getData(){
        return data;
    }
}
