package com.dkarv.comframe.tictactoe;

import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.dkarv.comframe.library.ComFrame;
import com.dkarv.comframe.library.ComFrameReceiver;
import com.dkarv.comframe.library.ComFrameSender;

enum MsgType {
    //request to play
    REQ,
    //reply to request
    ANSREQ,
    //acknowledgement of MSGPOS
    ACK,
    //wrongly received data
    NACK,
    //message with the position of a newly added symbol
    MSGPOS,
    // expecting nothing, if receiving something this may indicate that there's something wrong...
    NOTHING
}

public class TTT implements ComFrame.MessageListener {
    // 01010000 01101100 01100001 01111001 00111111
    private final static byte[] req = "Play?".getBytes();
    // 01011001 01100101 01110011 00100001
    private final static byte[] ansreq = "Yes!".getBytes();
    // 01001110 01101111 00100001
    private final static byte[] nack = "No!".getBytes();
    // 01001111 01101011
    private final static byte[] ack = "Ok".getBytes();
    // 00011000 position
    private final static byte[] msgpos = {0x18, 0};
    private final Context context;

    /**
     * which symbol this user represents
     */
    public byte symbol;
    public byte[] state = new byte[9];
    private ComFrameSender sender = new ComFrameSender();
    private ComFrameReceiver receiver = new ComFrameReceiver();
    private MsgType expectMsg = MsgType.NOTHING;
    private MsgType lastSendMsg = MsgType.NOTHING;
    private boolean myTurn = false;
    private Handler guiHandler;

    public TTT(Context context, byte symb, Handler gui) {
        this.context = context;
        this.guiHandler = gui;
        symbol = symb;

        //sender.debug = true;
        //receiver.debug = true;
        receiver.setMsgListener(this);
        receiver.startListening();
    }

    public void findOpponent() {
        // user has chosen the cross
        // send request
        sendToOp(MsgType.REQ, (byte) 0);
    }

    public void waitOpponent() {
        // user clicked on the circle
        // wait for another device picking the cross
        expectMsg = MsgType.REQ;
    }

    private void sendToOp(MsgType msg, int pos) {
        byte[] data = null;
        switch (msg) {
            case REQ:
                data = req;
                expectMsg = MsgType.ANSREQ;
                Log.d("TTT", "-> REQ [ANSREQ]");
                break;
            case ANSREQ:
                data = ansreq;
                expectMsg = MsgType.MSGPOS;
                Log.d("TTT", "-> ANSREQ [MSGPOS]");
                break;
            case ACK:
                data = ack;
                expectMsg = MsgType.MSGPOS;
                Log.d("TTT", "-> ACK [MSGPOS]");
                break;
            case NACK:
                data = nack;
                Log.d("TTT", "-> NACK");
                break;
            case MSGPOS:
                expectMsg = MsgType.ACK;
                if (pos >= 0 && pos < 9)
                    msgpos[1] = (byte) pos;
                data = msgpos;
                Log.d("TTT", "-> MSGPOS, ex: ACK");
                break;
            default:
                break;
        }
        if (data != null) {
            new SendToOp(data).start();
        }
    }

    public boolean getMyTurn() {
        return myTurn;
    }

    private void changeTurn() {
        myTurn = !myTurn;
        if (myTurn) {
            sendToView(TicTacToeView.YOUR_TURN);
        }
    }

    private void recvFromOp(byte[] rec) {
        switch (expectMsg) {
            case REQ:
                if (checkMsg(getMsg(expectMsg), rec)) {
                    Log.d("TTT", "<- REQ, -> ANSREQ");
                    sendToOp(MsgType.ANSREQ, 0);
                } else {
                    Log.d("TTT", "ex: REQ, -> NACK");
                    sendToOp(MsgType.NACK, 0);
                }
                break;
            case ANSREQ:
                if (checkMsg(getMsg(expectMsg), rec)) {
                    changeTurn();
                    Log.d("TTT", "<- ANSREQ");
                    expectMsg = MsgType.NOTHING;
                } else {
                    sendToOp(MsgType.NACK, 0);
                    Log.d("TTT", "ex: ANSREQ, -> NACK");
                }
                break;
            case ACK:
                if (checkMsg(getMsg(expectMsg), rec)) {
                    expectMsg = MsgType.MSGPOS;
                    Log.d("TTT", "<- ACK, ex: MSGPOS");
                } else if (checkMsg(getMsg(MsgType.NACK), rec)) {
                    sendToOp(lastSendMsg, 0);
                    Log.d("TTT", "<- NACK");
                } else {
                    sendToOp(MsgType.NACK, 0);
                    Log.d("TTT", "ex: ACK, -> NACK");
                }
                break;
            case MSGPOS:
                if (checkMsg(getMsg(expectMsg), rec)) {
                    // set the symbol of not the current user
                    if (setSymbol((byte)(symbol == 1 ? 2 : 1), rec[1])) {
                        Log.d("TTT", "<- MSGPOS, -> ACK");
                        sendToOp(MsgType.ACK, 0);
                        break;
                    }
                }
                Log.d("TTT", "<- ERROR, ex: MSGPOS, -> NACK");
                sendToOp(MsgType.NACK, 0);
                break;
            case NOTHING:
                Log.d("TTT", "<- NOTHING");
                break;
            default:
                Log.d("TTT", "error, expecting no message!!");
                break;
        }
    }

