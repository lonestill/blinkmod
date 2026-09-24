package com.blinkmap.mod;

import android.os.Handler;
import android.os.Looper;

public class FakeLocationPump implements Runnable {
    private static Handler handler;
    private static final FakeLocationPump instance = new FakeLocationPump();
    private static volatile boolean isRunning = false;

    public static synchronized void start() {
        if (isRunning) return;
        isRunning = true;
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }
        handler.postDelayed(instance, 3000L);
    }

    public static synchronized void stop() {
        isRunning = false;
        if (handler != null) {
            handler.removeCallbacks(instance);
        }
    }

    @Override
    public void run() {
        if (!isRunning) return;
        try {
            if (StepHooks.isSpoofingActive()) {
                long now = System.currentTimeMillis();
                if (now - StepHooks.lastRealLocationTime >= 3000L) {
                    StepHooks.dispatchSyntheticLocation();
                }
            }
        } catch (Throwable ignored) {}
        if (handler != null) {
            handler.postDelayed(this, 3000L);
        }
    }
}
