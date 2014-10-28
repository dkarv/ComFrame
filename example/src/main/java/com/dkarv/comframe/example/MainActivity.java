package com.dkarv.comframe.example;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.dkarv.comframe.library.ComFrameSender;

import java.util.Arrays;


public class MainActivity extends Activity implements View.OnClickListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button = (Button) findViewById(R.id.sender);
        button.setOnClickListener(this);

        button = (Button) findViewById(R.id.receiver);
        button.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.sender:
                Intent intentS = new Intent(this, SendActivity.class);
                startActivity(intentS);
                break;
            case R.id.receiver:
                Intent intentR = new Intent(this, ReceiveActivity.class);
                startActivity(intentR);
                break;
        }
    }
}
