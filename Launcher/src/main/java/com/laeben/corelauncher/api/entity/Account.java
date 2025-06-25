package com.laeben.corelauncher.api.entity;

import com.laeben.core.entity.RequestParameter;
import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.corelauncher.api.exception.PerformException;
import com.laeben.corelauncher.minecraft.util.Authenticator;
import com.laeben.corelauncher.minecraft.util.Tokener;
import com.laeben.corelauncher.util.GsonUtil;
import com.laeben.corelauncher.api.util.NetUtil;
import com.google.gson.*;
import com.laeben.corelauncher.util.ImageUtil;
import com.laeben.corelauncher.util.entity.LogType;
import javafx.scene.image.Image;

import java.lang.reflect.Type;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

public class Account{
    private static final String UUID_URL = "https://api.minecraftservices.com/minecraft/profile/lookup/bulk/byname";
    private static final String PROFILE_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";
    private static final String VERIFY_URL = "https://api.minecraftservices.com/entitlements/mcstore";

    public static final class AccountFactory implements JsonSerializer<Account>, JsonDeserializer<Account> {
        @Override
        public Account deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            var obj = jsonElement.getAsJsonObject();
            boolean isOnline = false;
            if (obj.has("isOnline"))
                isOnline = obj.get("isOnline").getAsBoolean();
            return Account.fromUsername(obj.get("name").getAsString()).setOnline(isOnline);
        }

        @Override
        public JsonElement serialize(Account account, Type type, JsonSerializationContext jsonSerializationContext) {
            var obj = new JsonObject();
            obj.add("name", new JsonPrimitive(account.username));
            obj.add("isOnline", new JsonPrimitive(account.isOnline));
            return obj;
        }
    }
    private final String username;
    private Tokener tokener;
    private String uuid;
    private String skin;
    private String cape;
    private Image head;
    private boolean isReloaded;
    private boolean isOnline;

    private transient String _token;

    private Account(String username){
        this.username = username;
    }

    public Account copyAs(String username){
        var account = new Account(username);
        account.tokener = tokener;
        account.isOnline = isOnline;
        return account;
    }

    /**
     * Authenticate the account with Mojang.
     * @return the account
     */
    public Account authenticate() throws PerformException {
        if (!isOnline || !NetUtil.check())
            return this;
        if (tokener == null)
            tokener = Authenticator.getAuthenticator().authenticate(username);
        return this;
    }

    public void cacheToken() throws PerformException {
        _token = isOnline() ? authenticate().getTokener().getAccessToken() : "null";
    }

    public String getCachedToken(){
        return _token == null ? "null" : _token;
    }

    public Tokener getTokener(){
        if (tokener == null)
            return Tokener.empty(username);
        return tokener;
    }

    /**
     * Create a new account from the username.
     * @param username the username
     * @return new account
     */
    public static Account fromUsername(String username){
        return new Account(username);
    }


    /**
     * Validates account access token.
     */
    public boolean validate(){
        if (tokener == null)
            return false;

        JsonObject obj;
        try {
            String str = NetUtil.urlToString(VERIFY_URL, List.of(RequestParameter.bearer(tokener.getAccessToken())));

            obj = GsonUtil.DEFAULT_GSON.fromJson(str, JsonObject.class);
        } catch (NoConnectionException | HttpException | PerformException ignored) {
            return false;
        }

        if (!obj.has("items"))
            return false;

        var items = obj.getAsJsonArray("items");
        return items.asList().stream().anyMatch(x -> x.getAsJsonObject().get("name").getAsString().equals("game_minecraft"));
    }

    public String getUsername(){
        return username;
    }

    /**
     * Reload the account.
     * <br/>
     * Helps to retrieve the uuid, skin, cape, and head.
     * @return the account
     */
    public Account reload(){
        if (isReloaded)
            return this;

        try{
            String accInfoJson = NetUtil.post(UUID_URL, "[\"" + username + "\"]", List.of(RequestParameter.contentType("application/json")));
            if (!accInfoJson.startsWith("[")){ // 403 Forbidden
                Logger.getLogger().log(LogType.WARN, "Cannot acquire account '" + username + "', response from server: \n" + accInfoJson);
                throw new NoConnectionException();
            }
            var accInfo = GsonUtil.EMPTY_GSON.fromJson(accInfoJson, JsonArray.class);
            if (!accInfo.isEmpty()){

                uuid = accInfo.get(0).getAsJsonObject().get("id").getAsString();

                String js = NetUtil.urlToString(PROFILE_URL + uuid);

                if (js != null){
                    var properties = GsonUtil.EMPTY_GSON.fromJson(js, JsonObject.class).get("properties").getAsJsonArray();

                    var b64textures = properties.asList().stream().map(JsonElement::getAsJsonObject).filter(x -> x.get("name").getAsString().equals("textures")).findFirst().orElse(null);
                    String base64profile = b64textures == null ? null : b64textures.get("value").getAsString();

                    var textures = GsonUtil.EMPTY_GSON.fromJson(new String(Base64.getDecoder().decode(base64profile)), JsonObject.class).get("textures").getAsJsonObject();

                    var skin = textures.get("SKIN");
                    if (skin != null)
                        this.skin = skin.getAsJsonObject().get("url").getAsString();
                    else
                        this.skin = null;

                    var cape = textures.get("CAPE");
                    if (cape != null)
                        this.cape = cape.getAsJsonObject().get("url").getAsString();
                    else
                        this.cape = null;
                }
            }

        }
        catch (NoConnectionException | NullPointerException ignored){

        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }

        reloadHead();

        isReloaded = true;

        return this;
    }

    /**
     * Mark the account as Mojang or not.
     * @param isOnline is online
     */
    public Account setOnline(boolean isOnline){
        this.isOnline = isOnline;

        return this;
    }

    public boolean isOnline(){
        return isOnline;
    }


    private void reloadHead(){
        if (skin == null){
            skin = Objects.requireNonNull(getClass().getResource("/com/laeben/corelauncher/images/steve.png")).toString();
        }

        try{
            Image img = new Image(skin);

            head = ImageUtil.resizeImage(img, 8, 8, 8, 8, 32);
        }
        catch (Exception e){
            head = null;
        }
    }

    public Image getHead(){
        return head;
    }
    public String getUuid(){
        return uuid;
    }
}
