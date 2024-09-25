package com.laeben.corelauncher.util;

import com.laeben.corelauncher.CoreLauncher;
import com.laeben.corelauncher.api.Translator;
import com.laeben.corelauncher.api.util.NetUtil;
import com.laeben.corelauncher.minecraft.entity.ServerInfo;
import com.laeben.corelauncher.ui.controller.Main;

import java.util.HashMap;

public class APIListener {

    public static String createClosePageRequest(){
        var resource = CoreLauncher.class.getResourceAsStream("data/auth.html");
        return resource == null ? null : NetUtil.inputStreamToString(resource).replace("$turnOff", Translator.translate("auth.ok"));
    }

    public static void start(){
        new Thread(() -> {
            while (true){
                var n = NetUtil.listenServer(9845, createClosePageRequest());
                if (n == null)
                    continue;

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

                    Main.getMain().launch(profile, false, new ServerInfo(server, port));
                }
            }
        }).start();
    }
}
