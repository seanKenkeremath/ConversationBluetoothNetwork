package edu.virginia.stk4zn;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;


/**
 * Created by sean on 1/15/14.
 */
public class BTInboundConnectionThread extends Thread{

    private BluetoothSocket socket;
    private MainActivity act;
    private boolean waiting;
    private Handler uiHandler;

    public BTInboundConnectionThread(MainActivity activity,  BluetoothSocket socket){
        Log.d(MainActivity.DEBUG,"Creating inbound connection thread to: " + socket.getRemoteDevice().getAddress());

        this.act = activity;
        this.socket = socket;
        this.uiHandler = new Handler(Looper.getMainLooper());


    }
    @Override
    public void run(){

        Log.d(MainActivity.DEBUG, "Listening to device " +
                socket.getRemoteDevice().getAddress());

        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        waiting = true;

        try {
            InputStream in = socket.getInputStream();
            int bytesRead = -1;
            String message = "";

            while (waiting) {
                message = "";
                bytesRead = in.read(buffer);

                if (bytesRead != -1) {
                    while ((bytesRead == bufferSize) && (buffer[bufferSize - 1] != 0)) {
                        message = message + new String(buffer, 0, bytesRead);
                        bytesRead = in.read(buffer);
                    }
                    message = message + new String(buffer, 0, bytesRead - 1);
                }

                Log.d(MainActivity.DEBUG, "Received message: " + message + " from device " +
                        socket.getRemoteDevice().getAddress());

                final String passMessage = message;

                uiHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        act.getMessage(passMessage);
                    }
                });

            }
        } catch (IOException e) {
            Log.d(MainActivity.DEBUG, "IO exception on receipt");
            act.removeDevice(getDevice());
        }


    }

    @Override
    public boolean equals(Object o){

        if (o instanceof BTInboundConnectionThread){
            return ((BTInboundConnectionThread) o).getDevice().getAddress().equals(getDevice().getAddress());
        }
        return false;
    }

    public BluetoothSocket getSocket(){
        return socket;
    }

    public BluetoothDevice getDevice(){
        return socket.getRemoteDevice();
    }



    public void cancel() {
        waiting = false;
        try {
            socket.close();
            act.removeDevice(getDevice());
        } catch (IOException e) {
            Log.d(MainActivity.DEBUG, "Failed to close socket: " + socket.getRemoteDevice().getAddress());
        }
    }



}