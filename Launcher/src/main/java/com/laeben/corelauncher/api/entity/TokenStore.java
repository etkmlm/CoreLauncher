package com.laeben.corelauncher.api.entity;

import java.util.ArrayList;
import java.util.List;

public class TokenStore {
    private List<TokenInfo> infos;

    public List<TokenInfo> getTokenInfos(){
        if (infos == null)
            infos = new ArrayList<>();

        return infos;
    }

    public TokenInfo findTokens(String username){
        for (TokenInfo info : getTokenInfos()){
            if (username.equals(info.getUsername())){
                return info;
            }
        }

        return null;
    }

    public void updateTokens(TokenInfo info){
        var all = getTokenInfos();
        int index = -1;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getUsername().equals(info.getUsername())){
                index = i;
                all.remove(index);
                break;
            }
        }

        if (index == -1) all.add(info);
        else all.add(index, info);
    }
}
