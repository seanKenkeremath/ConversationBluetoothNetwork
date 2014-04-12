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
    boolean sendSamples;

    public AsyncConnectTask(ConversationActivity activity, boolean sendSamples){
        super();
        this.act = activity;
        this.sendSamples = sendSamples;
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

        Log.d(Static.DEBUG,"Attempting connection to " + device.getAddress());


        try {
                BluetoothSocket socket = device.createRfcommSocketToServiceRecord(Static.BLUETOOTH_SERVICE_UUID);

                if (socket!=null){


                    try {
                        socket.connect();
                    } catch (IOException connectException) {
                        Log.d(Static.DEBUG, "Could not connect to: " + socket.getRemoteDevice().getAddress());
                        try {
                            socket.close();
                        } catch (IOException closeException) {
                            Log.d(Static.DEBUG, "Could not close socket: " + socket.getRemoteDevice().getAddress());

                        }
                        return false;
                    }

                    final PairedDevice connectedDevice = new PairedDevice(act, socket);
                    act.addDevice(connectedDevice);

                    if (sendSamples){
                        connectedDevice.sendSamples();
                        act.addNewMAC(connectedDevice.getAddress());
                    }

                }

            } catch (IOException e) {
                Log.d(Static.DEBUG, "Failed Connection to " + device.getName() + ": " + device.getAddress());
                return false;
            }

        return true;
    }
}
