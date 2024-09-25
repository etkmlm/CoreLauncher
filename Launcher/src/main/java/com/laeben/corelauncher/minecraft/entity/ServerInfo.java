package com.laeben.corelauncher.minecraft.entity;

public record ServerInfo(String ip, int port) {
    @Override
    public String toString(){
        return ip + ":" + port;
    }
}
