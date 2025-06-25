package com.laeben.corelauncher.minecraft.util;

import com.laeben.core.entity.RequestParameter;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.exception.PerformException;
import com.laeben.corelauncher.api.util.OSUtil;
import com.laeben.corelauncher.util.APIListener;
import com.laeben.corelauncher.util.GsonUtil;
import com.laeben.corelauncher.api.util.NetUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.laeben.corelauncher.util.entity.LogType;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

public class Authenticator {
    public record AuthInfo(String username, String acessToken, int expiresIn){}
    public record XblInfo(String xbl, String refresh, int expiresIn){

    }
    private static final String CLIENT = "957eb4bd-6505-49e7-b2b6-12cf64e8cacc";
    private static final int LOCALHOST_START_PORT = 57130;
    private static final int LOCALHOST_END_PORT = 57150;
    private static final String MICROSOFT_X = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
    private static final String CODE_URL = "https://login.live.com/oauth20_authorize.srf?" +
            "client_id=" + CLIENT +
            "&response_type=code" +
            "&redirect_uri=$" +
            "&scope=XboxLive.signin offline_access";

    private static final String TOKEN_URL = "https://login.live.com/oauth20_token.srf";

    private static final String AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate";
    private static final String XSTS_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";

    private static final String MC_AUTH_URL = "https://api.minecraftservices.com/authentication/login_with_xbox";

    private static final String CHECK_URL = "https://api.minecraftservices.com/entitlements/mcstore";

    private static Authenticator instance;

    private boolean listenPortBindFail = false;

    private final Gson gson;

    public Authenticator(){
        instance = this;

        gson = GsonUtil.EMPTY_GSON;
    }

    public static Authenticator getAuthenticator(){
        return instance;
    }

    public void bindFail(){
        listenPortBindFail = true;
    }

    public String listen(String parameter, int port) {
        String regex = ".*" + parameter + "=(.*) HTTP.*";
        Pattern p = Pattern.compile(regex, Pattern.DOTALL);
        String r = APIListener.createClosePageRequest();

        String s = NetUtil.listenServer(port, r);
        if (s == null)
            return null;
        var m = p.matcher(s);
        return m.matches() ? m.group(1) : null;
    }

    public XblInfo refreshXbl(String refreshToken) throws NoConnectionException {
        String link = NetUtil.post(MICROSOFT_X,
                "client_id=" + CLIENT +
                "&scope=XboxLive.signin offline_access" +
                "&refresh_token=" + refreshToken +
                "&grant_type=refresh_token");

        var json = gson.fromJson(link, JsonObject.class);

        String token = json.get("access_token").getAsString();
        String rToken = json.get("refresh_token").getAsString();
        int ein = json.get("expires_in").getAsInt();
        return new XblInfo(token, rToken, ein);
    }

    public XblInfo getXbl(String code, String redirect) throws NoConnectionException {
        String token_con = "client_id=" + CLIENT +
                "&code=" + code +
                "&grant_type=authorization_code&redirect_uri=" + redirect;

        var fx = gson.fromJson(NetUtil.post(TOKEN_URL, token_con, List.of(RequestParameter.contentType("application/x-www-form-urlencoded"))), JsonObject.class);

        String accessToken = fx.get("access_token").getAsString();
        String refreshAccessToken = fx.get("refresh_token").getAsString();
        int expiresIn = fx.get("expires_in").getAsInt();

        return new XblInfo(accessToken, refreshAccessToken, expiresIn);
    }

    public AuthInfo getATokenFromToken(String token) throws NoConnectionException, PerformException {
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
                """.replace("$", token);
        var prc = gson.fromJson(NetUtil.post(AUTH_URL, json, List.of(RequestParameter.contentType("application/json"))), JsonObject.class);

        String xblToken = prc.get("Token").getAsString();
        var arr = prc.get("DisplayClaims").getAsJsonObject().get("xui").getAsJsonArray().asList().stream().filter(x -> x.getAsJsonObject().has("uhs")).findFirst();
        String hash = "";
        if (arr.isPresent())
            hash = arr.get().getAsJsonObject().get("uhs").getAsString();

        String xjson =
                """
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

        var xstsResponse = gson.fromJson(NetUtil.post(XSTS_URL, xjson, List.of(RequestParameter.contentType("application/json"))), JsonObject.class);

        if (xstsResponse.has("XErr")){
            throw new PerformException("authFail", xstsResponse.get("XErr").getAsLong());
        }

        String authToken = xstsResponse.get("Token").getAsString();

        String fjson = "{ \"identityToken\": \"XBL3.0 x=" + hash + ";" + authToken + "\" }";
        var user = gson.fromJson(NetUtil.post(MC_AUTH_URL, fjson, List.of(RequestParameter.contentType("application/json"))), JsonObject.class);

        String name = null;
        //String name = user.get("username").getAsString();
        String mcAccessToken = user.get("access_token").getAsString();
        int expiresIn = user.get("expires_in").getAsInt();


        if (mcAccessToken != null){
            var base = mcAccessToken.split("\\.")[1];
            var js = gson.fromJson(new String(Base64.getDecoder().decode(base)), JsonObject.class);
            if (js.has("pfd")){
                var pfd = js.get("pfd").getAsJsonArray();
                if (!pfd.isEmpty())
                    name = pfd.get(0).getAsJsonObject().get("name").getAsString();
            }

        }

        return new AuthInfo(name, mcAccessToken, expiresIn);
    }

    public Tokener authenticate(String username) throws PerformException {
        try{
            int port = LOCALHOST_START_PORT;
            String redirect, code;

            do{
                redirect = "http://localhost:" + port;
                OSUtil.openURL(CODE_URL.replace("$", redirect));
                code = listen("code", port);

                if (code != null)
                    break;

                port++;
            }
            while (port <= LOCALHOST_END_PORT);

            if (code == null){
                Logger.getLogger().log(LogType.ERROR, "Authentication Failed: code was null");
                return Tokener.empty(username);
            }

            var xbl = getXbl(code, redirect);

            var info = getATokenFromToken(xbl.xbl);

            return new Tokener(xbl, info);
        } catch (IOException | NoConnectionException e){
            return Tokener.empty(username);
        }
        catch (Exception e){
            throw new PerformException("authFail", e);
        }
    }
}
