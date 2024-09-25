package com.laeben.corelauncher.discord.channel;

import com.laeben.corelauncher.discord.entity.Data;
import com.laeben.corelauncher.discord.entity.NoDiscordException;

import java.io.File;
import java.io.IOException;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class UnixPipeChannel implements PipeChannel{

    private final SocketChannel channel;
    private final ByteBuffer buffInt;

    public UnixPipeChannel() throws IOException, NoDiscordException {
        var pipes = getDiscordPipes();
        if (pipes.isEmpty())
            throw new NoDiscordException();

        String instance = System.getenv("DISCORD_INSTANCE_ID");
        int i = instance != null ? Integer.parseInt(instance) : 0;
        var p = pipes.get(0) + i;
        if (!new File(p).exists())
            throw new NoDiscordException();

        channel = SocketChannel.open(UnixDomainSocketAddress.of(p));
        buffInt = ByteBuffer.allocate(Integer.BYTES * 2);
    }

    private List<String> getDiscordPipes(){
        var bases = new ArrayList<String>(){{
            add(System.getenv("TMP"));
            add(System.getenv("TEMP"));
            add(System.getenv("TMPDIR"));
            add(System.getenv("XDG_RUNTIME_DIR"));
        }};

        var deb = bases.stream().map(a -> a + "/app/com.discordapp.Discord").toList();
        var snap = bases.stream().map(a -> a + "/snap.discord").toList();
        var snap2 = bases.stream().filter(Objects::nonNull).flatMap(a ->
                Arrays.stream(Objects.requireNonNull(new File(a).listFiles()))
                        .filter(x -> x.getName().startsWith("snap.discord_"))
        ).map(File::getAbsolutePath).toList();

        bases.addAll(deb);
        bases.addAll(snap);
        bases.addAll(snap2);

        bases.removeIf(a -> a == null || !new File(a).exists());

        return bases.stream().map(a -> a + "/discord-ipc-").toList();
    }


    @Override
    public Data read() throws IOException {
        buffInt.clear();
        channel.read(buffInt);
        buffInt.position(0);
        int code = Integer.reverseBytes(buffInt.getInt());
        int length = Integer.reverseBytes(buffInt.getInt());
        var buff = ByteBuffer.allocate(length);
        channel.read(buff);
        return Data.create(Data.OpCode.values()[code], new String(buff.array(), StandardCharsets.UTF_8));
    }

    @Override
    public void write(Data data) throws IOException {
        channel.write(data.buff());
    }

    @Override
    public void dispose() throws IOException {
        channel.close();
    }
}
