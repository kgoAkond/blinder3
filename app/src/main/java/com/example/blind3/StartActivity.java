package com.example.blind3;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class StartActivity extends AppCompatActivity {

    private static StartActivity sInstance;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sInstance = this;
        setContentView(R.layout.activity_start);

    }

    @Override
    protected void onDestroy() {
        if (sInstance == this) {
            sInstance = null;
        }
        super.onDestroy();
    }

    private void updateText(String text) {
        TextView txt = findViewById(R.id.star_text);
        txt.setText(text);
    }

    public static void setText(String text) {
        if (sInstance != null) {
            sInstance.updateText(text);
        }
    }

}
