package com.example.blind3;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;

public class TimeSpeakerService extends AccessibilityService implements TextToSpeech.OnInitListener {

    private TextToSpeech tts;
    private boolean ready = false;
    private long lastHandledAt = 0L;
    private static final long DEBOUNCE_MS = 200L;
    private int lastConsumedKey = -1;
    private boolean ttsSpeaking = false;  // czy TTS mówi
    private int lastSpokenKey = -1;       // klawisz, który uruchomił TTS (np. STAR lub 6)
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusReq;
    private SoundPool soundPool;
    private int robotSoundId = 0;
    private boolean robotLoaded = false;

    private static final Locale LOCALE_PL = new Locale("pl", "PL");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", LOCALE_PL);
    private BroadcastReceiver screenReceiver;

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

        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                    Log.d("KGO", "ACTION_SCREEN_ON -> playRobot()");
                    playRobot();
                }
            }
        };
        registerReceiver(screenReceiver, filter);

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
        // Nie używamy zdarzeń UI — tylko klawiszy.
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

        boolean isStar   = code == KeyEvent.KEYCODE_STAR;
        boolean isSix    = code == KeyEvent.KEYCODE_6 || code == KeyEvent.KEYCODE_NUMPAD_6;
        boolean isSeven  = code == KeyEvent.KEYCODE_MENU;
        boolean isNine   = code == KeyEvent.KEYCODE_9 || code == KeyEvent.KEYCODE_NUMPAD_9;
        boolean isZero   = code == KeyEvent.KEYCODE_0 || code == KeyEvent.KEYCODE_NUMPAD_0;

        if (!isStar && !isSix && !isSeven && !isNine && !isZero) return false;

        boolean isToggleMute = ttsSpeaking && (code == lastSpokenKey) && (isStar || isSix);

        long now = System.currentTimeMillis();

        if (!isToggleMute && (now - lastHandledAt < DEBOUNCE_MS)) return true;

        lastHandledAt = now;
        lastConsumedKey = code;

        Log.d("KGO", "Key code: " + code +
                ", ttsSpeaking=" + ttsSpeaking +
                ", lastSpokenKey=" + lastSpokenKey +
                ", isToggleMute=" + isToggleMute);

        if (isNine) {
            muteTts();
            return true;
        }

        if (isToggleMute) {
            muteTts();
            return true;
        }

        if (isStar) {
            speakTimeWithKey(code);
        } else if (isSix) {
            speakDateWithKey(code);
        } else if (isSeven) {
            playRobot();
        } else if (isZero) {
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

    private void playRobot() {
        if (soundPool == null || robotSoundId == 0) return;

        requestTransientAudioFocus();

        if (robotLoaded) {
            // volume L/R = 1f, priority=1, loop=0 (bez pętli), rate=1f
            soundPool.play(robotSoundId, 1f, 1f, 1, 0, 1f);
        } else {
            // Jeśli użytkownik naciśnie bardzo szybko po starcie usługi, jeszcze się ładuje.
            // Proste podejście: ustaw małe opóźnienie i spróbuj ponownie raz.
            new android.os.Handler(getMainLooper()).postDelayed(() -> {
                if (robotLoaded) soundPool.play(robotSoundId, 1f, 1f, 1, 0, 1f);
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

    private void placeCall(String number) {
        if (number == null || number.isEmpty()) return;

        // jeśli TTS mówi – ucisz, żeby nie gadał nad połączeniem
        muteTts();

        android.net.Uri uri = android.net.Uri.parse("tel:" + number);

        // Sprawdź zgodę na CALL_PHONE
        boolean canCall = androidx.core.content.ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.CALL_PHONE
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED;

        android.content.Intent intent;
        if (canCall) {
            // Połączenie od razu
            intent = new android.content.Intent(android.content.Intent.ACTION_CALL, uri);
        } else {
            // Fallback: otwórz dialer z numerem (bez wymaganego uprawnienia)
            intent = new android.content.Intent(android.content.Intent.ACTION_DIAL, uri);
            // (opcjonalnie) możesz powiedzieć krótkie TTS info:
            if (tts != null && ready) {
                tts.speak("Brak zgody na połączenia. Otwieram telefon z numerem.",
                        TextToSpeech.QUEUE_FLUSH, null, "no-call-permission");
            }
        }

        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Log.e("KGO", "placeCall failed: " + e.getMessage(), e);
            // fallback awaryjny: powiedz o błędzie
            if (tts != null && ready) {
                tts.speak("Nie udało się rozpocząć połączenia.",
                        TextToSpeech.QUEUE_FLUSH, null, "call-failed");
            }
        }
    }

    @Override
    public void onInit(int status) {
        ready = (status == TextToSpeech.SUCCESS);
        if (ready && tts != null) {
            tts.setLanguage(LOCALE_PL);
            tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
                @Override public void onStart(String utteranceId) { ttsSpeaking = true; }
                @Override public void onDone(String utteranceId)  { ttsSpeaking = false; }
                @Override public void onError(String utteranceId) { ttsSpeaking = false; }
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
