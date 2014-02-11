package edu.virginia.stk4zn;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;

/**
 * Created by sean on 1/21/14.
 */
public class AsyncConnectTask extends AsyncTask<BluetoothDevice, Integer, Boolean> {


    ConversationActivity act;

    public AsyncConnectTask(ConversationActivity activity){
        this.act = activity;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (result){
            act.startDiscovery();
        }
    }

    @Override
    protected void onPreExecute() {
        act.cancelDiscovery();
    }

    @Override
    protected Boolean doInBackground(BluetoothDevice... devices) {

        BluetoothDevice device = devices[0];

        Log.d(ConversationActivity.DEBUG,"Attempting connection to " + device.getAddress());


        try {
                BluetoothSocket socket = device.createRfcommSocketToServiceRecord(BluetoothServerThread.SERVICE_UUID);

                if (socket!=null){


                    try {
                        socket.connect();
                    } catch (IOException connectException) {
                        Log.d(ConversationActivity.DEBUG, "Could not connect to: " + socket.getRemoteDevice().getAddress());
                        try {
                            socket.close();
                        } catch (IOException closeException) {
                            Log.d(ConversationActivity.DEBUG, "Could not close socket: " + socket.getRemoteDevice().getAddress());

                        }
                        return false;
                    }

                    final PairedDevice connectedDevice = new PairedDevice(act, socket);
                    act.getHandler().post(new Runnable(){

                        @Override
                        public void run() {
                            act.addDevice(connectedDevice);
                        }
                    });

                }

            } catch (IOException e) {
                Log.d(ConversationActivity.DEBUG, "Failed Connection to " + device.getName() + ": " + device.getAddress());
                return false;
            }

        return true;
    }
}
