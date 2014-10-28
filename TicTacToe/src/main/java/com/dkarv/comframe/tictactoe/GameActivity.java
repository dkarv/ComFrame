package com.dkarv.comframe.tictactoe;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.RelativeLayout;


public class GameActivity extends Activity {

    private TicTacToeView tttv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            byte symbol = extras.getByte("symbol");

            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

            tttv = new TicTacToeView(this, symbol);
            tttv.setLayoutParams(lp);

            RelativeLayout rl = new RelativeLayout(this);
            rl.addView(tttv);

            setContentView(rl);
        } else {
            throw new RuntimeException("Specify symbol in intent");
        }
    }
}
