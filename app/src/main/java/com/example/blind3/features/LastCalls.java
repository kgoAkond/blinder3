package com.example.blind3.features;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.util.Log;

import com.example.blind3.Contacts;
import com.example.blind3.MainService;
import com.example.blind3.SoundManager;
import com.example.blind3.StartActivity;

import java.util.Locale;

public class LastCalls {

    public static void speakLastMissedCall(MainService mainService, SoundManager sound, Locale locale) {
        Uri uri = CallLog.Calls.CONTENT_URI;

        String[] projection = new String[]{
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.DATE,
                CallLog.Calls.TYPE
        };

        // Pobieramy wszystkie typy połączeń (bez filtrowania po TYPE i NEW)
        String selection = null;
        String[] selectionArgs = null;

        // Zmieniamy limit na 3 ostatnie połączenia
        String sortOrder = CallLog.Calls.DATE + " DESC LIMIT 3";

        StringBuilder fullText = new StringBuilder();

        try (Cursor cursor = mainService.getContentResolver().query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                int count = 1;
                do {
                    String number = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME));
                    long dateMs = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE));
                    int type = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE));

                    // Określenie typu połączenia po polsku
                    String typeDescription;
                    switch (type) {
                        case CallLog.Calls.INCOMING_TYPE:
                            typeDescription = "Połączenie przychodzące od ";
                            break;
                        case CallLog.Calls.OUTGOING_TYPE:
                            typeDescription = "Połączenie wychodzące do ";
                            break;
                        case CallLog.Calls.MISSED_TYPE:
                            typeDescription = "Połączenie nieodebrane ";
                            break;
                        case CallLog.Calls.REJECTED_TYPE:
                            typeDescription = "Połączenie odrzucone ";
                            break;
                        default:
                            typeDescription = "Połączenie ";
                    }

                    // Formatowanie czasu i dnia
                    java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", locale);
                    String timeText = timeFormat.format(new java.util.Date(dateMs));
                    String dayText = android.text.format.DateUtils.isToday(dateMs) ? "dzisiaj" : "wcześniej";

                    String who = getWho(number);
                    Log.d("KGO", "who: " + who + " number: " + number);
                    // Budowanie fragmentu tekstu dla jednego połączenia
                    fullText.append(count).append(". ")
                            .append(typeDescription)
                            .append(who).append(", godzina ")
                            .append(timeText).append(" ").append(dayText).append(". ");

                    count++;
                } while (cursor.moveToNext());
                showText(mainService, fullText.toString());
                sound.speak(fullText.toString());

            } else {
                sound.speak("Historia połączeń jest pusta.");
            }

        } catch (Exception e) {
            Log.e("KGO", "speakLastCalls error", e);
            sound.speak("Nie udało się odczytać historii połączeń.");
        }
    }

    private static String getWho(String number) {
        if (number == null) return "numer zastrzeżony";
        if (number.length() >= 9) {
            number = number.substring(number.length() - 9);
            var name = Contacts.findName(number);
            if (name != null) {
                return name;
            }
            return number.substring(0, 3) + " " + number.substring(3, 6) + " " + number.substring(6);
        }
        return number;
    }

    private static void showText(MainService mainService, String text) {
        StartActivity.setText(text);
        Intent intent = new Intent(mainService, StartActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        mainService.startActivity(intent);
    }

}
