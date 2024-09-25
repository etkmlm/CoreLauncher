package com.laeben.corelauncher.discord;

import com.laeben.corelauncher.discord.channel.PipeChannel;
import com.laeben.corelauncher.discord.entity.Data;
import com.laeben.corelauncher.discord.entity.NoDiscordException;

import java.io.IOException;

public class DiscordIPC {
    private PipeChannel channel;

    public void reload() throws IOException, NoDiscordException {
        if (channel != null)
            channel.dispose();


        try{
            channel = PipeChannel.create();
        }
        catch (NoDiscordException e){
            channel = null;
            throw e;
        }
    }

    public void send(Data data) throws IOException, NoDiscordException {
        if (channel == null)
            reload();

        channel.write(data);
    }

    public Data read() throws IOException, NoDiscordException {
        if (channel == null)
            reload();

        return channel.read();
    }
}
