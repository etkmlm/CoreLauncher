package com.cdev.corelauncher.data.entities;

public class Account {
    private final String username;
    private String token;

    private Account(String username){
        this.username = username;
    }

    public static Account fromUsername(String username){
        return new Account(username);
    }

    public String getUsername(){
        return username;
    }
}
