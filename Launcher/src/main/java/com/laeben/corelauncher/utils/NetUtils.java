package com.laeben.corelauncher.utils;

import com.laeben.core.entity.Path;
import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.FileNotFoundException;
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

    public static Path download(String url, Path path, boolean uon) throws NoConnectionException, HttpException {
        try{
            return NetUtils.download(url, path, uon, true);
        }
        catch (StopException ignored){

        }
        catch (FileNotFoundException ex){
            return null;
        }

        return path;
    }
}