    private boolean checkMsg(byte[] exp, byte[] msg) {
        if (exp.length != msg.length) {
            return false;
        }
        byte[] msg2 = new byte[exp.length];
        int count = 0;
        if (expectMsg == MsgType.MSGPOS) {
            msg2[0] = (byte) (exp[0] ^ msg[0]);
            while (msg2[0] != 0) {
                count += msg2[0] & 1;
                msg2[0] >>= 1;
            }
            return count < 4;
        } else {
            for (int i = 0; i < msg2.length; i++) {
                msg2[i] = (byte) (exp[i] ^ msg[i]);
            }

            for (int i = 0; i < msg2.length; i++) {
                while (msg2[i] != 0) {
                    count += msg2[i] & 1;
                    msg2[i] >>= 1;
                }
            }
            return count < msg2.length * 3;
        }
    }

    private byte[] getMsg(MsgType msg) {
        switch (msg) {
            case REQ:
                return req;
            case ANSREQ:
                return ansreq;
            case ACK:
                return ack;
            case NACK:
                return nack;
            case MSGPOS:
                return msgpos;
            default:
                return null;
        }
    }

    private int getExpectLength() {
        switch (expectMsg) {
            case REQ:
                return req.length;
            case ANSREQ:
                return ansreq.length;
            case ACK:
                return ack.length;
            case NACK:
                return nack.length;
            case MSGPOS:
                return msgpos.length;
            default:
                return 16;
        }
    }

    /**
     * @return 0 when game still running, 1 if the circle won, 2 if the cross won this game.
     */
    private byte checkDone() {
        byte[] checks = new byte[8];

        // check the rows
        checks[0] = checkThreeStates(0, 1, 2);
        checks[1] = checkThreeStates(3, 4, 5);
        checks[2] = checkThreeStates(6, 7, 8);

        // check the columns
        checks[3] = checkThreeStates(0, 3, 6);
        checks[4] = checkThreeStates(1, 4, 7);
        checks[5] = checkThreeStates(2, 5, 8);

        // check the diagonals
        checks[6] = checkThreeStates(0, 4, 8);
        checks[7] = checkThreeStates(2, 4, 6);

        for (int i = 0; i < 8; i++) {
            if (checks[i] != 0) {
                return checks[i];
            }
        }

        // nobody has won the game
        return 0;
    }

    private byte checkThreeStates(int i1, int i2, int i3) {
        if (state[i1] != 0 && state[i1] == state[i2] && state[i2] == state[i3]) {
            // the three states are the same and are not 0, so here's a winner
            return state[i1];
        }
        return 0;
    }

    public boolean setSymbol(byte symb, int pos) {
        if ((!myTurn && symb == symbol) || (symb != symbol && myTurn)) {
            return false;
        }
        if (pos < 0 || pos >= 9 || state[pos] != 0) {
            return false;
        }

        changeTurn();
        state[pos] = symb;

        if (symb != symbol) {
            // symbol was set by other player, so update the gui by messaging the handler
            sendToView(TicTacToeView.REDRAW);
        } else {
            // symbol was set by this player, broadcast the change
            sendToOp(MsgType.MSGPOS, pos);
        }

        byte check = checkDone();
        if (check == symbol) {
            // display popup to indicate that you the game is over
            sendToView(TicTacToeView.YOU_HAVE_WON);
            stop();
        } else if (check == 0) {
            // game is still running
        } else {
            // the user has lost this game...
            sendToView(TicTacToeView.YOU_HAVE_LOST);
            stop();
        }
        return true;
    }

    private void sendToView(int message) {
        Message msg = guiHandler.obtainMessage();
        msg.arg1 = message;
        msg.sendToTarget();
    }

    public void start() {
        if (symbol == 1) {
            waitOpponent();
        } else {
            findOpponent();
        }
    }

    private void stop() {
        if(sender != null){
            sender.close();
        }
        if(receiver!=null){
            receiver.close();
        }
    }

    private void resendLastMsg() {

    }

    @Override
    public void onMessageReceived(byte[] msg) {
        Log.d("TTT", "received message, length: " + msg.length);
        if (getExpectLength() == msg.length) {
            recvFromOp(msg);
        } else if (checkMsg(getMsg(MsgType.NACK), msg)) {
            // if we received a NACK, resend the last message
            resendLastMsg();
        } else {
            Log.d("TTT", "size wrong: " + (msg.length - getExpectLength()));
            sendToOp(MsgType.NACK, -1);
        }
    }

    private class SendToOp extends Thread {
        private byte[] data;

        SendToOp(byte[] data) {
            this.data = data;
        }

        @Override
        public void run() {
            receiver.stopListening();

            // give the other side time to startListening receiving again...
            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {

            }

            // to set the volume to max before starting to send
            AudioManager audioManager = (AudioManager) context.getSystemService(Context
                    .AUDIO_SERVICE);
            int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
            sender.send(data);
            // now reset the volume again:
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);

            receiver.startListening();
        }
    }


}

