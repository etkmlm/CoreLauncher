package com.laeben.corelauncher.api.socket.entity;

public class CLStatusPacket{

    public enum InGameType{
        IDLING, SINGLEPLAYER, MULTIPLAYER
    }

    private final CLPacket packet;

    private final InGameType type;

    private String data;

    public CLStatusPacket(CLPacket packet){
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
