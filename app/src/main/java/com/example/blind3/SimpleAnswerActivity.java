package com.example.blind3;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class SimpleAnswerActivity extends AppCompatActivity {

    // prosta statyczna referencja, żeby InCallService mógł ją zamknąć
    private static SimpleAnswerActivity sInstance;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sInstance = this;
        setContentView(R.layout.activity_simple_answer);

        Button btnAnswer = findViewById(R.id.btn_answer);
        Button btnHangup = findViewById(R.id.btn_hangup);

        btnAnswer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyInCallService.answerRingingCallIfPossible();
            }
        });

        btnHangup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyInCallService.disconnectCallIfPossible();
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (sInstance == this) {
            sInstance = null;
        }
        super.onDestroy();
    }

    // wywoływane z MyInCallService, gdy rozmowa się zakończy
    public static void finishIfShowing() {
        if (sInstance != null) {
            sInstance.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    sInstance.finish();
                }
            });
        }
    }
}
