package com.laeben.corelauncher.api.socket.packet;

import com.laeben.core.entity.Path;
import com.laeben.corelauncher.api.entity.Profile;
import com.laeben.corelauncher.api.socket.entity.CLPacketType;
import com.laeben.corelauncher.lan.handler.LANProgressHandler;
import com.laeben.corelauncher.lan.handler.ProgressHandler;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CancellationException;

public class FilePacket {
    public enum FileType{
        FLAT, PROFILE
    }

    protected Path tempPath;

    public FileType getType(){
        return FileType.FLAT;
    }

    public Path getTempPath() {
        return tempPath;
    }

    /**
     * @throws ArithmeticException if file size cannot be represented by int (4 bytes)
     */
    public CLPacket serialize(Path path) throws FileNotFoundException {
        final var file = path.toFile();
        final long fileLength = file.length();

        if (fileLength > Integer.MAX_VALUE) throw new ArithmeticException("File too large to send");

        return new CLPacket(CLPacketType.FILE)
                .writeInt(getType().ordinal())
                .writeString(path.getExtension())
                .withStream(new FileInputStream(file), (int)fileLength);
    }

    protected void writeFile(CLPacket packet, Path path, ProgressHandler<Path> handler) throws IOException {
        path.prepare();
        byte[] buff = new byte[4096];
        try (var stream = new FileOutputStream(path.toFile())){
            final int offset = packet.getReadBytes();
            int read = 0;
            int r;
            while ((r = packet.read(buff)) > 0){
                stream.write(buff, 0, r);
                if (handler != null && !handler.onProgress(path, offset + (read += r), packet.getSize())){
                    packet.killStream();
                    throw new CancellationException();
                }
            }
        }
    }

    public void deserialize(CLPacket packet, Path path, ProgressHandler<Path> handler) throws IOException {
        String extension = packet.readString();

        tempPath = path.isDirectory() ? path.to(System.currentTimeMillis() + "." + extension) : path;

        writeFile(packet, tempPath, handler);
    }

    public static FilePacket fromPacket(CLPacket packet, Path path, ProgressHandler<Path> handler) throws IOException {
        final FileType type = FileType.values()[packet.readInt()];

        var pack = switch (type){
            case FLAT -> new FilePacket();
            case PROFILE -> new ProfilePacket();
        };

        pack.deserialize(packet, path, handler);

        return pack;
    }

    public static class ProfilePacket extends FilePacket {

        @Override
        public FileType getType(){
            return FileType.PROFILE;
        }

        @Override
        public CLPacket serialize(Path path) throws FileNotFoundException {
            if (path.isDirectory())
                path = path.to("profile.json");

            final var file = path.toFile();
            final long fileLength = file.length();

            if (fileLength > Integer.MAX_VALUE) throw new ArithmeticException("File too large to send");

            return new CLPacket(CLPacketType.FILE)
                    .writeInt(getType().ordinal())
                    .withStream(new FileInputStream(file), (int)fileLength);
        }

        @Override
        public void deserialize(CLPacket packet, Path path, ProgressHandler<Path> handler) throws IOException {
            tempPath = path.isDirectory() ? path.to(path.hashCode() + ".json") : path;

            writeFile(packet, tempPath, handler);
        }

        public Profile getProfile(){
            if (tempPath == null) return null;

            return null;
        }
    }
}
