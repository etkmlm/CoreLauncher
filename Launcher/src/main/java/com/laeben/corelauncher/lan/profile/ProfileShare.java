package com.laeben.corelauncher.lan.profile;

import com.laeben.core.entity.Path;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.api.socket.packet.CLPacket;
import com.laeben.corelauncher.api.socket.packet.FilePacket;
import com.laeben.corelauncher.lan.LANShare;
import com.laeben.corelauncher.lan.entity.LANProgressTuple;
import com.laeben.corelauncher.lan.entity.LANRecipient;
import com.laeben.corelauncher.lan.handler.LANProgressHandler;
import com.laeben.corelauncher.ui.dialog.DProfileSharePanel;
import javafx.stage.Window;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileShare {
    private static ExecutorService generateService(){
        return Executors.newFixedThreadPool(10);
    }

    public static void fromRecipients(Set<LANRecipient> recipients, Window owner, LANProgressHandler<Path> handler){
        final var result = new DProfileSharePanel(owner)
                .pick(true, false);

        if (result.isEmpty()) return;

        share(new SharePreference(result.get().targetProfiles(), result.get().sendCompleteSetup(), recipients), handler);
    }

    public static void fromProfiles(Set<Profile> profiles, Window owner, LANProgressHandler<Path> handler){
        final var result = new DProfileSharePanel(owner)
                .pick(false, true);

        if (result.isEmpty()) return;

        share(new SharePreference(Set.copyOf(profiles), result.get().sendCompleteSetup(), result.get().targetRecipients()), handler);
    }

    public static void share(SharePreference pref, LANProgressHandler<Path> handler){
        new Thread(() -> {
            final var service = generateService();
            for (var profile : pref.targetProfiles()){
                var path = Path.begin(java.nio.file.Path.of("D:\\Dosyalar\\Untitled Project.mp4")); //profile.getPath();

                var localHandler = new LANProgressHandler<CLPacket>(){
                    @Override
                    public boolean onProgress(LANProgressTuple<CLPacket> payload, long current, long total) {
                        return handler.onProgress(new LANProgressTuple<>(payload.recipient(), path), current, total);
                    }
                };

                var tasks = pref.targetRecipients().stream().map(recipient -> (Callable<Object>) () -> {
                    LANShare.getInstance().share(recipient, new FilePacket.ProfilePacket().serialize(path), localHandler);
                    return null;
                }).toList();

                try {
                    service.invokeAll(tasks);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }
}
