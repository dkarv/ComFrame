package com.dkarv.comframe.example;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.dkarv.comframe.library.ComFrame;
import com.dkarv.comframe.library.ComFrameReceiver;


public class ReceiveActivity extends Activity implements OnClickListener,
        ComFrame.BitListener, ComFrame.MessageListener {
    private ComFrameReceiver receiver;
    private TextView dataView;
    private TextView textView;

    private Button button;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive);

        button = ((Button) findViewById(R.id.start_stop));
        button.setOnClickListener(this);

        dataView = (TextView) findViewById(R.id.dataView);
        textView = (TextView) findViewById(R.id.textView);

        receiver = new ComFrameReceiver();
        receiver.setMsgListener(this);
        receiver.setBitListener(this);
        // receiver.setHammingCode(HammingCode.HAMMING_7_4);
        receiver.debug = true;
        receiver.prepare();
    }

    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_stop:
                if (receiver.isRunning()) {
                    receiver.stopListening();
                    button.setText("start listening");
                } else {
                    receiver.startListening();
                    button.setText("stop listening");
                }
                break;
        }
    }

    @Override
    public void onBitReceived(final boolean bit) {
        // important! if you want to show the received bits somehow on the UI,
        // watch out that they are handed over in a different thread!
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dataView.setText(dataView.getText() + (bit ? "1" : "0"));
                String str = dataView.getText().toString();
                String[] split = str.split("_");
                int len = split[split.length - 1].length();
                if (len == 8 || (len > 8 && (len - 8) % 9 == 0)) {
                    dataView.setText(dataView.getText() + " ");
                }
            }
        });
    }

    @Override
    public void onMessageReceived(byte[] msg) {
        String str = "";
        for (int i = 0; i < msg.length; i++) {
            str += (char) msg[i];
        }
        final String s = str;
        // important! if you want to show the received bits somehow on the UI,
        // watch out that they are handed over in a different thread!
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(textView.getText() + "" + s);
            }
        });
    }
}