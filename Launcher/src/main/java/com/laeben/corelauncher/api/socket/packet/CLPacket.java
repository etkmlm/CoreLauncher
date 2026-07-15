package com.laeben.corelauncher.api.socket.packet;

import com.laeben.core.util.Cat;
import com.laeben.corelauncher.api.socket.entity.CLPacketType;
import com.laeben.corelauncher.lan.handler.ProgressHandler;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CancellationException;

public class CLPacket {
    private final CLPacketType type;

    private ByteBuffer inputBuffer;
    private InputStream stream;
    private int size;
    private int read;

    private ByteArrayOutputStream outputBuffer;

    public CLPacket(CLPacketType type) {
        outputBuffer = new ByteArrayOutputStream();
        this.type = type;

        writeInt(type.number());
    }

    private CLPacket(ByteBuffer inputBuffer, InputStream stream, int streamSize) {
        assert inputBuffer != null;
        this.type = CLPacketType.fromNumber(inputBuffer.getInt());
        this.stream = stream;
        this.size = streamSize + inputBuffer.capacity();
        this.inputBuffer = inputBuffer;
    }

    private CLPacket(InputStream stream, int streamSize) throws IOException {
        assert stream != null;
        this.type = CLPacketType.fromNumber(ByteBuffer.allocate(4).put(stream.readNBytes(4)).getInt(0));
        this.stream = stream;
        this.size = streamSize;
        this.read = 4;
        this.inputBuffer = null;
    }

    private CLPacket(CLPacketType type, ByteBuffer inputBuffer, InputStream stream, int streamSize) {
        this.type = type;
        this.stream = stream;
        this.size = streamSize + 4 + (inputBuffer != null ? inputBuffer.capacity() : 0);
        this.inputBuffer = inputBuffer;
    }

    public static CLPacket fromInputStream(InputStream stream, int length) throws IOException {
        return new CLPacket(stream, length);
    }
    public static CLPacket fromBuffer(ByteBuffer buffer){
        return new CLPacket(buffer, null, 0);
    }
    public static CLPacket fromArrayBuffer(byte[] buffer){
        return fromBuffer(ByteBuffer.wrap(buffer));
    }

    public CLPacketType getType(){
        return type;
    }

    public int getSize(){
        return size;
    }

    public int getReadBytes(){
        return read;
    }

    public void killStream() throws IOException {
        if (stream != null) stream.close();
    }
    public String readString() throws IOException {
        return readString(StandardCharsets.UTF_8);
    }
    public String readString(Charset charset) throws IOException {
        if (inputBuffer != null && inputBuffer.position() + 4 < inputBuffer.capacity()){
            int size = inputBuffer.getInt();
            if (inputBuffer.position() + size < inputBuffer.capacity()){
                inputBuffer.position(inputBuffer.position() - 4);
            }
            else{
                byte[] bytes = new byte[size];
                inputBuffer.get(bytes);
                this.read += 4 + size;
                return new String(bytes, charset);
            }
        }

        if (stream != null && stream.available() >= 4){
            int size = ByteBuffer.allocate(4).put(stream.readNBytes(4)).getInt(0);

            byte[] bytes = new byte[size];
            int read = stream.read(bytes);
            this.read += read + 4;
            return new String(bytes, 0, read, charset);
        }

        return null;
    }
    public int readInt() throws IOException {
        if (inputBuffer != null && inputBuffer.position() + 4 <= inputBuffer.capacity()){
            this.read += 4;
            return inputBuffer.getInt();
        }
        else if (stream != null && stream.available() >= 4){
            this.read += 4;
            return ByteBuffer.allocate(4).put(stream.readNBytes(4)).getInt(0);
        }
        else
            return -1;
    }
    public long readLong() throws IOException {
        if (inputBuffer != null && inputBuffer.position() + 8 <= inputBuffer.capacity()){
            this.read += 8;
            return inputBuffer.getLong();
        }
        else if (stream != null && stream.available() >= 8){
            this.read += 8;
            return ByteBuffer.allocate(8).put(stream.readNBytes(8)).getLong(0);
        }
        else
            return -1;
    }
    public int read(byte[] buffer) throws IOException {
        if (this.stream != null){
            int read = this.stream.read(buffer);
            this.read += read;
            return read;
        }

        if (this.inputBuffer == null) return -1;

        final int remaining = this.inputBuffer.capacity() - this.inputBuffer.position();

        int read = buffer.length;
        if (remaining < buffer.length)
            read = remaining;

        this.inputBuffer.get(buffer, 0, read);

        this.read += read;

        return read;
    }

    public CLPacket writeString(String value){
        return writeString(value, StandardCharsets.UTF_8);
    }
    public CLPacket writeString(String value, Charset charset) {
        assert outputBuffer != null;

        final byte[] bytes = value.getBytes(charset);

        writeInt(bytes.length);
        write(bytes, 0, bytes.length);

        return this;
    }
    public CLPacket writeInt(int value) {
        return write(ByteBuffer.allocate(4).putInt(value).array());
    }
    public CLPacket writeLong(long value) {
        return write(ByteBuffer.allocate(8).putLong(value).array());
    }
    public CLPacket write(byte[] buffer){
        return write(buffer, 0, buffer.length);
    }
    public CLPacket write(byte[] buffer, int offset, int length){
        assert outputBuffer != null;

        outputBuffer.write(buffer, offset, length);
        size += length;

        return this;
    }
    public CLPacket withStream(InputStream stream, int length){
        this.stream = stream;
        this.size += length;

        return this;
    }

    public void print(OutputStream output, ProgressHandler<CLPacket> handler) throws IOException {
        long written = 0;
        output.write(ByteBuffer.allocate(4).putInt(this.size).array());
        if (handler != null) handler.onProgress(this, written += 4, this.size);

        if (this.outputBuffer != null){
            outputBuffer.writeTo(output);
            if (handler != null) handler.onProgress(this, written += outputBuffer.size(), this.size);
        }

        if (this.stream != null){
            byte[] buff = new byte[4096];
            while (stream.available() > 0){
                int size = stream.read(buff);
                output.write(buff, 0, size);
                if (handler != null && !handler.onProgress(this, written += size, this.size)){
                    stream.close();
                    throw new CancellationException();
                }
                Cat.sleep(20);
            }
            stream.reset();
        }
    }
}
