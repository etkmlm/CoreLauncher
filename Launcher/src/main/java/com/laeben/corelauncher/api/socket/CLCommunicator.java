package com.laeben.corelauncher.api.socket;

import com.laeben.core.concurrency.CancellableToken;
import com.laeben.core.event.context.ValueContext;
import com.laeben.core.event.type.ValueEvent;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.socket.entity.CLPacket;
import com.laeben.corelauncher.event.bus.FrequentEventBus;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class CLCommunicator {
    public static final ValueContext RECEIVE = new ValueContext("Receive", null);

    private static CLCommunicator instance;

    private final ServerSocket server;
    private final int port;
    private final List<Socket> sockets;

    private final FrequentEventBus<ValueContext, ValueEvent> eventBus;

    private CancellableToken<?> cancellableToken;
    private boolean isRunning;
    private boolean killed;

    public CLCommunicator(int port) throws IOException {
        server = new ServerSocket();
        server.setSoTimeout(100);
        this.port = port;
        sockets = new ArrayList<>();

        eventBus = new FrequentEventBus<>();

        instance = this;
    }

    public static CLCommunicator getCommunicator(){
        return instance;
    }

    public FrequentEventBus<ValueContext, ValueEvent> getHandler(){
        return eventBus;
    }

    public void setCancellableToken(CancellableToken<?> cancellableToken){
        assert cancellableToken != null;
        this.cancellableToken = cancellableToken;
    }

    public void start(){
        if (cancellableToken == null) cancellableToken = new CancellableToken<>();

        new Thread(() -> {
            final var intBuffer = ByteBuffer.allocate(4);

            isRunning = true;

            while (!cancellableToken.shouldStop()){
                Socket newSocket = null;
                try {
                    if (!server.isBound())
                        server.bind(new InetSocketAddress(port));
                    newSocket = server.accept();
                    newSocket.setSoTimeout(50);
                }
                catch (SocketTimeoutException ignored){

                }
                catch (IOException e) {
                    Logger.getLogger().log(e);
                }

                if (newSocket != null)
                    sockets.add(newSocket);


                for (var sock : sockets){
                    try{
                        var s = sock.getInputStream().readNBytes(4);
                        int size = intBuffer.put(0, s).getInt();
                        intBuffer.clear();
                        var pack = CLPacket.fromArrayBuffer(sock.getInputStream().readNBytes(size));
                        eventBus.execute(new ValueEvent(RECEIVE, pack));
                    }
                    catch (SocketTimeoutException ignored){

                    }
                    catch (IOException ignored){
                        if (!sock.isClosed()) {
                            try {
                                sock.close();
                            } catch (IOException ignored1) {

                            }
                        }

                        sockets.remove(sock);
                    }
                }


            }

            for (var s : sockets){
                try {
                    s.close();
                } catch (IOException ignored) {

                }
            }

            isRunning = false;

            if (killed){
                try {
                    server.close();
                } catch (IOException e) {
                    Logger.getLogger().log(e);
                }
            }
        }).start();
    }

    public void stop(){
        assert this.cancellableToken != null;
        this.cancellableToken.stop();
    }

    public void kill(){
        stop();
        killed = true;
    }

}
