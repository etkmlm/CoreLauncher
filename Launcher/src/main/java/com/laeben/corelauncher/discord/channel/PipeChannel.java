package com.laeben.corelauncher.discord.channel;

import com.laeben.corelauncher.api.entity.OS;
import com.laeben.corelauncher.discord.entity.Data;
import com.laeben.corelauncher.discord.entity.NoDiscordException;

import java.io.IOException;

public interface PipeChannel {
    Data read() throws IOException;
    void write(Data data) throws IOException;
    void dispose() throws IOException;

    static PipeChannel create() throws IOException, NoDiscordException {
        return switch (OS.getSystemOS()){
            case WINDOWS -> new WindowsPipeChannel();
            case OSX, LINUX -> new UnixPipeChannel();
            default -> throw new NoDiscordException();
        };
    }
}
