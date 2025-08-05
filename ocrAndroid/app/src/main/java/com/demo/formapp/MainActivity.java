package com.demo.formapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RadioGroup rg = findViewById(R.id.rg_api_choice);
        rg.setOnCheckedChangeListener((group, checkedId) -> {
            String choice = (checkedId == R.id.rb_paddle) ? "paddle" : "llm";
            OcrConfig.setOcrType(choice);
        });
        Button open = findViewById(R.id.btn_open_form);

        open.setOnClickListener(v -> {
            Log.d("Main Activity",OcrConfig.getOcrType());
            Intent intent = new Intent(this, FormActivity.class);
            startActivity(intent);
        });
    }
}