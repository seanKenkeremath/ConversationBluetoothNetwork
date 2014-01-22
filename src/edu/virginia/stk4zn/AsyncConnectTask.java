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


    MainActivity act;

    public AsyncConnectTask(MainActivity activity){
        this.act = activity;
    }

    @Override
    protected Boolean doInBackground(BluetoothDevice... devices) {

        BluetoothDevice device = devices[0];

        Log.d(MainActivity.DEBUG,"Attempting connection to " + device.getAddress());


        try {
                BluetoothSocket socket = device.createRfcommSocketToServiceRecord(BluetoothServerThread.SERVICE_UUID);

                if (socket!=null){

                    try {
                        socket.connect();
                    } catch (IOException connectException) {
                        Log.d(MainActivity.DEBUG, "Could not connect to: " + socket.getRemoteDevice().getAddress());
                        try {
                            act.removeDevice(device);
                            socket.close();
                        } catch (IOException closeException) {
                            Log.d(MainActivity.DEBUG, "Could not close socket: " + socket.getRemoteDevice().getAddress());

                        }
                    }

                    BTOutboundConnectionThread outThread = new BTOutboundConnectionThread(act, socket);
                    BTInboundConnectionThread inThread = new BTInboundConnectionThread(act, socket);

                    act.addDevice(device);
                    act.addOutboundThread(outThread);
                    act.addInboundThread(inThread);
                }

            } catch (IOException e) {
                Log.d(MainActivity.DEBUG, "Failed Connection to " + device.getName() + ": " + device.getAddress());
                return false;
            }

        return true;
    }
}
