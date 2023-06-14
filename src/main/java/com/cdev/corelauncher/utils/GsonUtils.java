package com.cdev.corelauncher.utils;

import com.cdev.corelauncher.data.entities.Account;
import com.cdev.corelauncher.utils.entities.Java;
import com.cdev.corelauncher.utils.entities.Path;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonUtils {
    public static final Gson DEFAULT_GSON = new GsonBuilder()
            .registerTypeAdapter(Path.class, new Path.PathFactory())
            .registerTypeAdapter(Account.class, new Account.AccountFactory())
            .registerTypeAdapter(Java.class, new Java.JavaFactory())
            .create();
}
