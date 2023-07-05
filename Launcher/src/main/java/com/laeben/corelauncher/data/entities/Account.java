package com.laeben.corelauncher.data.entities;

import com.laeben.corelauncher.minecraft.utils.Authenticator;
import com.laeben.corelauncher.minecraft.utils.Tokener;
import com.laeben.corelauncher.utils.GsonUtils;
import com.laeben.corelauncher.utils.Logger;
import com.laeben.corelauncher.utils.NetUtils;
import com.laeben.corelauncher.utils.Requester;
import com.laeben.corelauncher.utils.entities.NoConnectionException;
import com.google.gson.*;
import javafx.scene.image.Image;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;

import java.lang.reflect.Type;
import java.nio.IntBuffer;
import java.util.Base64;

public class Account{
    private static final String UUID_URL = "https://api.mojang.com/profiles/minecraft";
    private static final String PROFILE_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";

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

    private Account(String username){
        this.username = username;
    }

    public Account authenticate(){
        if (!isOnline || !NetUtils.check())
            return this;
        if (tokener == null)
            tokener = Authenticator.getAuthenticator().authenticate();
        return this;
    }

    public Tokener getTokener(){
        if (tokener == null)
            return new Tokener(new Authenticator.XblInfo("", "", 1000), new Authenticator.AuthInfo(username, "T", 1000));
        return tokener;
    }

    public static Account fromUsername(String username){
        return new Account(username);
    }


    public String getUsername(){
        return username;
    }

    public Account reload(){

        if (isReloaded)
            return this;

        try{
            String accInfoJson = NetUtils.post(UUID_URL, "[\"" + username + "\"]", Requester.Parameter.contentType("application/json"));
            var accInfo = GsonUtils.empty().fromJson(accInfoJson, JsonArray.class);
            if (accInfo.size() > 0){

                uuid = accInfo.get(0).getAsJsonObject().get("id").getAsString();

                String js = NetUtils.urlToString(PROFILE_URL + uuid);

                var properties = GsonUtils.empty().fromJson(js, JsonObject.class).get("properties").getAsJsonArray();

                var b64textures = properties.asList().stream().map(JsonElement::getAsJsonObject).filter(x -> x.get("name").getAsString().equals("textures")).findFirst().orElse(null);
                String base64profile = b64textures == null ? null : b64textures.get("value").getAsString();

                var textures = GsonUtils.empty().fromJson(new String(Base64.getDecoder().decode(base64profile)), JsonObject.class).get("textures").getAsJsonObject();

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
        catch (NoConnectionException ignored){

        }
        catch (Exception e){
            Logger.getLogger().log(e);
        }

        reloadHead();

        isReloaded = true;

        return this;
    }

    public Account setOnline(boolean isOnline){
        this.isOnline = isOnline;

        return this;
    }

    public boolean isOnline(){
        return isOnline;
    }

    private void reloadHead(){
        if (skin == null){
            skin = getClass().getResource("/com/laeben/corelauncher/images/steve.png").toString();
        }

        try{
            Image img = new Image(skin);

            head = resize(img, 8, 8, 8, 8, 32);
        }
        catch (Exception e){
            head = null;
        }
    }

    private static Image resize(Image original, int x, int y, int w, int h, int scaleFactor){
        int newWidth = w * scaleFactor;
        int newHeight = h * scaleFactor;

        var reader = original.getPixelReader();

        int[] fx = new int[w * h];
        reader.getPixels(x, y, w, h, WritablePixelFormat.getIntArgbInstance(), fx, 0, 8);

        int[][] nMatrix = new int[newWidth][newHeight];

        for (int i = 0; i < w; i++)
            for (int j = 0; j < h; j++) {
                var color = fx[j * w + i];
                for (int f = 0; f < scaleFactor; f++)
                    for (int s = 0; s < scaleFactor; s++)
                        nMatrix[i * scaleFactor + f][j * scaleFactor + s] = color;
            }

        IntBuffer buffer = IntBuffer.allocate(newWidth * newHeight * 4);

        for (int i = 0; i < newWidth; i++)
            for (int j = 0; j < newHeight; j++){
                buffer.put(j * newWidth + i, nMatrix[i][j]);
            }

        PixelBuffer<IntBuffer> pixBuffer = new PixelBuffer<>(newWidth, newHeight, buffer, WritablePixelFormat.getIntArgbPreInstance());

        return new WritableImage(pixBuffer);
    }

    public Image getHead(){
        return head;
    }
    public String getUuid(){
        return uuid;
    }
}
