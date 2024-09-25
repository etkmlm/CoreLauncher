package com.laeben.corelauncher.discord.channel;

import com.laeben.corelauncher.discord.entity.Data;
import com.laeben.corelauncher.discord.entity.NoDiscordException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

public class WindowsPipeChannel implements PipeChannel{
    private static final String IPC_BASE= "\\\\.\\pipe\\discord-ipc-";
    private final RandomAccessFile raf;

    public WindowsPipeChannel() throws NoDiscordException {
        String instance = System.getenv("DISCORD_INSTANCE_ID");
        int i = instance != null ? Integer.parseInt(instance) : 0;
        try {
            raf = new RandomAccessFile(IPC_BASE + i, "rw");
        } catch (FileNotFoundException e) {
            throw new NoDiscordException();
        }
    }

    @Override
    public Data read() throws IOException {
        int code = Integer.reverseBytes(raf.readInt());
        int length = Integer.reverseBytes(raf.readInt());
        byte[] buff = new byte[length];
        raf.read(buff, 0, length);
        return Data.create(Data.OpCode.values()[code], new String(buff, StandardCharsets.UTF_8));
    }

    @Override
    public void write(Data data) throws IOException {
        raf.write(data.buff().array());
    }

    @Override
    public void dispose() throws IOException {
        raf.close();
    }
}
