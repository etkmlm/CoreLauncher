package com.laeben.corelauncher.utils;

import com.laeben.core.entity.exception.NoConnectionException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.UnknownHostException;

public class NetUtils extends com.laeben.core.util.NetUtils{
    public static Document getDocumentFromUrl(String url) throws IOException, NoConnectionException {
        if (offline)
            throw new NoConnectionException();
        try{
            return Jsoup.connect(url).get();
        }
        catch (UnknownHostException e){
            throw new NoConnectionException();
        }
    }
}
