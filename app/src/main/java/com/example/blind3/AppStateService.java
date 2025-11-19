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
            case KeyEvent.KEYCODE_5 -> StartActionEnum.HOT_CALL;
            case KeyEvent.KEYCODE_0 -> StartActionEnum.MISSED_CALL;
            default -> StartActionEnum.EMPTY;
        };
    }
}
