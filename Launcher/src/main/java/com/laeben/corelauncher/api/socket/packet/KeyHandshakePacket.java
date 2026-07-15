package com.laeben.corelauncher.api.socket.packet;

import com.laeben.corelauncher.api.socket.entity.CLPacketType;

import java.io.IOException;

public record KeyHandshakePacket(String name) {

    public CLPacket serialize() {
        return new CLPacket(CLPacketType.KEY_HANDSHAKE).writeString(name);
    }

    public static KeyHandshakePacket fromPacket(CLPacket packet) throws IOException {
        return new KeyHandshakePacket(packet.readString());
    }

}
