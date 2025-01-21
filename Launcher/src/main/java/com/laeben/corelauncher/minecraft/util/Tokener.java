package com.laeben.corelauncher.minecraft.util;

import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.corelauncher.api.exception.PerformException;

import java.time.Instant;
import java.util.Date;

public class Tokener {
    private Authenticator.AuthInfo auth;
    private Authenticator.XblInfo xbl;
    private Date expireDate;

    public Tokener(Authenticator.XblInfo xbl, Authenticator.AuthInfo auth){
        setXblInfo(xbl);
        setAuthInfo(auth);
    }

    public static Tokener empty(String username){
        return new Tokener(new Authenticator.XblInfo("", "", 1000), new Authenticator.AuthInfo(username, "T", 1000));
    }

    private Date now(){
        return Date.from(Instant.now());
    }

    public Tokener setAuthInfo(Authenticator.AuthInfo info){
        this.auth = info;

        expireDate = Date.from(now().toInstant().plusSeconds(info.expiresIn()));

        return this;
    }

    public Tokener setXblInfo(Authenticator.XblInfo xbl){
        this.xbl = xbl;
        return this;
    }

    public Tokener refreshToken() throws NoConnectionException, PerformException {
        return setXblInfo(Authenticator.getAuthenticator().refreshXbl(xbl.refresh()))
                .setAuthInfo(Authenticator.getAuthenticator().getATokenFromToken(xbl.xbl()));
    }

    public String getXUID(){
        return auth.username();
    }

    public String getAccessToken() throws PerformException {

        if (now().getTime() > expireDate.getTime()){
            try{
                refreshToken();
            }
            catch (NoConnectionException ignored){

            }
        }

        return auth.acessToken();
    }
}
