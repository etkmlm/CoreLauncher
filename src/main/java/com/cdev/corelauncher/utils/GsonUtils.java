package com.cdev.corelauncher.utils;

import com.cdev.corelauncher.data.entities.Account;
import com.cdev.corelauncher.data.entities.Language;
import com.cdev.corelauncher.utils.entities.Path;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonUtils {
    public static final Gson DEFAULT_GSON = new GsonBuilder()
            .registerTypeAdapter(Path.class, new Path.PathFactory())
            .registerTypeAdapter(Account.class, new Account.AccountFactory())
            .registerTypeAdapter(Language.class, new Language.LanguageFactory())
            .create();
}
