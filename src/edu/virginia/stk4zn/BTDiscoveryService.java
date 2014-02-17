package edu.virginia.stk4zn;


import android.util.Log;


/**
 * Created by sean on 1/24/14.
 */
public class BTDiscoveryService extends Thread {


    private ConversationActivity act;
    private boolean waiting;

    public BTDiscoveryService(ConversationActivity act){
        super("Discovery Thread");
        this.act = act;

    }


    @Override
    public void run(){
        Log.d(Static.DEBUG,"Starting Discovery Thread");
        waiting = true;
        while(waiting){
            if (!act.getBluetoothAdapter().isDiscovering()){
                act.startDiscovery();
            }
            try {
                Thread.sleep(Static.BLUETOOTH_DISCOVERY_WAIT_TIME);
            } catch (InterruptedException e) {
                Log.d(Static.DEBUG, "Discovery thread interuptted");
            }
        }

    }

    public void cancel() {
        Log.d(Static.DEBUG, "Killing Discovery Thread");
        waiting = false;
    }
}
