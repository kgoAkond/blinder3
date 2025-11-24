package com.example.blind3;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class SimpleAnswerActivity extends AppCompatActivity {

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
        updateText(MyInCallService.getContactName());

    }

    @Override
    protected void onDestroy() {
        if (sInstance == this) {
            sInstance = null;
        }
        super.onDestroy();
    }
    private void updateText(String text) {
        TextView txt = findViewById(R.id.call_text);
        txt.setText(text);
        MainService.speak("Dzwoni " + text);
    }


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
