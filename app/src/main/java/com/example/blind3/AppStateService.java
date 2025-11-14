package com.example.blind3;

import android.view.KeyEvent;

import com.example.blind3.model.AppStateEnum;
import com.example.blind3.model.StartActionEnum;

public class AppStateService {

    private AppStateEnum appState = AppStateEnum.START;

    public StartActionEnum process(int keyCode) {
        if (appState == AppStateEnum.START) {
            return processStart(keyCode);
        }
        return StartActionEnum.EMPTY;
    }
    private void setAppState(AppStateEnum appState) {
        this.appState = appState;
    }

    private StartActionEnum processStart(int keyCode) {
        return switch (keyCode) {
            case KeyEvent.KEYCODE_STAR -> StartActionEnum.SAY_TIME;
            case KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_NUMPAD_6 -> StartActionEnum.SAY_DATE;
            case KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_NUMPAD_7 -> StartActionEnum.CHECK_ACTIVE;
            case KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_NUMPAD_0 -> StartActionEnum.ASSISTANCE;
            case KeyEvent.KEYCODE_5-> StartActionEnum.HOT_CALL;
            default -> StartActionEnum.EMPTY;
        };
    }
}
