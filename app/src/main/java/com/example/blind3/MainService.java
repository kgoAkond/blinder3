package com.example.blind3;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.CallLog;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import androidx.core.content.ContextCompat;

import com.example.blind3.model.StartActionEnum;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class MainService extends AccessibilityService implements TextToSpeech.OnInitListener {

    private static final String TAG = "KGO";
    private static final long DEBOUNCE_MS = 200L;
    private static final Locale LOCALE_PL = new Locale("pl", "PL");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", LOCALE_PL);
    private TextToSpeech tts;
    private boolean ready = false;
    private long lastHandledAt = 0L;
    private int lastConsumedKey = -1;
    private boolean ttsSpeaking = false;
    private int lastSpokenKey = -1;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusReq;
    private SoundPool soundPool;
    private int robotSoundId = 0;
    private int beepSoundId = 0;
    private boolean robotLoaded = false;
    private BroadcastReceiver screenReceiver;
    private AppStateService appStateService;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) info = new AccessibilityServiceInfo();
        info.flags = info.flags | AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        setServiceInfo(info);
        audioManager = getSystemService(AudioManager.class);
        AudioAttributes spAttrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(spAttrs)
                .build();
        soundPool.setOnLoadCompleteListener((sp, sampleId, status) -> {
            if (status == 0 && sampleId == robotSoundId) robotLoaded = true;
        });
        robotSoundId = soundPool.load(this, R.raw.robot, 1);
        beepSoundId = soundPool.load(this, R.raw.beep, 1);

        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                    Log.d("KGO", "ACTION_SCREEN_ON -> playRobot()");
                    playSound(robotSoundId);
                }
            }
        };
        registerReceiver(screenReceiver, filter);
        appStateService = new AppStateService();
        tts = new TextToSpeech(this, this);
    }

    private void requestTransientAudioFocus() {
        if (audioManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();
            audioFocusReq = new AudioFocusRequest.Builder(
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(attrs)
                    .build();
            audioManager.requestAudioFocus(audioFocusReq);
        } else {
            audioManager.requestAudioFocus(
                    null, AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            );
        }
    }


    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.d("KGO", "event: " + event.toString());
    }


    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        int code = event.getKeyCode();
        String text = "ready";
        if (event.getAction() == KeyEvent.ACTION_UP) {
            if (code == lastConsumedKey) {
                lastConsumedKey = -1;
                return true;
            }
            return false;
        }

        if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
        if (event.getRepeatCount() > 0) return false;

        if (CallStateReceiver.isRinging() && code == KeyEvent.KEYCODE_2) {
            String caller = CallStateReceiver.getCallerInfo();
            if (caller != null && !caller.isEmpty()) {
                Log.d("TimeSpeakerService", "Key 2 pressed. Announcing caller: " + caller);
                speak("Dzwoni " + caller);
            } else {
                speak("Nieznany numer");
            }
            return true;
        }


        if (code == KeyEvent.KEYCODE_DPAD_CENTER) {
            MyInCallService.answerRingingCallIfPossible();
            return true;
        }
        var step = appStateService.process(code);

        if (step == StartActionEnum.EMPTY) return false;

        boolean isToggleMute = ttsSpeaking && (code == lastSpokenKey);

        long now = System.currentTimeMillis();

        if (!isToggleMute && (now - lastHandledAt < DEBOUNCE_MS)) return true;

        lastHandledAt = now;
        lastConsumedKey = code;

        Log.d("KGO", "Key code: " + code +
                ", ttsSpeaking=" + ttsSpeaking +
                ", lastSpokenKey=" + lastSpokenKey +
                ", isToggleMute=" + isToggleMute);

        if (isToggleMute) {
            muteTts();
            return true;
        }

        if (step == StartActionEnum.SAY_TIME) {
            text = speakTimeWithKey(code);
            showStartActivity(text);

        } else if (step == StartActionEnum.SAY_DATE) {
            text = speakDateWithKey(code);
            showStartActivity(text);
        } else if (step == StartActionEnum.CHECK_ACTIVE) {
            showStartActivity("Wszystko OK");
            playSound(robotSoundId);
        } else if (step == StartActionEnum.ASSISTANCE) {
            launchAssistant();
        } else if (step == StartActionEnum.HOT_CALL) {
            RequestCallPermissionActivity.start(this, "+48888868868");
        } else if (step == StartActionEnum.MISSED_CALL) {
            speakLastMissedCall();
        }


        return true;
    }

    private void showStartActivity(String text) {
        StartActivity.setText(text);
        Intent intent = new Intent(this, StartActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    private void muteTts() {
        if (tts != null) {
            tts.stop();
        }
        ttsSpeaking = false;
        lastSpokenKey = -1;

        if (audioManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusReq != null) {
                audioManager.abandonAudioFocusRequest(audioFocusReq);
                audioFocusReq = null;
            } else {
                audioManager.abandonAudioFocus(null);
            }
        }
    }

    private String speakTimeWithKey(int keyCode) {
        lastSpokenKey = keyCode;
        return speakTime();
    }

    private String speakDateWithKey(int keyCode) {
        lastSpokenKey = keyCode;
        return speakDate();
    }

    private void playSound(int soundId) {
        float volume = 0.2f;
        if (soundPool == null || soundId == 0) return;

        requestTransientAudioFocus();

        if (robotLoaded) {
            soundPool.play(soundId, volume, volume, 1, 0, 1f);
        } else {
            new android.os.Handler(getMainLooper()).postDelayed(() -> {
                if (robotLoaded) soundPool.play(soundId, volume, volume, 1, 0, 1f);
            }, 120);
        }
    }

    private String speakTime() {
        if (!ready || tts == null) return "...";
        String text = LocalTime.now().format(FMT);
        requestTransientAudioFocus();
        tts.speak("Godzina " + text, TextToSpeech.QUEUE_FLUSH, null, "time-utterance");
        return text;
    }

    private String speakDate() {
        if (!ready || tts == null) return "...";
        String text = "Dzisiaj jest " + java.time.LocalDate.now().format(DATE_FMT);
        requestTransientAudioFocus();
        text += "\n" + getBatteryStatus();
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "date-utterance");
        return text;
    }

    private String getBatteryStatus() {
        if (!ready || tts == null) return "";

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        if (batteryStatus == null) {
            return "Nie udało się odczytać stanu baterii.";
        }

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

        int percent = -1;
        if (level >= 0 && scale > 0) {
            percent = (int) (100f * level / scale);
        }

        String chargingText;
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
                chargingText = "Bateria jest ładowana.";
                break;
            case BatteryManager.BATTERY_STATUS_FULL:
                chargingText = "Bateria jest naładowana.";
                break;
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                chargingText = "Bateria nie jest ładowana.";
                break;
            default:
                chargingText = "";
        }

        String text;
        if (percent >= 0) {
            text = "Poziom baterii " + percent + " procent. " + chargingText;
        } else {
            text = "Nie udało się obliczyć poziomu baterii. " + chargingText;
        }

        requestTransientAudioFocus();
        return text;
    }

    private boolean hasReadCallLogPermission() {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void speakLastMissedCall() {
        if (!ready || tts == null) return;

        if (!hasReadCallLogPermission()) {
            tts.speak("Brak zgody na odczyt historii połączeń.",
                    TextToSpeech.QUEUE_FLUSH, null, "no-calllog-perm");
            return;
        }

        Uri uri = CallLog.Calls.CONTENT_URI;

        String[] projection = new String[] {
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.DATE,
                CallLog.Calls.TYPE,
                CallLog.Calls.NEW
        };

        String selection = CallLog.Calls.TYPE + "=? AND " + CallLog.Calls.NEW + "=?";
        String[] selectionArgs = new String[] {
                String.valueOf(CallLog.Calls.MISSED_TYPE),
                "1"
        };

        String sortOrder = CallLog.Calls.DATE + " DESC LIMIT 1";

        try (Cursor cursor = getContentResolver().query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                String number = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER));
                String name   = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME));

                String who;
                if (name != null && !name.isEmpty()) {
                    who = name;
                } else if (number != null && !number.isEmpty()) {
                    who = "numer " + number;
                } else {
                    who = "nieznany numer";
                }

                String text = "Ostatnie nieodebrane połączenie od " + who + ".";
                requestTransientAudioFocus();
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "missed-call");
            } else {
                tts.speak("Brak nowych nieodebranych połączeń.",
                        TextToSpeech.QUEUE_FLUSH, null, "no-missed-calls");
            }
        } catch (Exception e) {
            Log.e("KGO", "speakLastMissedCall error", e);
            tts.speak("Nie udało się odczytać nieodebranych połączeń.",
                    TextToSpeech.QUEUE_FLUSH, null, "missed-call-error");
        }
    }

    private void speak(String text) {
        if (ready && tts != null) {
            requestTransientAudioFocus();
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }


    private void launchAssistant() {
        playSound(beepSoundId);
        try {
            Intent intent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                intent = new Intent(Intent.ACTION_VOICE_COMMAND);
            } else {
                intent = new Intent(Intent.ACTION_ASSIST);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Brak aktywności dla asystenta", e);
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_APP_CONTACTS);
            home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(home);
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
            Log.e(TAG, "launchAssistant error", e);
        }
    }

    @Override
    public void onInit(int status) {
        ready = (status == TextToSpeech.SUCCESS);
        if (ready && tts != null) {
            tts.setLanguage(LOCALE_PL);
            tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    ttsSpeaking = true;
                }

                @Override
                public void onDone(String utteranceId) {
                    ttsSpeaking = false;
                }

                @Override
                public void onError(String utteranceId) {
                    ttsSpeaking = false;
                }
            });
        }
    }

    @Override
    public void onInterrupt() {
        if (tts != null) tts.stop();
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        super.onDestroy();
    }
}
