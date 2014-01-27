package edu.virginia.stk4zn;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;

/**
 * Created by sean on 1/24/14.
 */
public class ProcessThread extends Thread {


    private MainActivity act;
    final static int WAIT_TIME = 50000;
    private boolean waiting;

    public ProcessThread(MainActivity act){
        this.act = act;

    }


    @Override
    public void run(){
        Log.d(MainActivity.DEBUG,"Starting Process Thread");
        waiting = true;
        while(waiting){
            act.sendMessageToAll(""+System.currentTimeMillis());
            try {
                Thread.sleep(WAIT_TIME);
            } catch (InterruptedException e) {
                Log.d(MainActivity.DEBUG, "Process thread interuptted");
            }
        }

    }

    public void cancel() {
        Log.d(MainActivity.DEBUG, "Killing Process Thread");
        waiting = false;
    }
}
