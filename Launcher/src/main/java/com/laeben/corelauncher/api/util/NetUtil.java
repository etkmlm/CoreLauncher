package com.laeben.corelauncher.api.util;

import com.laeben.core.entity.Path;
import com.laeben.core.entity.exception.HttpException;
import com.laeben.core.entity.exception.NoConnectionException;
import com.laeben.core.entity.exception.StopException;
import com.laeben.core.network.Network;
import com.laeben.core.network.entity.NetworkToken;
import com.laeben.corelauncher.api.entity.ImageEntity;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.InvalidPathException;
import java.util.UUID;

public class NetUtil {
    public static Document getDocumentFromUrl(String url) throws IOException, NoConnectionException {
        if (Network.isOffline())
            throw new NoConnectionException();
        try{
            return Jsoup.connect(url).get();
        }
        catch (UnknownHostException e){
            throw new NoConnectionException();
        }
    }

    public static ImageEntity downloadImage(Path path, String url, boolean uon){
        try {

            Path i;
            try{
                i = Network.download(NetworkToken.create(url, path, uon));
            }
            catch (InvalidPathException e){
                i = Network.download(NetworkToken.create(url, path.to(UUID.randomUUID() + ".png"),false));
            }
            if (i != null){
                String identifier = i.getName();
                if (i.getExtension() == null)
                    identifier += ".png";
                return ImageEntity.fromLocal(identifier).setUrl(url);
            }
            else
                return null;
        } catch (NoConnectionException | StopException | HttpException | IOException e) {
            return null;
        }
    }

    public static ImageEntity downloadImage(Path path, String url){
        return downloadImage(path, url, true);
    }
}
