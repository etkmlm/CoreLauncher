package com.laeben.corelauncher.minecraft.util;

import com.laeben.core.entity.Path;
import com.laeben.core.entity.RequestParameter;
import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.core.network.Network;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.Account;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.entity.TokenInfo;
import com.laeben.corelauncher.api.entity.TokenStore;
import com.laeben.corelauncher.api.exception.PerformException;
import com.laeben.corelauncher.api.ui.UI;
import com.laeben.corelauncher.api.util.OSUtil;
import com.laeben.corelauncher.ui.control.CMsgBox;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.ui.controller.page.WebPage;
import com.laeben.corelauncher.util.APIListener;
import com.laeben.corelauncher.util.GsonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.laeben.corelauncher.util.entity.LogType;
import com.laeben.corelauncher.util.entity.UTCDateFactory;
import com.laeben.corelauncher.web.EmbeddedBrowser;
import com.laeben.corelauncher.web.WebComplex;
import javafx.scene.control.Alert;
import javafx.scene.control.Tab;

import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class Authenticator {
    public static final String KEY = "authtor";

    public record AuthInfo(String username, String accessToken, int expiresIn){}
    public record XblInfo(String xbl, String refresh, int expiresIn){}

    public static TokenInfo constructTokenInfo(AuthInfo auth, XblInfo xbl){
        return new TokenInfo(auth.username(), auth.accessToken(), xbl.refresh(), auth.expiresIn());
    }

    private static final String CLIENT = "957eb4bd-6505-49e7-b2b6-12cf64e8cacc";
    private static final int LOCALHOST_START_PORT = 57130;
    private static final int LOCALHOST_END_PORT = 57150;
    private static final String OAUTH_TOKEN_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
    private static final String CODE_URL = "https://login.live.com/oauth20_authorize.srf?" +
            "client_id=" + CLIENT +
            "&response_type=code" +
            "&redirect_uri=$" +
            "&scope=XboxLive.signin offline_access";

    private static final String AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate";
    private static final String XSTS_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";

    private static final String MC_AUTH_URL = "https://api.minecraftservices.com/authentication/login_with_xbox";
    private static final String VERIFY_URL = "https://api.minecraftservices.com/entitlements/mcstore";

    private static Authenticator instance;
    private TokenStore tokenStore;

    private boolean listenPortBindFail = false;

    private static final String CODE_STOP = "$";
    private static final String CODE_EMPTY = "";

    private final Gson gson;

    public Authenticator(){
        instance = this;

        gson = GsonUtil.EMPTY_GSON.newBuilder()
                .registerTypeAdapter(Date.class, new UTCDateFactory())
                .create();

        Configurator.getConfigurator().getHandler().addHandler(KEY, a -> {
            if (!a.getKey().equals(Configurator.GAME_PATH_CHANGE)) return;

            reloadTokenStore();
        }, true);
    }

    public static Authenticator getAuthenticator(){
        return instance;
    }

    public Path getTokenStorePath(){
        return Configurator.getConfig().getLauncherPath().to("auth", "tokens.json");
    }

    public TokenStore getTokenStore(){
        if (tokenStore == null)
            tokenStore = new TokenStore();

        return tokenStore;
    }

    public void reloadTokenStore(){
        var path = getTokenStorePath();
        if (!path.exists()){
            tokenStore = null;
            return;
        }

        tokenStore = gson.fromJson(path.read(), TokenStore.class);
    }

    public void saveTokenStore(){
        var path = getTokenStorePath();
        if (tokenStore == null) path.delete();
        else path.write(gson.toJson(tokenStore));
    }

    public void bindFail(){
        listenPortBindFail = true;
    }

    public String listen(String parameter, int port) {
        String regex = ".*" + parameter + "=(.*) HTTP.*";
        Pattern p = Pattern.compile(regex);
        String r = APIListener.createClosePageRequest();

        String s = Network.listenServer(port, r);
        if (s == null)
            return null;
        var m = p.matcher(s);
        return m.find() ? m.group(1) : null;
    }

    private XblInfo extractXbl(String body) throws PerformException, NoConnectionException {
        final String response = Network.post(OAUTH_TOKEN_URL, body);

        var json = gson.fromJson(response, JsonObject.class);

        var errorElement = json.get("error");
        if (errorElement != null){
            final String errorCode = errorElement.getAsString();
            final String errorDesc = json.get("error_description").getAsString();

            throw new PerformException(errorCode, errorDesc);
        }

        String token = json.get("access_token").getAsString();
        String rToken = json.get("refresh_token").getAsString();
        int ein = json.get("expires_in").getAsInt();
        return new XblInfo(token, rToken, ein);
    }
    private XblInfo refreshXbl(String refreshToken) throws NoConnectionException, PerformException {
        final String body = String.format(
                "client_id=%s" +
                "&scope=XboxLive.signin offline_access" +
                "&refresh_token=%s" +
                "&grant_type=refresh_token", CLIENT, refreshToken);

        return extractXbl(body);
    }
    private XblInfo getXbl(String code, String redirect) throws NoConnectionException, PerformException {
        final String body = String.format(
                        "client_id=%s" +
                        "&scope=XboxLive.signin offline_access" +
                        "&redirect_uri=%s" +
                        "&code=%s" +
                        "&grant_type=authorization_code", CLIENT, redirect, code);

        return extractXbl(body);
    }
    private AuthInfo getAccessTokenFromXBLToken(String token) throws NoConnectionException, PerformException {
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
        var prc = gson.fromJson(Network.post(AUTH_URL, json, List.of(RequestParameter.contentType("application/json"))), JsonObject.class);

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

        var xstsResponse = gson.fromJson(Network.post(XSTS_URL, xjson, List.of(RequestParameter.contentType("application/json"))), JsonObject.class);

        if (xstsResponse.has("XErr")){
            throw new PerformException("authFail", xstsResponse.get("XErr").getAsLong());
        }

        String authToken = xstsResponse.get("Token").getAsString();

        String fjson = "{ \"identityToken\": \"XBL3.0 x=" + hash + ";" + authToken + "\" }";
        var user = gson.fromJson(Network.post(MC_AUTH_URL, fjson, List.of(RequestParameter.contentType("application/json"))), JsonObject.class);

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

    private void imitateCodeRedirect(String redirect, String code){
        try {
            Network.urlToString(redirect + "?code=" + code);
        } catch (Exception ignored) {

        }
    }
    private boolean startEmbeddedAuthentication(String redirect, String codeUrl) throws StopException {
        final var state = UI.runSync(() -> EmbeddedBrowser.getInstance()
                .getWebComplex(10000)
                .onTimeoutReached(event -> {
                    if (event.location() != null && event.location().equals(codeUrl))
                        WebPage.open(Translator.translate("browser.title.auhenticate")).reload(event.complex());
                })
                .onLocationChanged(event -> {
                    Logger.getLogger().logDebug("Authentication Browser: Location changed.");
                    if (event.isLocationEmpty()) return;
                    if (event.isLocationBlank()) {
                        imitateCodeRedirect(redirect, null);
                        return;
                    }

                    if (event.location().startsWith(redirect)){
                        Logger.getLogger().log(LogType.INFO, "Authentication Browser: Done.");
                        if (event.complex().getAttachedPage() != null)
                            Main.getMain().closeTab((Tab) event.complex().getAttachedPage().getParentObject());
                    }
                    else{
                        WebPage.open(Translator.translate("browser.title.auhenticate")).reload(event.complex());
                    }
                }).navigate(codeUrl).getState());

        if (state == WebComplex.State.OK)
            return true;

        boolean download = state == WebComplex.State.MISSING_LIBRARIES;

        final boolean enableListening = !download;

        if (download){
            var result = UI.runSync(() -> CMsgBox.msg(Alert.AlertType.CONFIRMATION, Translator.translate("ask.ask"), Translator.translate("browser.embedded.library"))
                    .setButtons(CMsgBox.ResultType.YES, CMsgBox.ResultType.NO)
                    .executeForResult());
            download = result.isPresent() && result.get().result().isPositive();
        }

        if (download){
            new Thread(() -> {
                String c = null;

                try {
                    Logger.getLogger().log(LogType.INFO, "Authentication Browser: Downloading necessary libraries...");
                    EmbeddedBrowser.downloadNativeLibraries();
                    c = CODE_STOP;
                }
                catch (NoConnectionException ignored) {

                } catch (HttpException e) {
                    Logger.getLogger().log(e);
                } catch (StopException ignored) {
                    c = CODE_STOP;
                }
                imitateCodeRedirect(redirect, c);
            }).start();
            throw new StopException();
        }

        return enableListening;
    }


    /**
     * Tries to retrieve a valid instance of the access token.
     * @param account source account
     * @return valid access token
     */
    public String getAccessToken(Account account) throws NoConnectionException, PerformException, StopException {
        if (!account.isOnline() || !Network.check()) return TokenInfo.DEFAULT_ACCESS_TOKEN;

        TokenInfo tokens = account.getTokens();

        if (tokens == null){
            tokens = getTokenStore().findTokens(account.getUsername());
            account.setTokens(tokens);
        }

        if (tokens != null){
            if (tokens.isAccessTokenValid()) return tokens.getAccessToken();
            else {
                XblInfo freshXbl = null;
                try{
                    freshXbl = refreshXbl(tokens.getRefreshToken());
                }
                catch (PerformException e){
                    if (!e.getMessage().equals("invalid_grant")) // refresh token failed, reauth
                        throw e;
                }
                if (freshXbl != null){
                    var authInfo = getAccessTokenFromXBLToken(freshXbl.xbl());

                    tokens = constructTokenInfo(authInfo, freshXbl);
                    getTokenStore().updateTokens(tokens);
                    saveTokenStore();

                    account.setTokens(tokens);
                    return tokens.getAccessToken();
                }
            }
        }

        tokens = authenticate(account.getUsername());
        getTokenStore().updateTokens(tokens);
        saveTokenStore();

        account.setTokens(tokens);

        return tokens.getAccessToken();
    }

    /**
     * Validates account access token.
     */
    public boolean validateAccessToken(Account account){
        if (!account.isOnline() || account.getTokens() == null)
            return false;

        JsonObject obj;
        try {
            String str = Network.urlToString(VERIFY_URL, List.of(RequestParameter.bearer(account.getTokens().getAccessToken())));

            obj = GsonUtil.DEFAULT_GSON.fromJson(str, JsonObject.class);
        } catch (NoConnectionException | HttpException ignored) {
            return false;
        }

        if (!obj.has("items"))
            return false;

        var items = obj.getAsJsonArray("items");
        return items.asList().stream().anyMatch(x -> x.getAsJsonObject().get("name").getAsString().equals("game_minecraft"));
    }

    /**
     * Performs the authentication through browser instances.
     * @return token info
     */
    public TokenInfo authenticate(String username) throws PerformException, StopException, NoConnectionException {
        try{
            int port = LOCALHOST_START_PORT;
            String redirect, code;

            do{
                redirect = "http://localhost:" + port;
                final String codeUrl = CODE_URL.replace("$", redirect);

                boolean doListen = true;

                if (Configurator.getConfig().useExternalAuth())
                    OSUtil.openURL(CODE_URL.replace("$", redirect));
                else{
                    // auth with embedded browser
                    Logger.getLogger().logDebug("Authentication Browser: Starting...");
                    doListen = startEmbeddedAuthentication(redirect, codeUrl);
                }

                if (!doListen){
                    code = null;
                    break;
                }
                code = listen("code", port);

                if (code != null)
                    break;

                port++;
            }
            while (port <= LOCALHOST_END_PORT);

            if (code == null || code.isBlank()){
                throw new PerformException("listenFail", "code was null.");
            }

            if (code.equals(CODE_STOP)) throw new StopException();

            var xbl = getXbl(code, redirect);

            var info = getAccessTokenFromXBLToken(xbl.xbl);

            return constructTokenInfo(info, xbl);
        }
        catch (NoConnectionException | StopException | PerformException e){
            throw e;
        }
        catch (Exception e){
            throw new PerformException("authFail", e);
        }
    }
}
