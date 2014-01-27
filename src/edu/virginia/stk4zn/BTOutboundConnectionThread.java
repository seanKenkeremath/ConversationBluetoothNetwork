package edu.virginia.stk4zn;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;


/**
 * Created by sean on 1/15/14.
 */
public class BTOutboundConnectionThread extends Thread{

    private BluetoothSocket socket;
    LinkedList<byte[]> queue;
    private MainActivity act;
    private boolean waiting;
    final static String INIT_MESSAGE = "CONNECTED: ";

    public BTOutboundConnectionThread(MainActivity activity, BluetoothSocket socket){
        Log.d(MainActivity.DEBUG,"Creating outbound connection thread to: " + socket.getRemoteDevice().getAddress());
        this.act = activity;
        this.socket = socket;
        String testMessage = INIT_MESSAGE + socket.getRemoteDevice().getAddress();
        queue = new LinkedList<byte[]>();
        queueMessage(testMessage.getBytes());


    }
    @Override
    public void run(){
        Log.d(MainActivity.DEBUG,"Running outbound connection thread to: " + socket.getRemoteDevice().getAddress());

        /*
        try {
            socket.connect();
        } catch (IOException connectException) {
            Log.d(MainActivity.DEBUG, "Could not connect to: " + socket.getRemoteDevice().getAddress());
            try {
                act.removeDevice(getDevice());
                socket.close();
            } catch (IOException closeException) {
                Log.d(MainActivity.DEBUG, "Could not close socket: " + socket.getRemoteDevice().getAddress());

            }
            return;
        }
        */

        waiting = true;

        while (waiting){
            //if queue has messages
            if (!queue.isEmpty()){
                Log.d(MainActivity.DEBUG, "preparing to send message: " + queue.peek() + " to " +
                        socket.getRemoteDevice().getAddress());
                try {
                   OutputStream out = socket.getOutputStream();
                    out.write(queue.peek());
                    queue.poll();
                } catch (IOException e) {
                    Log.d(MainActivity.DEBUG, "failed to write to: " + socket.getRemoteDevice().getAddress());
                    act.removeDevice(getDevice());
                    cancel();
                }
            }
        }


    }


    public BluetoothSocket getSocket(){
        return socket;
    }

    public BluetoothDevice getDevice(){
        return socket.getRemoteDevice();
    }


    @Override
    public boolean equals(Object o){

        if (o instanceof BTOutboundConnectionThread){
            return ((BTOutboundConnectionThread) o).getDevice().getAddress().equals(getDevice().getAddress());
        }
        return false;
    }



    public void cancel() {
        Log.d(MainActivity.DEBUG, "Killing outbound thread: " + socket.getRemoteDevice().getAddress());

        waiting = false;
        try {
            socket.close();
            act.removeDevice(getDevice());
        } catch (IOException e) {
            Log.d(MainActivity.DEBUG, "Failed to close socket: " + socket.getRemoteDevice().getAddress());
        }

    }

    public void queueMessage(byte[] message){
        queue.add(message);
    }


}
