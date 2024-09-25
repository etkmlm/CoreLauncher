package com.laeben.corelauncher.api.socket;

import com.laeben.core.util.events.ValueEvent;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.socket.entity.CLPacket;
import com.laeben.corelauncher.util.EventHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class CLCommunicator {
    public static final String EVENT_RECEIVE = "receive";

    private static CLCommunicator instance;

    private final ServerSocket server;
    private final int port;
    private final List<Socket> sockets;

    private final EventHandler<ValueEvent> handler;

    private boolean isRunning;
    private boolean killed;

    public CLCommunicator(int port) throws IOException {
        server = new ServerSocket();
        server.setSoTimeout(100);
        this.port = port;
        sockets = new ArrayList<>();

        handler = new EventHandler<>();

        instance = this;
    }

    public static CLCommunicator getCommunicator(){
        return instance;
    }

    public EventHandler<ValueEvent> getHandler(){
        return handler;
    }

    public void start(){
        isRunning = true;
        new Thread(() -> {
            var intBuffer = ByteBuffer.allocate(4);
            while (isRunning){
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
                        handler.execute(new ValueEvent(EVENT_RECEIVE, pack));
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
        isRunning = false;
    }

    public void kill(){
        stop();
        killed = true;
    }

}
