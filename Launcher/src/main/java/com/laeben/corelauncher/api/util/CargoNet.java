package com.laeben.corelauncher.api.util;

import com.laeben.core.entity.Path;
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

    public CargoNet(int size) {
        executor = Executors.newFixedThreadPool(size);
        parcels = new ArrayList<>();
    }

    public void add(NetParcel parcel) {
        parcels.add(parcel);
        executor.submit(() -> {
            Path result = null;
            Exception ex = null;
            try{
                result = NetUtil.download(parcel);
            }
            catch (Exception e){
                ex = e;
            }
            if (ex == null){
                parcel.markAsDone();
                //System.out.println("X");
                if (parcel.getOnFinish() != null)
                    parcel.getOnFinish().run();
            }
            else
                parcel.markAsException(ex);

            onParcelDone(parcel, result, ++done, parcels.size());
        });
    }

    public void terminate(){
        executor.shutdownNow();
    }

    public boolean await() throws InterruptedException {
        executor.shutdown();
        boolean o = executor.awaitTermination(1, TimeUnit.DAYS);

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

    public abstract void onParcelDone(NetParcel p, Path path, int done, int total);
}
