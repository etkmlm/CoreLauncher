package com.cdev.corelauncher.data.entities;

import com.google.gson.*;

import java.lang.reflect.Type;

public class Account{
    public static final class AccountFactory implements JsonSerializer<Account>, JsonDeserializer<Account> {
        @Override
        public Account deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            return Account.fromUsername(jsonElement.getAsString());
        }

        @Override
        public JsonElement serialize(Account account, Type type, JsonSerializationContext jsonSerializationContext) {
            return new JsonPrimitive(account.username);
        }
    }
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
