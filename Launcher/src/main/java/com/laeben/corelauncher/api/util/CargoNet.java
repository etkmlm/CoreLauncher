package com.laeben.corelauncher.api.util;

import com.laeben.core.concurrency.CancellableToken;
import com.laeben.core.entity.Path;
import com.laeben.core.entity.exception.StopException;
import com.laeben.core.event.function.ProgressFunction;
import com.laeben.core.network.Network;
import com.laeben.corelauncher.api.util.entity.NetParcel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class CargoNet {

    private final List<NetParcel> parcels;
    private int done;

    private final ExecutorService executor;

    private final CancellableToken<?> cancellableToken;
    private final ProgressFunction onProgress;

    public CargoNet(int size) {
        this.executor = Executors.newFixedThreadPool(size);
        this.parcels = new ArrayList<>();
        this.cancellableToken = null;
        this.onProgress = null;
    }

    public CargoNet(int size, CancellableToken<?> cancellableToken){
        this.executor = Executors.newFixedThreadPool(size);
        this.parcels = new ArrayList<>();
        this.cancellableToken = cancellableToken;
        this.onProgress = null;
    }

    public CargoNet(int size, ProgressFunction onProgress, CancellableToken<?> cancellableToken){
        this.executor = Executors.newFixedThreadPool(size);
        this.parcels = new ArrayList<>();
        this.cancellableToken = cancellableToken;
        this.onProgress = onProgress;
    }

    public void add(NetParcel parcel) {
        parcels.add(parcel);
        final var future = executor.submit(() -> {
            Path result = null;
            Exception ex = null;
            try {
                result = Network.download(parcel.toToken().withLogging(onProgress).syncWith(cancellableToken));
            } catch (Exception e) {
                ex = e;
            }
            if (ex == null) {
                parcel.markAsDone();
                if (parcel.getOnFinish() != null)
                    parcel.getOnFinish().run();
            } else
                parcel.markAsException(ex);

            try {
                onParcelDone(parcel, result, ++done, parcels.size());
            } catch (StopException e) {
                terminate();
            }
        });
    }

    public void terminate(){
        executor.shutdownNow();
    }

    public boolean await() throws StopException {
        executor.shutdown();
        boolean o;
        try {
            o = executor.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StopException();
        }

        done = 0;

        for (NetParcel parcel : parcels) {
            if (!parcel.isSuccessful()){
                o = false;
                break;
            }
        }

        return o;
    }

    public List<NetParcel> getParcels(){
        return parcels;
    }

    public abstract void onParcelDone(NetParcel p, Path path, int done, int total) throws StopException;
}
