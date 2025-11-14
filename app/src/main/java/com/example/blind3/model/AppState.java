package com.example.blind3.model;

public class AppState {

    private AppStateEnum appState = AppStateEnum.START;

    public AppStateEnum getAppState() {
        return appState;
    }

    public void setAppState(AppStateEnum appState) {
        this.appState = appState;
    }
}
