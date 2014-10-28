package com.dkarv.comframe.tictactoe;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;


public class MainActivity extends Activity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        (findViewById(R.id.chooseCircle)).setOnClickListener(this);
        (findViewById(R.id.choosePlus)).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.chooseCircle:
                Intent intentC = new Intent(this, GameActivity.class);
                intentC.putExtra("symbol", (byte) 1);
                startActivity(intentC);
                break;
            case R.id.choosePlus:
                Intent intentP = new Intent(this, GameActivity.class);
                intentP.putExtra("symbol", (byte) 2);
                startActivity(intentP);
                break;
        }
    }
}
