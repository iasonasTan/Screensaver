package com.app.screensaver;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.Objects;

public class TimerService extends Service {
    private long mStartTime = -1, mPauseTime = -1, mPausedDuration = 0;
    private boolean mRunning = false;
    private Handler mHandler;
    private Runnable mTimeRunnable;

    private void sendTime(long time) {
        Intent intent = new Intent("UPDATE_TIME").setPackage(getPackageName());
        intent.putExtra("time", time);
        sendBroadcast(intent);
        Log.d("dev-test", "Sending time " + time);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;
        String action = intent.getAction();
        switch (Objects.requireNonNull(action)) {
            case "START":
                if (mPauseTime != -1) {
                    mPausedDuration += System.currentTimeMillis() - mPauseTime;
                    mPauseTime = -1;
                } else {
                    mStartTime = System.currentTimeMillis();
                }
                mRunning = true;
                mHandler.post(mTimeRunnable);
                break;
            case "PAUSE":
                if (mRunning) {
                    mPauseTime = System.currentTimeMillis();
                    mRunning = false;
                    mHandler.removeCallbacks(mTimeRunnable);
                }
                break;
            case "RESET":
                mRunning = false;
                mStartTime = -1;
                mPauseTime = -1;
                mPausedDuration = 0;
                mHandler.removeCallbacks(mTimeRunnable);
                sendTime(0);
                stopSelf();
                break;
        }
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler(Looper.getMainLooper());
        mTimeRunnable = () -> {
            if (mStartTime != -1) {
                long elapsed = System.currentTimeMillis() - mStartTime - mPausedDuration;
                sendTime(elapsed);
            }
            if (mRunning) {
                mHandler.postDelayed(mTimeRunnable, 500);
            }
        };
    }

    @Override
    public void onDestroy() {
        mHandler.removeCallbacks(mTimeRunnable);
        super.onDestroy();
    }

    public static String longToTime(long time) {
        if (time <= 0) return "00:00";
        long totalSeconds = time / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
