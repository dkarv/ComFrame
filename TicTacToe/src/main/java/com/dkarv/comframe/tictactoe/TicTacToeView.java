package com.dkarv.comframe.tictactoe;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;


public class TicTacToeView extends View {
    public final static int REDRAW = 0;
    public final static int YOU_HAVE_WON = 1;
    public final static int YOU_HAVE_LOST = 2;
    public final static int YOUR_TURN = 3;

    private TTT game;
    private Paint black = new Paint();
    private Paint blue = new Paint();
    private Paint red = new Paint();
    private Paint white = new Paint();

    private int w, h;
    private int thickL;
    private int thickS;
    private int radiusS;

    private Context context;

    public TicTacToeView(final Context context, byte symbol) {
        super(context);
        this.context = context;
        black.setColor(Color.BLACK);
        blue.setColor(Color.BLUE);
        red.setColor(Color.RED);
        white.setColor(Color.WHITE);

        Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.arg1) {
                    case REDRAW:
                        invalidate();
                        break;
                    case YOU_HAVE_LOST:
                        gameOver(false);
                        break;
                    case YOU_HAVE_WON:
                        gameOver(true);
                        break;
                    case YOUR_TURN:
                        Toast.makeText(context, "your turn!", Toast.LENGTH_SHORT).show();
                }
            }
        };

        game = new TTT(context, symbol, handler);
        game.start();
    }

    private void gameOver(boolean won) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.game_over)
                .setMessage(won ? R.string.win : R.string.lost)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(context, MainActivity.class);
                        context.startActivity(intent);
                    }
                })
                .setIcon(won ? R.drawable.trophy : R.drawable.sad)
                .show();
    }

    private void drawO(int x, int y, Canvas canvas) {
        canvas.drawCircle(x, y, radiusS, red);
        canvas.drawCircle(x, y, radiusS - thickS * 2, white);
    }

    private void drawX(int x, int y, Canvas canvas) {
        canvas.drawRect(x - radiusS, y - thickS, x + radiusS, y + thickS, blue);
        canvas.drawRect(x - thickS, y - radiusS, x + thickS, y + radiusS, blue);
    }

    //calculate new sizes of grid, symbol positions and radius
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (w == oldw && h == oldh)
            return;

        this.w = w;
        this.h = h;
        // set the thickness of the lines
        this.thickL = w / 200;
        // set also the thickness of the symbols
        this.thickS = Math.min(h, w) / 50;
        // set the radius of the symbols
        this.radiusS = (int) (Math.min(h, w) / 6 * 0.8);
    }

    @Override
    public void onDraw(Canvas canvas) {
        drawLines(canvas);

        for (int i = 0; i < game.state.length; i++) {
            // calculate the middle points
            int y = (i / 3) * (h / 3) + h / 6;
            int x = (i % 3) * (w / 3) + w / 6;
            switch (game.state[i]) {
                case 0:
                    // there's no item at this field
                    break;
                case 1:
                    drawO(x, y, canvas);
                    break;
                case 2:
                    drawX(x, y, canvas);
                    break;
                default:
                    break;
            }
        }
    }

    private void drawLines(Canvas canvas) {
        // vertical
        canvas.drawRect(w / 3 - thickL, 0, w / 3 + thickL, h, black);
        canvas.drawRect(w * 2 / 3 - thickL, 0, w * 2 / 3 + thickL, h, black);
        // hori
        canvas.drawRect(0, h / 3 - thickL, w, h / 3 + thickL, black);
        canvas.drawRect(0, h * 2 / 3 - thickL, w, h * 2 / 3 + thickL, black);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // if not my turn, done
        if (!game.getMyTurn() || event.getAction() != MotionEvent.ACTION_UP) {
            return true;
        }

        // get cords of pressed point
        float xc = event.getX();
        float yc = event.getY();

        // calculate the position where the user clicked
        int x = (int) ((xc / (w / 3)));
        int y = (int) ((yc / (h / 3)));
        int pos = y * 3 + x;

        if (game.setSymbol(game.symbol, pos)) {
            this.invalidate();
            return true;
        }

        return true;
    }
}

