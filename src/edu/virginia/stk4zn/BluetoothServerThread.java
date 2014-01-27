package edu.virginia.stk4zn;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by sean on 1/15/14.
 */
public class BluetoothServerThread extends Thread{

    private BluetoothServerSocket serverSocket;
    private BluetoothAdapter adapt;
    private MainActivity act;
    final static String SERVICE_NAME  = "SOCIAL_INTERACTION";
    private final static String UUIDString = "662ab3f4-c79c-11d1-3a37-a500712cf000";
    public final static UUID SERVICE_UUID = UUID.fromString(UUIDString);
    private Handler uiHandler;



    private boolean waiting;

    public BluetoothServerThread(MainActivity activity, BluetoothAdapter adapter){
        Log.d(MainActivity.DEBUG,"Server thread created");

        this.adapt = adapter;
        this.act = activity;
        this.uiHandler = new Handler(Looper.getMainLooper());

        try {
            serverSocket = adapt.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SERVICE_UUID);
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(MainActivity.DEBUG,"Could not start host connection");

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
                Log.d(MainActivity.DEBUG,"Hosting Connection.. waiting for devices..");
                socket = serverSocket.accept();
                if (socket!=null){
                    Log.d(MainActivity.DEBUG,"connection made to "+ socket.getRemoteDevice().getAddress());
                    act.addDevice(socket.getRemoteDevice());

                    final BTInboundConnectionThread inThread = new BTInboundConnectionThread(act, socket);
                    final BTOutboundConnectionThread outThread = new BTOutboundConnectionThread(act, socket);

                    uiHandler.post(new Runnable(){

                        @Override
                        public void run() {
                            act.addOutboundThread(outThread);
                            act.addInboundThread(inThread);
                        }
                    });

                    //serverSocket.close(); //maybe?
                    //waiting = false;
                }
            } catch (IOException e){
                cancel();
                Log.d(MainActivity.DEBUG,"Error Hosting Connection");
            }


        }

    }


    public void cancel() {
        waiting = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            Log.d(MainActivity.DEBUG,"Failed to cancel server thread");

        }

    }


}
