package com.example.blind3.features;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.blind3.Contacts;
import com.example.blind3.MainService;
import com.example.blind3.SoundManager;
import com.example.blind3.StartActivity;
import com.example.blind3.model.Contact;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LastCalls {

    public static void speakLastMissedCall(MainService mainService, SoundManager sound) {
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
                    long dateMs = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE));
                    int type = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE));

                    // Określenie typu połączenia po polsku
                    String typeDescription = getTypeDescription(type);

                    // Formatowanie czasu i dnia
                    java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", MainService.LOCALE_PL);
                    String timeText = timeFormat.format(new java.util.Date(dateMs));
                    String dayText = getDayText(dateMs);

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

    public static List<Contact> getLastCalls(MainService mainService) {
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

        List<Contact> lastContacts = new ArrayList<>();

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
                    long dateMs = cursor.getLong(cursor.getColumnIndexOrThrow(CallLog.Calls.DATE));
                    int type = cursor.getInt(cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE));

                    // Określenie typu połączenia po polsku
                    String typeDescription = getTypeDescription(type);

                    // Formatowanie czasu i dnia
                    java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", MainService.LOCALE_PL);
                    String timeText = timeFormat.format(new java.util.Date(dateMs));
                    String dayText = getDayText(dateMs);

                    String who = getWho(number);
                    Log.d("KGO", "who: " + who + " number: " + number);
                    // Budowanie fragmentu tekstu dla jednego połączenia
                    String desc = new StringBuilder().append(count).append(". ")
                            .append(typeDescription)
                            .append(who).append(", godzina ")
                            .append(timeText).append(" ").append(dayText).append(". ").toString();
                    lastContacts.add(new Contact(who, number, 0, count, desc));

                    count++;
                } while (cursor.moveToNext());
            }

        } catch (Exception e) {
            Log.e("KGO", "speakLastCalls error", e);
        }
        return lastContacts;
    }

    @NonNull
    private static String getTypeDescription(int type) {
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
        return typeDescription;
    }

    private static String getDayText(long dateMs) {
        String dayText;
        if (android.text.format.DateUtils.isToday(dateMs)) {
            dayText = "dzisiaj";
        } else {
            // Obliczamy różnicę dni
            long now = System.currentTimeMillis();
            long diffMs = now - dateMs;
            long days = diffMs / (24 * 60 * 60 * 1000); // przeliczenie ms na dni

            if (days <= 1) {
                // Sprawdzenie czy to nie było wczoraj (jeśli isToday zwróciło false, a days < 2)
                dayText = "wczoraj";
            } else if (days < 5) {
                dayText = days + " dni temu";
            } else {
                // Dla starszych niż 4 dni możemy podać datę lub po prostu "X dni temu"
                dayText = days + " dni temu";
            }
        }
        return dayText;
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
