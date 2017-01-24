package com.mahhaus.scanloto;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final int RESULT_TEXT = 99;
    private Button buttonScan;
    private TextView textViewResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initComponents();
        actionComponents();
    }

    private void actionComponents() {
        buttonScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, ScanActivity.class);
                startActivityForResult(i, RESULT_TEXT);
            }
        });
    }

    private void initComponents() {
        textViewResult = (TextView) findViewById(R.id.textViewResult);
        buttonScan = (Button) findViewById(R.id.buttonScan);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_TEXT) { // Please, use a final int instead of hardcoded int value
            if (resultCode == RESULT_OK) {
                textViewResult.setText(data.getExtras().getString("resultText"));
            } else {
                textViewResult.setText("Falha na leitura! \n Tente novamente. ");
            }
        }
    }
}
