package com.darloonlino.tcc_sig;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;

public class MainActivity extends AppCompatActivity {

    private Button botaoAcessar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // esconde o menu superior
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        botaoAcessar = findViewById(R.id.botaoAcessar);
        botaoAcessar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent in = new Intent(MainActivity.this, MapActivity.class);
                startActivity(in);
            }
        });
    }
}