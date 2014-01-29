package edu.virginia.stk4zn;


import android.util.Log;


/**
 * Created by sean on 1/24/14.
 */
public class BTDiscoveryService extends Thread {


    private MainActivity act;
    final static int WAIT_TIME = 30000;
    private boolean waiting;

    public BTDiscoveryService(MainActivity act){
        this.act = act;

    }


    @Override
    public void run(){
        Log.d(MainActivity.DEBUG,"Starting Discovery Thread");
        waiting = true;
        while(waiting){
            if (!act.getBluetoothAdapter().isDiscovering()){
                act.startDiscovery();
            }
            try {
                Thread.sleep(WAIT_TIME);
            } catch (InterruptedException e) {
                Log.d(MainActivity.DEBUG, "Discovery thread interuptted");
            }
        }

    }

    public void cancel() {
        Log.d(MainActivity.DEBUG, "Killing Discovery Thread");
        waiting = false;
    }
}
