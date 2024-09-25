package com.laeben.corelauncher.discord;

import com.google.gson.Gson;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.discord.entity.*;
import com.laeben.corelauncher.discord.entity.response.HandshakeResponse;
import com.laeben.corelauncher.util.entity.LogType;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Discord {

    private static Discord instance;
    private static final String CLIENT_ID = "1279516377023123537";

    private final DiscordIPC ipc;

    private final Gson gson;
    private final Handshake handshake;

    private boolean handshooked = false;
    private boolean isRunning = false;
    private Activity activity;
    private boolean idlingActivity = false;
    private boolean needToUpdateActivity = false;

    private final ScheduledExecutorService executor;
    private ScheduledFuture task;

    public Discord() {
        ipc = new DiscordIPC();
        gson = new Gson();

        handshake = Handshake.create(CLIENT_ID);
        executor = Executors.newSingleThreadScheduledExecutor();

        instance = this;
    }

    public void startDiscordThread(){
        isRunning = true;

        task = executor.scheduleWithFixedDelay(() -> {
            if (!isRunning){
                task.cancel(true);
                return;
            }

            try {
                if (!handshooked){
                    handshake.nonce = generateNonce();
                    var resp = handshake();
                    handshooked = resp.isSuccessful() || resp.code == 1003;
                }

                if (Configurator.getConfig().isDisabledRPC()){
                    if (!idlingActivity){
                        updateActivity(null);
                        needToUpdateActivity = true;
                        idlingActivity = true;
                    }
                    return;
                }

                idlingActivity = false;

                if (needToUpdateActivity){
                    updateActivity(activity);
                    needToUpdateActivity = false;
                }

                heartbeat();
            } catch (IOException e) {
                if (e.getMessage().toLowerCase().contains("closed")){
                    try {
                        ipc.reload();
                    } catch (IOException x) {
                        Logger.getLogger().log(x);
                    }
                    catch (NoDiscordException ignored){
                        return;
                    }
                    handshooked = false;
                    needToUpdateActivity = true;
                }
                else
                    Logger.getLogger().log(e);
            }
            catch (NoDiscordException ignored){

            }
            catch (Exception e){
                Logger.getLogger().log(e);
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
    }

    public void stopDiscordThread(){
        isRunning = false;
    }

    public static Discord getDiscord(){
        return instance;
    }

    public static String generateNonce(){
        return UUID.randomUUID().toString();
    }

    public void reload() throws IOException, NoDiscordException {
        ipc.reload();
    }

    private void heartbeat() throws NoDiscordException, IOException {
        ipc.send(Data.create(Data.OpCode.HEARTBEAT, "{}"));
        ipc.read().getData();
    }

    private HandshakeResponse handshake() throws IOException, NoDiscordException {
        var hs = gson.toJson(handshake);
        ipc.send(Data.create(Data.OpCode.DISPATCH, hs));
        return gson.fromJson(ipc.read().getData(), HandshakeResponse.class);
    }

    private Command updateActivity(Activity a) throws IOException, NoDiscordException{
        var cmd = Command.createForActivity(a);
        cmd.nonce = generateNonce();
        var ac = gson.toJson(cmd);
        ipc.send(Data.create(Data.OpCode.HEARTBEAT, ac));
        var d = ipc.read().getData();
        var resp = gson.fromJson(d, Command.class);
        if (!resp.isSuccessful())
            Logger.getLogger().log(LogType.ERROR, "Error while updating activity (" + resp.data.code + "): " + resp.data.message);
        return resp;
    }

    public void setActivity(Consumer<Activity> process){
        if (activity == null)
            activity = new Activity();
        process.accept(activity);
        needToUpdateActivity = true;
    }

    public void setActivity(Activity a) {
        activity = a;
        needToUpdateActivity = true;
    }
}