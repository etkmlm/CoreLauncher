package com.laeben.corelauncher.api.socket.entity;

public enum CLPacketType {
    HANDSHAKE(0), LAUNCH(100), STATUS(101);

    private final int number;

    CLPacketType(int number){
        this.number = number;
    }

    public int getNumber(){
        return number;
    }

    public static CLPacketType fromNumber(int number){
        for(CLPacketType type : values()){
            if (type.getNumber() == number)
                return type;
        }
        return null;
    }

    @Override
    public String toString(){
        return String.valueOf(number);
    }
}
