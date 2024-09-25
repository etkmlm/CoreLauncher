package com.laeben.corelauncher.discord.entity.response;

public class HandshakeResponse {
    public Integer code;
    public String message;
    public Object data;

    public boolean isSuccessful(){
        return code == null && data != null;
    }
}
