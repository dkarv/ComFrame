package com.dkarv.comframe.example;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.dkarv.comframe.library.ComFrameSender;

import java.io.UnsupportedEncodingException;


public class SendActivity extends Activity implements View.OnClickListener {
    private EditText editText;

    private ComFrameSender sender;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);

        editText = (EditText) findViewById(R.id.message);
        Button button = (Button) findViewById(R.id.send);
        button.setText("send!");
        button.setOnClickListener(this);

        sender = new ComFrameSender();
        // maybe set some setting in ComFrameSender
        sender.debug = true;
        // sender.setHammingCode(HammingCode.HAMMING_7_4);
        sender.prepare();
    }

    @Override
    public void onClick(View view) {
        Log.d("MainActivity", "onClick");
        switch (view.getId()) {
            case R.id.send:
                sendData(editText.getText().toString());
                break;
        }
    }

    public void sendData(String str) {
        // set the volume to max before starting to send
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
        Log.d("MainActivity", "startListening sending");
        byte[] data = new byte[0];
        try {
            data = str.getBytes("US-ASCII");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // the data are prepared now, send them out
        sender.send(data);
        // now reset the volume again:
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume,0);
    }
}
