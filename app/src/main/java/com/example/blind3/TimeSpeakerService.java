package com.example.blind3;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import com.example.blind3.model.StartActionEnum;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class TimeSpeakerService extends AccessibilityService implements TextToSpeech.OnInitListener {

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

        if (event.getAction() == KeyEvent.ACTION_UP) {
            if (code == lastConsumedKey) {
                lastConsumedKey = -1;
                return true;
            }
            return false;
        }

        if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
        if (event.getRepeatCount() > 0) return false;

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
            speakTimeWithKey(code);
        } else if (step == StartActionEnum.SAY_DATE) {
            speakDateWithKey(code);
        } else if (step == StartActionEnum.CHECK_ACTIVE) {
            playSound(robotSoundId);
        } else if (step == StartActionEnum.ASSISTANCE) {
            launchAssistant();
        } else if (step == StartActionEnum.HOT_CALL) {
            RequestCallPermissionActivity.start(this, "+48888868868");
        }
        return true;
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

    private void speakTimeWithKey(int keyCode) {
        lastSpokenKey = keyCode;
        speakTime();
    }

    private void speakDateWithKey(int keyCode) {
        lastSpokenKey = keyCode;
        speakDate();
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

    private void speakTime() {
        if (!ready || tts == null) return;
        String text = LocalTime.now().format(FMT);
        requestTransientAudioFocus();
        tts.speak("Godzina " + text, TextToSpeech.QUEUE_FLUSH, null, "time-utterance");
    }

    private void speakDate() {
        if (!ready || tts == null) return;
        String dateText = java.time.LocalDate.now().format(DATE_FMT);
        requestTransientAudioFocus();
        tts.speak("Dzisiaj jest " + dateText, TextToSpeech.QUEUE_FLUSH, null, "date-utterance");
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
