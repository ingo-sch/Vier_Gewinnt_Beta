package com.example.viergewinntbeta;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private VierGewinntView spiel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Buttons aktivieren
        Button btn1 = findViewById(R.id.button);
        btn1.setOnClickListener(this);
        Button btn2 = findViewById(R.id.button2);
        btn2.setOnClickListener(this);
        Button btn3 = findViewById(R.id.button3);
        btn3.setOnClickListener(this);
        RadioButton rbtn1 = findViewById(R.id.radioButton1);
        rbtn1.setOnClickListener(this);
        RadioButton rbtn2 = findViewById(R.id.radioButton2);
        rbtn2.setOnClickListener(this);
        RadioButton rbtn3 = findViewById(R.id.radioButton3);
        rbtn3.setOnClickListener(this);

        // neues Spiel initialisieren
        spiel = new VierGewinntView(this);
        LinearLayout layout = findViewById(R.id.linLayout0);
        layout.addView(spiel);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button : spiel.starteNeu(true); break;
            case R.id.button3: spiel.starteNeu(false); break;
            case R.id.button2 : finish(); break;
            case R.id.radioButton1 :
                spiel.setKiStaerke(1); break;
            case R.id.radioButton2 :
                spiel.setKiStaerke(2); break;
            case R.id.radioButton3 :
                spiel.setKiStaerke(3); break;
        }
    }

}