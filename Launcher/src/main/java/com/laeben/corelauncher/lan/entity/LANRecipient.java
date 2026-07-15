package com.laeben.corelauncher.lan.entity;

import com.laeben.corelauncher.lan.exception.InvalidKeyException;

import java.net.Inet4Address;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HexFormat;

public record LANRecipient(byte[] ip, int port, String name) {

    @Override
    public int hashCode(){
        return Arrays.hashCode(ip);
    }

    @Override
    public boolean equals(Object o){
        return this == o || o instanceof LANRecipient && o.hashCode() == hashCode();
    }

    public static LANRecipient fromSocket(Socket socket){
        return new LANRecipient(socket.getInetAddress().getAddress(), socket.getLocalPort(), socket.getInetAddress().getHostName());
    }
    public static LANRecipient fromKey(String key){
        return fromKey(key, null);
    }
    public static LANRecipient fromKey(String key, String name){
        if (key.length() < 9) throw new InvalidKeyException(key + " was invalid.");

        try{
            final byte[] ip = HexFormat.of().parseHex(key, 0, 8);
            final int port = HexFormat.fromHexDigits(key, 8, key.length());

            return new LANRecipient(ip, port, name);
        }
        catch (Exception e){
            throw new InvalidKeyException("%s was invalid. (%s)".formatted(key, e.getMessage()));
        }
    }

    public static String getKey(byte[] ip, int port){
        return HexFormat.of().formatHex(ip).toUpperCase() + String.format("%2X", port);
    }

    public String getKey(){
        return getKey(this.ip, this.port);
    }

    public String getNetworkIdentifier() throws UnknownHostException {
        return Inet4Address.getByAddress(ip).getHostAddress() + ":" + port;
    }

    @Override
    public String toString(){
        return name != null ? name : getKey();
    }
}
