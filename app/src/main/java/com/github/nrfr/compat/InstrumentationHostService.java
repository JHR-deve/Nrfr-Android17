package com.github.nrfr.compat;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

/** Holds the default process while instrumentation runs; the UI lives in :ui. */
public final class InstrumentationHostService extends Service {
    private final IBinder binder = new Binder();

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
