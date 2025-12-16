package com.example.blind3;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;

public class CallStateReceiver extends BroadcastReceiver {
    private static final String TAG = "CallStateReceiver";
    private static boolean isRinging = false;
    private static String callerInfo = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            Log.d(TAG, "Phone state changed: " + state);

            isRinging = TelephonyManager.EXTRA_STATE_RINGING.equals(state);

            if (!isRinging) {
                clearCallerInfo();
            }
        }
    }

    public static boolean isRinging() {
        return isRinging;
    }

    public static String getCallerInfo() {
        return callerInfo;
    }

    public static void updateCallerInfo(Context context, String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            callerInfo = "Nieznany numer";
        } else {
            callerInfo = getContactName(context, phoneNumber);
        }
    }

    public static void clearCallerInfo() {
        callerInfo = null;
    }

    private static String getContactName(Context context, String phoneNumber) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        String[] projection = new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME};
        String contactName = phoneNumber;

        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
                if (nameIndex > -1) {
                    contactName = cursor.getString(nameIndex);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting contact name", e);
        }
        return contactName;
    }
}
