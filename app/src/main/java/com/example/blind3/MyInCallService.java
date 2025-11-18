package com.example.blind3;

import android.content.Intent;
import android.telecom.Call;
import android.telecom.InCallService;
import android.telecom.VideoProfile;
import android.util.Log;

public class MyInCallService extends InCallService {

    private static final String TAG = "MyInCallService";

    // statyczna referencja, żeby Activity mogła poprosić o answer()/disconnect()
    static MyInCallService sInstance;

    private Call currentCall;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        Log.d(TAG, "onCreate()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sInstance = null;
        Log.d(TAG, "onDestroy()");
    }

    @Override
    public void onCallAdded(Call call) {
        super.onCallAdded(call);
        Log.d(TAG, "onCallAdded: " + call);

        currentCall = call;
        currentCall.registerCallback(callCallback);

        // Otwórz nasze proste UI do rozmowy
        Intent i = new Intent(this, SimpleAnswerActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    @Override
    public void onCallRemoved(Call call) {
        super.onCallRemoved(call);
        Log.d(TAG, "onCallRemoved: " + call);

        if (call == currentCall) {
            currentCall.unregisterCallback(callCallback);
            currentCall = null;
        }
    }

    private final Call.Callback callCallback = new Call.Callback() {
        @Override
        public void onStateChanged(Call call, int state) {
            Log.d(TAG, "onStateChanged: " + call + " state=" + state);
            // Możesz tu np. zamknąć Activity, gdy rozmowa się rozłączy
            if (state == Call.STATE_DISCONNECTED) {
                SimpleAnswerActivity.finishIfShowing();
            }
        }
    };

    public void answerCurrentCall() {
        if (currentCall == null) {
            Log.d(TAG, "answerCurrentCall: no current call");
            return;
        }
        if (currentCall.getState() == Call.STATE_RINGING) {
            currentCall.answer(VideoProfile.STATE_AUDIO_ONLY);
            Log.d(TAG, "answerCurrentCall: answered");
        } else {
            Log.d(TAG, "answerCurrentCall: not ringing");
        }
    }

    public void disconnectCurrentCall() {
        if (currentCall == null) {
            Log.d(TAG, "disconnectCurrentCall: no current call");
            return;
        }
        currentCall.disconnect();
        Log.d(TAG, "disconnectCurrentCall: disconnect requested");
    }

    // statyczne helpery dla Activity
    public static void answerRingingCallIfPossible() {
        if (sInstance != null) {
            sInstance.answerCurrentCall();
        }
    }

    public static void disconnectCallIfPossible() {
        if (sInstance != null) {
            sInstance.disconnectCurrentCall();
        }
    }
}
