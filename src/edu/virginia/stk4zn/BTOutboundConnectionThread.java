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
    private PairedDevice device;
    LinkedList<byte[]> queue;
    private MainActivity act;
    private boolean waiting;
    final static String INIT_MESSAGE = "CONNECTED: ";

    public BTOutboundConnectionThread(MainActivity activity, PairedDevice device, BluetoothSocket socket){
        super("Outbound Thread to " +device.getAddress());
        Log.d(MainActivity.DEBUG,"Creating outbound connection thread to: " + socket.getRemoteDevice().getAddress());
        this.device = device;
        this.act = activity;
        this.socket = socket;
        String testMessage = INIT_MESSAGE + act.getBluetoothAdapter().getAddress();
        queue = new LinkedList<byte[]>();
        queueMessage(testMessage.getBytes());


    }
    @Override
    public void run(){
        Log.d(MainActivity.DEBUG,"Running outbound connection thread to: " + socket.getRemoteDevice().getAddress());

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
                    device.disconnect();
                }
            }
        }


    }




    public void cancel() {
        Log.d(MainActivity.DEBUG, "Killing outbound thread: " + socket.getRemoteDevice().getAddress());
        waiting = false;
        try {
            socket.getOutputStream().close();
        } catch (IOException e) {
            Log.d(MainActivity.DEBUG, "Failed closing output stream: " + socket.getRemoteDevice().getAddress());
        }


    }

    public void queueMessage(byte[] message){
        queue.add(message);
    }


}
