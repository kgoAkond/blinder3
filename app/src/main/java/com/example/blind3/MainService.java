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
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.CallLog;
import android.telecom.Call;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import androidx.core.content.ContextCompat;

import com.example.blind3.features.BatteryInfo;
import com.example.blind3.features.LastCalls;
import com.example.blind3.model.StartActionEnum;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class MainService extends AccessibilityService {

    private static final String TAG = "KGO";
    private static final long DEBOUNCE_MS = 200L;
    private static final Locale LOCALE_PL = new Locale("pl", "PL");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", LOCALE_PL);
    static MainService sInstance;
    private SoundManager sound;
    private long lastHandledAt = 0L;
    private int lastConsumedKey = -1;
    private int lastSpokenKey = -1;
    private BroadcastReceiver screenReceiver;
    private AppStateService appStateService;
    private Contacts contacts;
    private static Call ringingCall = null;

    public static SoundManager getSound() {
        if (sInstance != null) {
            return sInstance.sound;
        }
        return null;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        sInstance = this;
        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) info = new AccessibilityServiceInfo();
        info.flags = info.flags | AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        setServiceInfo(info);


        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                    Log.d("KGO", "ACTION_SCREEN_ON -> playRobot()");
                    sound.playSound();
                }
            }
        };
        registerReceiver(screenReceiver, filter);
        appStateService = new AppStateService();
        sound = new SoundManager(this);
        contacts = new Contacts();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

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
        Log.d("KGO", "code: " + code + " ringing " + MyInCallService.getCallState() + " name " + MyInCallService.getContactName());

        if (MyInCallService.getCallState() == Call.STATE_RINGING && code != KeyEvent.KEYCODE_DPAD_CENTER) {
            String caller = MyInCallService.getContactName();
            if (caller != null && !caller.isEmpty()) {
                Log.d("TimeSpeakerService", "Key 2 pressed. Announcing caller: " + caller);
                sound.speak("Dzwoni " + caller);
            } else {
                sound.speak("Nieznany numer");
            }
            return true;
        }

        if (MyInCallService.getCallState() == Call.STATE_ACTIVE && code != KeyEvent.KEYCODE_DPAD_CENTER) {
            MyInCallService.toggleSpeaker();
            return true;
        }

        var step = appStateService.process(code);
        if (code == KeyEvent.KEYCODE_DPAD_CENTER) {
            if (MyInCallService.answerRingingCallIfPossible() || MyInCallService.disconnectCallIfPossible()) {
                return true;
            } else {
                step = StartActionEnum.CHECK_ACTIVE;
            }
        }

        if (step == StartActionEnum.EMPTY) return false;

        boolean isToggleMute = sound.ttsSpeaking() && (code == lastSpokenKey);

        long now = System.currentTimeMillis();

        if (!isToggleMute && (now - lastHandledAt < DEBOUNCE_MS)) return true;

        lastHandledAt = now;
        lastConsumedKey = code;

        if (isToggleMute) {
            sound.muteTts();
            lastSpokenKey = -1;
            return true;
        }

        if (step == StartActionEnum.SAY_TIME) {
            lastSpokenKey = code;
            text = speakTime();
            showStartActivity(text);

        } else if (step == StartActionEnum.SAY_DATE) {
            lastSpokenKey = code;
            text = speakDate();
            showStartActivity(text);
        } else if (step == StartActionEnum.CHECK_ACTIVE) {
            handleActiveAction();

        } else if (step == StartActionEnum.ASSISTANCE) {
            sound.muteTts();
            launchAssistant();
            return true;
        } else if (isHot(step)) {
            handleHots(step);
        } else if (step == StartActionEnum.MISSED_CALL) {
            lastSpokenKey = code;
            speakLastMissedCall();
        } else if (step == StartActionEnum.EMPTY) {
            return false;
        }
        return true;
    }

    private boolean isHot(StartActionEnum act) {
        return switch (act) {
            case HOT_1, HOT_2, HOT_3, HOT_4, HOT_5, HOT_6, HOT_7, HOT_8, HOT_9 -> true;
            default -> false;
        };
    }

    private void handleHots(StartActionEnum act) {
        var c = switch (act) {
            case HOT_1 -> contacts.selectContact(1, sound);
            case HOT_2 -> contacts.selectContact(2, sound);
            case HOT_3 -> contacts.selectContact(3, sound);
            case HOT_4 -> contacts.selectContact(4, sound);
            case HOT_5 -> contacts.selectContact(5, sound);
            case HOT_6 -> contacts.selectContact(6, sound);
            case HOT_7 -> contacts.selectContact(7, sound);
            case HOT_8 -> contacts.selectContact(8, sound);
            case HOT_9 -> contacts.selectContact(9, sound);
            default -> null;
        };
        if (c != null) {
            showStartActivity("Zadzwoń do " + c.name());
        }
    }

    private void handleActiveAction() {
        var contact = contacts.getSelectedContact();
        if (contact == null) {
            showStartActivity("Wybierz kontakt");
            sound.speak("Wybierz kontakt");
        } else {
            showStartActivity(contact.name());
            sound.speak("Dzwonię do " + contact.name(), () -> RequestCallPermissionActivity.start(this, contact.number()));
        }
    }

    private void showStartActivity(String text) {
        StartActivity.setText(text);
        Intent intent = new Intent(this, StartActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    private String speakTime() {
        String text = LocalTime.now().format(FMT);
        sound.speakTime("Godzina " + text);
        return text;
    }

    private String speakDate() {
        String text = "Dzisiaj jest " + java.time.LocalDate.now().format(DATE_FMT);
        text += "\n" + BatteryInfo.getBatteryStatus(this);
        sound.speakDate(text);
        return text;
    }

    private boolean hasReadCallLogPermission() {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void speakLastMissedCall() {
        if (!hasReadCallLogPermission()) {
            sound.speak("Brak zgody na odczyt historii połączeń.");
            return;
        }
        LastCalls.speakLastMissedCall(this, sound, LOCALE_PL);
    }

    private void launchAssistant() {
        sound.playSound();
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
    public void onInterrupt() {
        sound.muteTts();
    }

    @Override
    public void onDestroy() {
        sound.shutdown();
        ;
        super.onDestroy();
    }
}
