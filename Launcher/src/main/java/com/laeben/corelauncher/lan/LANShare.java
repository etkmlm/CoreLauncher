package com.laeben.corelauncher.lan;

import com.laeben.core.util.events.KeyEvent;
import com.laeben.core.util.events.ValueEvent;
import com.laeben.corelauncher.api.Configurator;
import com.laeben.corelauncher.api.entity.Logger;
import com.laeben.corelauncher.api.socket.CLCommunicator;
import com.laeben.corelauncher.api.socket.packet.FilePacket;
import com.laeben.corelauncher.api.socket.packet.CLPacket;
import com.laeben.corelauncher.api.socket.entity.CLPacketType;
import com.laeben.corelauncher.api.socket.packet.KeyHandshakePacket;
import com.laeben.corelauncher.api.util.OSUtil;
import com.laeben.corelauncher.lan.entity.LANRecipient;
import com.laeben.corelauncher.lan.handler.LANProgressHandler;
import com.laeben.corelauncher.lan.handler.proxy.LANProgressHandlerProxy;
import com.laeben.corelauncher.util.EventHandler;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.function.Consumer;

public class LANShare {
    public static final String KEY = "lshare";

    public static final String FILE_RECEIVED = "rfile";
    public static final String NEW_RECIPIENT = "nrecp";
    public static final String REMOVE_RECIPIENT = "rrecp";

    private static LANShare instance;
    private static final int PORT = 50000;

    private final EventHandler<KeyEvent> handler;

    private Consumer<CLPacket> onFileReceived;

    private final Set<LANRecipient> storedRecipients;

    private CLCommunicator communicator;
    private boolean doReload = false;

    private int port = PORT;

    public LANShare(){
        handler = new EventHandler<>();
        storedRecipients = new HashSet<>();

        instance = this;
    }

    public int getPort(){
        return port;
    }

    public Set<LANRecipient> getStoredRecipients(){
        return Collections.unmodifiableSet(storedRecipients);
    }

    public void setOnFileReceived(Consumer<CLPacket> onFileReceived){
        this.onFileReceived = onFileReceived;
    }

    private void onKeyReceived(KeyHandshakePacket packet, Socket sock){
        final String name = packet.name();
        if (name == null) return;

        final var recp = new LANRecipient(sock.getInetAddress().getAddress(), sock.getLocalPort(), name);
        addRecipient(recp);
    }
    private boolean addRecipient(LANRecipient recp){
        boolean b = storedRecipients.add(recp);

        if (b) handler.execute(new ValueEvent(NEW_RECIPIENT, recp));

        return b;
    }
    public boolean removeRecipient(byte[] ip){
        for (LANRecipient recp : storedRecipients){
            if (!Arrays.equals(ip, recp.ip())) continue;

            storedRecipients.remove(recp);
            handler.execute(new ValueEvent(REMOVE_RECIPIENT, recp));
            return true;
        }

        return false;
    }

    /**
     * Sends a handshake request to the target recipient.
     * @param r recipient to sync
     * @return duplicate recipient, null if no duplicate
     */
    public LANRecipient syncRecipient(LANRecipient r, LANProgressHandler<CLPacket> handler) throws IOException {
        for (LANRecipient recp : storedRecipients){
            if (Arrays.equals(r.ip(), recp.ip())) return recp;
        }

        share(r, new CLPacket(CLPacketType.HANDSHAKE), handler);
        return null;
    }

    public void reload() {
        if (communicator != null){
            if (!doReload){
                doReload = true;
                communicator.kill();
                return;
            }
            port++;
        }

        if (doReload){
            doReload = false;
        }

        try {
            communicator = new CLCommunicator(getPort());

            communicator.getHandler().addHandler(KEY, a -> {
                if (a.getKey().equals(CLCommunicator.ENDED)){
                    doReload = false;
                    communicator.getHandler().removeHandler(KEY);
                    communicator = null;
                }
                else if (a.getKey().equals(CLCommunicator.RECEIVE) && a.getValue() instanceof CLPacket clp){
                    var socket = (Socket)a.getSource();
                    try {
                        if (clp.getType() == CLPacketType.FILE){
                            if (onFileReceived != null) onFileReceived.accept(clp);
                            syncRecipient(LANRecipient.fromSocket(socket), null);
                        }
                        else if (clp.getType() == CLPacketType.HANDSHAKE){
                            sendKey(LANRecipient.fromSocket(socket), null);
                        }
                        else if (clp.getType() == CLPacketType.KEY_HANDSHAKE){
                            onKeyReceived(KeyHandshakePacket.fromPacket(clp), socket);
                        }

                    } catch (IOException e) {
                        Logger.getLogger().log(e);
                    }
                }
            }, false);

            communicator.start();
        } catch (IOException e) {
            Logger.getLogger().log(e);
        }

    }

    public static LANShare getInstance(){
        return instance;
    }
    public EventHandler<KeyEvent> getHandler(){
        return handler;
    }

    public void share(LANRecipient recipient, CLPacket packet, LANProgressHandler<CLPacket> handler) throws IOException {
        try(var socket = new Socket()){
            socket.setSoTimeout(5000);
            socket.connect(new InetSocketAddress(InetAddress.getByAddress(recipient.ip()), recipient.port()));

            try(var stream = socket.getOutputStream()){
                packet.print(stream, handler == null ? null : new LANProgressHandlerProxy(recipient, handler));
                //packet.print(stream);
            }
        }
        catch (ConnectException e){
            removeRecipient(recipient.ip());

            throw e;
        }
    }
    public void share(String key, CLPacket packet, LANProgressHandler<CLPacket> handler) throws IOException {
        share(LANRecipient.fromKey(key), packet, handler);
    }

    public void sendKey(String targetKey, LANProgressHandler<CLPacket> handler) throws IOException {
        share(targetKey, new KeyHandshakePacket(OSUtil.getHostName()).serialize(), handler);
    }
    public void sendKey(LANRecipient recipient, LANProgressHandler<CLPacket> handler) throws IOException {
        share(recipient, new KeyHandshakePacket(OSUtil.getHostName()).serialize(), handler);
    }
    public String generateKeyString(){
        return LANRecipient.getKey(OSUtil.getLocalIP(), getPort());
    }
}
