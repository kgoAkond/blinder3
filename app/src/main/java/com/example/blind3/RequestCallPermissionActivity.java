package com.example.blind3;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Uruchamiaj tę aktywność, gdy chcesz poprosić użytkownika o CALL_PHONE,
 * a po udzieleniu zgody od razu wykonać połączenie.
 *
 * Start:
 *   RequestCallPermissionActivity.start(thisOrServiceContext, "+48123456789");
 */
public class RequestCallPermissionActivity extends android.app.Activity {

    public static final String EXTRA_PHONE_NUMBER = "extra_phone_number";
    private static final int RC_CALL_PHONE = 1001;

    private String phoneNumber;

    /**
     * Wygodny starter – z usługi pamiętaj o FLAG_ACTIVITY_NEW_TASK.
     */
    public static void start(android.content.Context ctx, String number) {
        Intent i = new Intent(ctx, RequestCallPermissionActivity.class);
        i.putExtra(EXTRA_PHONE_NUMBER, number);
        if (!(ctx instanceof android.app.Activity)) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        ctx.startActivity(i);
    }

    @Override
    protected void onStart() {
        super.onStart();

        phoneNumber = getIntent().getStringExtra(EXTRA_PHONE_NUMBER);
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            finish();
            return;
        }

        // Brak runtime permissions < API 23
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            makeCallOrFinish();
            return;
        }

        // Jeśli już jest zgoda – dzwoń
        if (hasCallPermission()) {
            makeCallOrFinish();
        } else {
            // Poproś o CALL_PHONE
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CALL_PHONE},
                    RC_CALL_PHONE
            );
        }
    }

    private boolean hasCallPermission() {
        return ContextCompat.checkSelfPermission(
                this, Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void makeCallOrFinish() {
        // jeśli z jakiegoś powodu nie chcesz dzwonić od razu – podmień na ACTION_DIAL
        Uri uri = Uri.parse("tel:" + phoneNumber);
        try {
            startActivity(new Intent(Intent.ACTION_CALL, uri));
        } catch (SecurityException se) {
            // Nie powinno się zdarzyć jeśli mamy zgodę, ale na wszelki wypadek
            startDialFallback("Brak uprawnienia do wykonywania połączeń.");
        } catch (Exception e) {
            startDialFallback("Nie udało się rozpocząć połączenia.");
        } finally {
            // Zazwyczaj chcesz zakończyć tę aktywność od razu
            finish();
        }
    }

    private void startDialFallback(String toastMsg) {
        if (toastMsg != null) {
            Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show();
        }
        Uri uri = Uri.parse("tel:" + phoneNumber);
        try {
            startActivity(new Intent(Intent.ACTION_DIAL, uri));
        } catch (Exception ignored) { /* brak aktywności dialera */ }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != RC_CALL_PHONE) {
            finish();
            return;
        }

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            makeCallOrFinish();
        } else {
            // Odmowa. Sprawdź, czy „Nie pytaj ponownie”:
            boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission.CALL_PHONE
            );
            if (!showRationale) {
                // Użytkownik zaznaczył „Nie pytaj ponownie” – zaproponuj wejście w ustawienia
                Toast.makeText(this,
                        "Nadaj uprawnienie Połączenia telefoniczne w Ustawieniach aplikacji.",
                        Toast.LENGTH_LONG).show();
                try {
                    Intent intent = new Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", getPackageName(), null)
                    );
                    startActivity(intent);
                } catch (Exception ignored) {}
            } else {
                // Zwykła odmowa – fallback do dialera
                startDialFallback("Brak zgody na połączenia. Otwieram telefon z numerem.");
            }
            finish();
        }
    }
}
