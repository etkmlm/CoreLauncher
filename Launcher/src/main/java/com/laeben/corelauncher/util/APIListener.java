package com.laeben.corelauncher.util;

import com.laeben.core.concurrency.CancellableToken;
import com.laeben.core.entity.exception.StopException;
import com.laeben.core.network.Network;
import com.laeben.core.util.Cat;
import com.laeben.corelauncher.CoreLauncher;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.minecraft.entity.ServerInfo;
import com.laeben.corelauncher.ui.controller.Main;
import com.laeben.corelauncher.util.entity.LogType;

import java.io.IOException;
import java.net.BindException;
import java.util.HashMap;

public class APIListener {
    private final static int DEFAULT_PORT = 9845;
    private final static int MAX_PORT = 9860;
    private static int listeningPort = DEFAULT_PORT;

    public static String createClosePageRequest() throws IOException, StopException {
        var resource = CoreLauncher.class.getResourceAsStream("data/auth.html");
        return resource == null ? null : Network.inputStreamToString(resource).replace("$turnOff", Translator.translate("auth.ok"));
    }

    public static int getActivePort(){
        return listeningPort;
    }

    private static String listenAndGet() throws IOException, StopException {
        String n = null;
        final int firstPort = listeningPort;

        do{
            try {
                if (firstPort != listeningPort){
                    Logger.getLogger().logDebug(LogType.WARN, "Trying to open Web API on " + listeningPort);
                }
                n = Network.listenServer(listeningPort, createClosePageRequest());
                break;
            }
            catch (BindException e){
                listeningPort++;
            }
        } while (listeningPort <= MAX_PORT);

        if (listeningPort > MAX_PORT) {
            Logger.getLogger().log(LogType.ERROR, "No ports are available between %d and %d, disabling the web API.".formatted(DEFAULT_PORT, MAX_PORT));
            listeningPort = DEFAULT_PORT;
            throw new StopException();
        }

        return n;
    }

    public static void start(CancellableToken<?> token){
        new Thread(() -> {
            while (!token.shouldStop()){
                String n;
                try{
                    n = listenAndGet();
                }
                catch (IOException e) {
                    Logger.getLogger().log("Could not create listening server for port " + listeningPort, e);
                    Cat.sleep(1000);
                    continue;
                } catch (StopException ignored) {
                    break;
                }

                var lines = n.split("\n");
                var head = lines[0].split(" ");

                // only get requests are allowed
                if (head.length != 3 || !head[0].equals("GET"))
                    continue;

                var exact = lines[0]
                        .replace(head[0] + " ", "")
                        .replace(" " + head[head.length - 1], "")
                        .split("\\?");
                String path = exact[0];
                var a = new HashMap<String, String>();
                if (exact.length == 2){
                    for (var b : exact[1].split("&")){
                        var c = b.split("=");
                        a.put(c[0], c[1]);
                    }
                }

                // Server Join Command
                if (path.equals("/join") && a.containsKey("server")){
                    String server = a.get("server");
                    int port = a.containsKey("port") ? Integer.parseInt(a.get("port")) : 25565;
                    if (Main.getMain() == null || Main.getMain().getSelectedProfile() == null)
                        continue;
                    var profile = Main.getMain().getSelectedProfile();

                    Main.getMain().launch(profile, null, new ServerInfo(server, port));
                }
            }
        }).start();
    }
}
