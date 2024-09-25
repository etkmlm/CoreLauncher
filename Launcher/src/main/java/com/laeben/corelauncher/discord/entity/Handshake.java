package com.laeben.corelauncher.discord.entity;

public class Handshake {
    public int v;
    public String client_id;
    public String nonce;

    public static Handshake create(String client){
        Handshake h = new Handshake();
        h.client_id = client;
        h.v = 1;
        return h;
    }
}
