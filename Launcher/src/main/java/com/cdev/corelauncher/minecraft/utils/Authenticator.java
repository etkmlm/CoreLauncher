package com.cdev.corelauncher.minecraft.utils;

import com.cdev.corelauncher.data.entities.Account;
import com.cdev.corelauncher.utils.NetUtils;
import com.cdev.corelauncher.utils.OSUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.regex.Pattern;

public class Authenticator {
    private static final String CLIENT = "957eb4bd-6505-49e7-b2b6-12cf64e8cacc";
    private static final String LOCALHOST = "http://localhost:4567";
    private static final String CODE_URL = "https://login.live.com/oauth20_authorize.srf?" +
            "client_id=" + CLIENT +
            "&response_type=code" +
            "&redirect_uri=" + LOCALHOST +
            "&scope=XboxLive.signin offline_access";

    private static final String TOKEN_URL = "https://login.live.com/oauth20_token.srf";

    private static final String AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate";
    private static final String XSTS_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";

    private static final String MC_AUTH_URL = "https://api.minecraftservices.com/authentication/login_with_xbox";

    private static final String CHECK_URL = "https://api.minecraftservices.com/entitlements/mcstore";

    public Authenticator(){

    }

    public String listen(String parameter) {
        String regex = ".*" + parameter + "=(.*) HTTP.*";
        Pattern p = Pattern.compile(regex, Pattern.DOTALL);
        String s = NetUtils.listenServer(4567);
        if (s == null)
            return null;
        var m = p.matcher(s);
        return m.matches() ? m.group(1) : null;
    }

    public Account authenticate(Account a){
        OSUtils.openURL(CODE_URL);
        String code = listen("code");

        String token_con = "client_id=" + CLIENT +
                "&code=" + code +
                "&grant_type=authorization_code&redirect_uri=" + LOCALHOST;

        var gson = new Gson();

        var fx = gson.fromJson(NetUtils.post(TOKEN_URL, token_con, "application/x-www-form-urlencoded"), JsonObject.class);

        String accessToken = fx.get("access_token").getAsString();
        String refreshAccessToken = fx.get("refresh_token").getAsString();

        String json =
                """
                {
                   "Properties": {
                       "AuthMethod": "RPS",
                       "SiteName": "user.auth.xboxlive.com",
                       "RpsTicket": "d=$"
                   },
                   "RelyingParty": "http://auth.xboxlive.com",
                   "TokenType": "JWT"
                }
                """.replace("$", accessToken);
        var prc = gson.fromJson(NetUtils.post(AUTH_URL, json, "application/json"), JsonObject.class);

        String xblToken = prc.get("Token").getAsString();
        var arr = prc.get("DisplayClaims").getAsJsonObject().get("xui").getAsJsonArray().asList().stream().filter(x -> x.getAsJsonObject().has("uhs")).findFirst();
        String hash = "";
        if (arr.isPresent())
            hash = arr.get().getAsJsonObject().get("uhs").getAsString();

        String xjson = """
                {         
                    "Properties": { 
                        "SandboxId": "RETAIL",
                        "UserTokens": [
                            "$"
                        ] 
                    },  
                    "RelyingParty": "rp://api.minecraftservices.com/",  
                    "TokenType": "JWT"   
                 }
                """.replace("$", xblToken);

        String authToken = gson.fromJson(NetUtils.post(XSTS_URL, xjson, "application/json"), JsonObject.class).get("Token").getAsString();

        String fjson = "{ \"identityToken\": \"XBL3.0 x=" + hash + ";" + authToken + "\" }";
        var user = gson.fromJson(NetUtils.post(MC_AUTH_URL, fjson, "application/json"), JsonObject.class);

        String name = user.get("username").getAsString();
        String mcAccessToken = user.get("access_token").getAsString();


        return a;
    }
}
