package com.laeben.corelauncher.api.util;

import com.laeben.core.entity.Path;
import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.corelauncher.api.entity.ImageEntity;
import com.laeben.corelauncher.api.util.entity.NetParcel;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.InvalidPathException;
import java.util.UUID;

public class NetUtil extends com.laeben.core.util.NetUtils{
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

    public static Path download(NetParcel parcel) throws NoConnectionException, HttpException, StopException {
        return download(parcel.getUrl(), parcel.getPath(), parcel.useOriginalName());
    }

    public static Path download(String url, Path path, boolean uon) throws NoConnectionException, HttpException, StopException {
        try{
            return NetUtil.download(url, path, uon, true);
        }
        catch (FileNotFoundException ex){
            return null;
        }
    }

    public static ImageEntity downloadImage(Path path, String url, boolean uon){
        try {

            Path i;
            try{
                i = NetUtil.download(url, path, uon, false);
            }
            catch (InvalidPathException e){
                i = NetUtil.download(url, path.to(UUID.randomUUID() + ".png"),false, false);
            }
            if (i != null){
                String identifier = i.getName();
                if (i.getExtension() == null)
                    identifier += ".png";
                return ImageEntity.fromLocal(identifier).setUrl(url);
            }
            else
                return null;
        } catch (NoConnectionException | FileNotFoundException | StopException | HttpException e) {
            return null;
        }
    }

    public static ImageEntity downloadImage(Path path, String url){
        return downloadImage(path, url, true);
    }
}
