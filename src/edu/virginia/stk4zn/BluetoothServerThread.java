package edu.virginia.stk4zn;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by sean on 1/15/14.
 */
public class BluetoothServerThread extends Thread{

    private BluetoothServerSocket serverSocket;
    private BluetoothAdapter adapt;
    private ConversationActivity act;
    final static String SERVICE_NAME  = "SOCIAL_INTERACTION";
    private final static String UUIDString = "662ab3f4-c79c-11d1-3a37-a500712cf000";
    public final static UUID SERVICE_UUID = UUID.fromString(UUIDString);



    private boolean waiting;

    public BluetoothServerThread(ConversationActivity activity, BluetoothAdapter adapter){
        super("Server Thread");
        Log.d(ConversationActivity.DEBUG,"Server thread created");

        this.adapt = adapter;
        this.act = activity;

        try {
            serverSocket = adapt.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SERVICE_UUID);
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(ConversationActivity.DEBUG,"Could not start host connection");

            //failed to start server
        }

    }
    @Override
    public void run(){
        BluetoothSocket socket = null;

        waiting = true;

        while (waiting){

            if (serverSocket==null){
                break;
            }

            try {
                Log.d(ConversationActivity.DEBUG,"Hosting Connection.. waiting for devices..");
                socket = serverSocket.accept();
                if (socket!=null){
                    Log.d(ConversationActivity.DEBUG,"connection made to "+ socket.getRemoteDevice().getAddress());

                    final PairedDevice connectedDevice = new PairedDevice(act,socket);
                    act.getHandler().post(new Runnable() {

                        @Override
                        public void run() {
                            act.addDevice(connectedDevice);

                        }
                    });
                }
            } catch (IOException e){
                cancel();
                Log.d(ConversationActivity.DEBUG,"Error Hosting Connection");
            }


        }

    }


    public void cancel() {
        waiting = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            Log.d(ConversationActivity.DEBUG,"Failed to cancel server thread");

        }

    }


}
