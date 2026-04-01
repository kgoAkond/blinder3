package com.example.blind3;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import java.util.Locale;

public class SoundManager {

    private final AudioManager audioManager;
    private SoundPool soundPool;
    private TextToSpeech tts;
    private boolean ready = false;
    private boolean ttsSpeaking = false;
    private boolean robotLoaded = false;
    private int soundId = 0;
    public int robotSoundId = 0;
    public int beepSoundId = 0;
    private AudioFocusRequest audioFocusReq;
    private Runnable postSpeakAction;
    private final Locale LOCALE_PL = new Locale("pl", "PL");

    public SoundManager(Context context) {
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        AudioAttributes spAttrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        // Inicjalizacja SoundPool
        this.soundPool = new SoundPool.Builder()
                .setMaxStreams(2)
                .setAudioAttributes(spAttrs)
                .build();

        soundPool.setOnLoadCompleteListener((sp, sampleId, status) -> {
            if (status == 0 && sampleId == robotSoundId) {
                robotLoaded = true;
            }
        });

        robotSoundId = soundPool.load(context, R.raw.robot, 1);
        beepSoundId = soundPool.load(context, R.raw.beep, 1);
        soundId = robotSoundId;

        // Inicjalizacja TextToSpeech
        this.tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                ready = true;
                tts.setLanguage(LOCALE_PL);

                // Przypisanie atrybutów audio do TTS (ważne dla Audio Focus)
                AudioAttributes ttsAttrs = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build();
                tts.setAudioAttributes(ttsAttrs);

                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        ttsSpeaking = true;
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        ttsSpeaking = false;
                        abandonFocus(); // Oddaj focus po zakończeniu mówienia
                        if (postSpeakAction != null) {
                            new Handler(Looper.getMainLooper()).post(postSpeakAction);
                            postSpeakAction = null;
                        }
                    }

                    @Override
                    public void onError(String utteranceId) {
                        ttsSpeaking = false;
                        abandonFocus();
                    }
                });
            }
        });
    }

    public void speak(String text, Runnable onFinish) {
        if (!ready || tts == null) return;
        this.postSpeakAction = onFinish;
        requestTransientAudioFocus();
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "general-utterance");
    }

    public void speak(String text) {
        if (!ready || tts == null) return;
        requestTransientAudioFocus();
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "general-utterance");
    }

    public void speakTime(String text) {
        if (!ready || tts == null) return;
        requestTransientAudioFocus();
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "time-utterance");
    }

    public void speakDate(String text) {
        if (!ready || tts == null) return;
        requestTransientAudioFocus();
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "date-utterance");
    }

    public void muteTts() {
        if (tts != null) {
            tts.stop();
        }
        ttsSpeaking = false;
        abandonFocus();
    }

    public void playSound() {
        if (soundPool == null || soundId == 0) return;
        float volume = 0.5f;

        requestTransientAudioFocus();

        if (robotLoaded || soundId == beepSoundId) {
            soundPool.play(soundId, volume, volume, 1, 0, 1f);
        } else {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                soundPool.play(soundId, volume, volume, 1, 0, 1f);
            }, 150);
        }
    }

    private void requestTransientAudioFocus() {
        if (audioManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();

            // PRZYPISANIE DO POLA KLASY (bez var)
            audioFocusReq = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(attrs)
                    .build();

            audioManager.requestAudioFocus(audioFocusReq);
        } else {
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
        }
    }

    private void abandonFocus() {
        if (audioManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusReq != null) {
                audioManager.abandonAudioFocusRequest(audioFocusReq);
                audioFocusReq = null;
            }
        } else {
            audioManager.abandonAudioFocus(null);
        }
    }

    public void shutdown() {
        if (tts != null) {
            tts.shutdown();
        }
        if (soundPool != null) {
            soundPool.release();
        }
    }

    public boolean ttsSpeaking() {
        return ttsSpeaking;
    }
}