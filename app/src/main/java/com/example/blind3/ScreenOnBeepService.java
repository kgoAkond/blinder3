package com.example.blind3;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class ScreenOnBeepService extends Service {

    private static final String CHANNEL_ID = "screen_on_beep_channel";
    private BroadcastReceiver receiver;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();

        Notification notif = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Time Speaker")
                .setContentText("Dźwięk przy włączeniu ekranu: aktywny")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setOngoing(true)
                .build();
        startForeground(1, notif);

        // Rejestrujemy dynamicznie, bo manifestowo ACTION_SCREEN_ON jest ograniczone
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_SCREEN_ON);
        // Opcjonalnie: sygnał po odblokowaniu
        f.addAction(Intent.ACTION_USER_PRESENT);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (Intent.ACTION_SCREEN_ON.equals(action)) {
                    playBeep(120);
                } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                    playBeep(80);
                }
            }
        };
        registerReceiver(receiver, f);
    }

    private void playBeep(int durationMs) {
        ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_ACCESSIBILITY, 100);
        tg.startTone(ToneGenerator.TONE_PROP_BEEP, durationMs);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    "Ekran włączony – sygnał",
                    NotificationManager.IMPORTANCE_MIN
            );
            ch.setDescription("Odtwarza krótki dźwięk, gdy ekran zostaje włączony/odblokowany.");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (receiver != null) {
            try { unregisterReceiver(receiver); } catch (Exception ignored) {}
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}