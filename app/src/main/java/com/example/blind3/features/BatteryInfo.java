package com.example.blind3.features;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

public class BatteryInfo {
    public static String getBatteryStatus(AccessibilityService srv) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = srv.registerReceiver(null, ifilter);
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
        return text;
    }
}
