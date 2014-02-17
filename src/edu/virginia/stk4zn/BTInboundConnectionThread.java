package edu.virginia.stk4zn;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;


/**
 * Created by sean on 1/15/14.
 */
public class BTInboundConnectionThread extends Thread{

    private BluetoothSocket socket;
    private ConversationActivity act;
    private boolean waiting;
    private PairedDevice device;
    private Handler uiHandler;

    public BTInboundConnectionThread(ConversationActivity activity, PairedDevice device, BluetoothSocket socket){
        super("Inbound Thread from " +device.getAddress());
        Log.d(Static.DEBUG,"Creating inbound connection thread to: " + socket.getRemoteDevice().getAddress());
        this.device = device;
        this.act = activity;
        this.socket = socket;
        this.uiHandler = new Handler(Looper.getMainLooper());


    }
    @Override
    public void run(){

        Log.d(Static.DEBUG, "Listening to device " +
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

                Log.d(Static.DEBUG, "Received message: " + message + " from device " +
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
            Log.d(Static.DEBUG, "IO exception on receipt");
            device.disconnect();

        }


    }



    public void cancel() {
        Log.d(Static.DEBUG, "Killing inbound thread: " + socket.getRemoteDevice().getAddress());

        waiting = false;
        try {
            socket.getInputStream().close();
        } catch (IOException e) {
            Log.d(Static.DEBUG, "Failed to close Input Stream from: " + socket.getRemoteDevice().getAddress());
        }

    }



}
