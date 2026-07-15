package com.laeben.corelauncher.lan.handler.proxy;

import com.laeben.corelauncher.api.socket.packet.CLPacket;
import com.laeben.corelauncher.lan.entity.LANProgressTuple;
import com.laeben.corelauncher.lan.entity.LANRecipient;
import com.laeben.corelauncher.lan.handler.LANProgressHandler;
import com.laeben.corelauncher.lan.handler.ProgressHandler;

public class LANProgressHandlerProxy<T> extends ProgressHandler<CLPacket> {
    private final LANProgressHandler<T> payloadProgressHandler;
    private final LANRecipient recipient;
    public LANProgressHandlerProxy(LANRecipient recipient, LANProgressHandler<T> payloadProgressHandler) {
        this.payloadProgressHandler = payloadProgressHandler;
        this.recipient = recipient;
    }
    @Override
    public boolean onProgress(CLPacket payload, long current, long total) {
        if (this.payloadProgressHandler != null)
            return this.payloadProgressHandler.onProgress(new LANProgressTuple(this.recipient, payload), current, total);
        return true;
    }
}
