package com.particlesdevs.photoncamera.control;

import android.os.CountDownTimer;
import android.widget.TextView;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class CountdownTimer extends CountDownTimer {
    private final TextView tv;
    private final TimerCallback callback;
    private final long interval;


    public CountdownTimer(TextView tv, long millisInFuture, long countDownInterval, TimerCallback callback) {
        super(millisInFuture, countDownInterval);
        this.callback = callback;
        this.tv = tv;
        this.interval = countDownInterval;
    }

    @Override
    public void onTick(long millisUntilFinished) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) + 1;
        tv.post(() -> {
            tv.setScaleX(3);
            tv.setScaleY(3);
            tv.setAlpha(1);
            tv.setText((String.format(Locale.ROOT, "%d", seconds)));
            tv.animate().scaleXBy(2).scaleYBy(2).alpha(0).setDuration(interval - 50).start();
        });
    }

    @Override
    public void onFinish() {
        tv.post(() -> {
            tv.setText("");
            tv.setAlpha(1);
            tv.setScaleY(1);
            tv.setScaleX(1);
        });
        callback.onFinished();
    }

    public interface TimerCallback {
        void onFinished();
    }
}