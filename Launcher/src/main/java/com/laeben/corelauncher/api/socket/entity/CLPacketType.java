package com.laeben.corelauncher.api.socket.entity;

import java.util.Map;
import java.util.Objects;

public record CLPacketType(int number) {

    // 0-512 will be reserved by the launcher itself
    public static final CLPacketType HANDSHAKE = new CLPacketType(0);
    public static final CLPacketType KEY_HANDSHAKE = new CLPacketType(1);
    public static final CLPacketType LAUNCH = new CLPacketType(100);
    public static final CLPacketType STATUS = new CLPacketType(101);
    public static final CLPacketType FILE = new CLPacketType(102);

    private static final Map<Integer, CLPacketType> map = Map.of(
            HANDSHAKE.number, HANDSHAKE,
            KEY_HANDSHAKE.number, KEY_HANDSHAKE,
            LAUNCH.number, LAUNCH,
            STATUS.number, STATUS,
            FILE.number, FILE);

    public static CLPacketType fromNumber(int number) {
        var got = map.get(number);

        return got == null ? new CLPacketType(number) : got;
    }

    @Override
    public String toString() {
        return String.valueOf(number);
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof CLPacketType t && t.number == number;
    }

    @Override
    public int hashCode(){
        return Objects.hash(number);
    }
}
