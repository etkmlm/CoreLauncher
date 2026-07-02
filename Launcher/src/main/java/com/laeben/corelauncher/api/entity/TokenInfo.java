package com.laeben.corelauncher.api.entity;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

public class TokenInfo {
    public static final String DEFAULT_ACCESS_TOKEN = "null";

    private final String username;
    private final String accessToken;
    private final String refreshToken;
    private final Date accessTokenExpiresAt;

    public TokenInfo(String username, String accessToken, String refreshToken, long accessExpiresIn){
        this.username = username;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessTokenExpiresAt = Date.from(Instant.now().plus(Duration.ofSeconds(accessExpiresIn)));
    }

    public TokenInfo(String username){
        this.username = username;

        this.accessToken = DEFAULT_ACCESS_TOKEN;
        this.refreshToken = null;
        this.accessTokenExpiresAt = null;
    }

    public String getAccessToken(){
        return accessToken == null ? DEFAULT_ACCESS_TOKEN : accessToken;
    }

    public String getRefreshToken(){
        return refreshToken;
    }

    public String getUsername(){
        return username;
    }

    public boolean isAccessTokenValid(){
        return accessTokenExpiresAt == null || accessTokenExpiresAt.after(Date.from(Instant.now()));
    }

    /*public TokenInfo(String username, String accessToken, String refreshToken, Date accessExpiresAt, Date refreshExpiresAt){
        this.username = username;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessTokenExpiresAt = accessExpiresAt;
        this.refreshTokenExpiresAt = refreshExpiresAt;
    }*/
}
