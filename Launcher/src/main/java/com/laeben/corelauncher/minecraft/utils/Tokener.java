package com.laeben.corelauncher.minecraft.utils;

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

    public Tokener refreshToken(){
        return setXblInfo(Authenticator.getAuthenticator().refreshXbl(xbl.refresh()))
                .setAuthInfo(Authenticator.getAuthenticator().getATokenFromToken(xbl.xbl()));
    }

    public String getAccessToken(){

        if (now().getTime() > expireDate.getTime())
            refreshToken();

        return auth.acessToken();
    }
}
