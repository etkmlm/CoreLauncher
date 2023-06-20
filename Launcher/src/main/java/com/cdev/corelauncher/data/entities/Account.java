package com.cdev.corelauncher.data.entities;

import com.cdev.corelauncher.utils.Logger;
import com.cdev.corelauncher.utils.NetUtils;
import com.cdev.corelauncher.utils.entities.NoConnectionException;
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
            return Account.fromUsername(jsonElement.getAsString());
        }

        @Override
        public JsonElement serialize(Account account, Type type, JsonSerializationContext jsonSerializationContext) {
            return new JsonPrimitive(account.username);
        }
    }
    private final String username;
    private String token;
    private String uuid;
    private String skin;
    private String cape;
    private Image head;
    private boolean isReloaded;
    private boolean isMojangUser;

    private Account(String username){
        this.username = username;
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
            String accInfoJson = NetUtils.post(UUID_URL, "[\"" + username + "\"]", "application/json");
            var accInfo = new Gson().fromJson(accInfoJson, JsonArray.class);
            if (isMojangUser = accInfo.size() > 0){

                uuid = accInfo.get(0).getAsJsonObject().get("id").getAsString();

                String js = NetUtils.urlToString(PROFILE_URL + uuid);

                var properties = new Gson().fromJson(js, JsonObject.class).get("properties").getAsJsonArray();

                var b64textures = properties.asList().stream().map(JsonElement::getAsJsonObject).filter(x -> x.get("name").getAsString().equals("textures")).findFirst().orElse(null);
                String base64profile = b64textures == null ? null : b64textures.get("value").getAsString();

                var textures = new Gson().fromJson(new String(Base64.getDecoder().decode(base64profile)), JsonObject.class).get("textures").getAsJsonObject();

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

    public boolean mojangUser(){
        return isMojangUser;
    }

    private void reloadHead(){
        if (skin == null){
            skin = getClass().getResource("/com/cdev/corelauncher/images/steve.png").toString();
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
