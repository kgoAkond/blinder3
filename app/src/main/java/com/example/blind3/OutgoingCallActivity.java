package com.example.blind3;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class OutgoingCallActivity extends AppCompatActivity {

    private static OutgoingCallActivity sInstance;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sInstance = this;
        setContentView(R.layout.activity_outgoing_call);

        TextView tvInfo = findViewById(R.id.tv_call_info);
        Button btnHangup = findViewById(R.id.btn_hangup_outgoing);

        // pobierz numer z InCallService (jeśli jest)
        String number = MyInCallService.getCurrentCallAddress();
        if (number == null || number.isEmpty()) {
            tvInfo.setText("Łączenie...");
        } else {
            tvInfo.setText("Łączenie z: " + number);
        }

        btnHangup.setOnClickListener(v -> {
            MyInCallService.disconnectCallIfPossible();
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        if (sInstance == this) {
            sInstance = null;
        }
        super.onDestroy();
    }

    public static void finishIfShowing() {
        if (sInstance != null) {
            sInstance.runOnUiThread(sInstance::finish);
        }
    }
}
