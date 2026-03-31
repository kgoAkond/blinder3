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
            case KeyEvent.KEYCODE_MENU -> StartActionEnum.SAY_TIME;
            case KeyEvent.KEYCODE_BACK -> StartActionEnum.SAY_DATE;
            case KeyEvent.KEYCODE_DPAD_CENTER -> StartActionEnum.CHECK_ACTIVE;
            case KeyEvent.KEYCODE_STAR -> StartActionEnum.ASSISTANCE;
            case KeyEvent.KEYCODE_1 -> StartActionEnum.HOT_1;
            case KeyEvent.KEYCODE_2 -> StartActionEnum.HOT_2;
            case KeyEvent.KEYCODE_3 -> StartActionEnum.HOT_3;
            case KeyEvent.KEYCODE_4 -> StartActionEnum.HOT_4;
            case KeyEvent.KEYCODE_5 -> StartActionEnum.HOT_5;
            case KeyEvent.KEYCODE_6 -> StartActionEnum.HOT_6;
            case KeyEvent.KEYCODE_7 -> StartActionEnum.HOT_7;
            case KeyEvent.KEYCODE_8 -> StartActionEnum.HOT_8;
            case KeyEvent.KEYCODE_9 -> StartActionEnum.TOGGLE_SPEAKER;
            case KeyEvent.KEYCODE_0 -> StartActionEnum.MISSED_CALL;
            default -> StartActionEnum.EMPTY;
        };
    }
}
