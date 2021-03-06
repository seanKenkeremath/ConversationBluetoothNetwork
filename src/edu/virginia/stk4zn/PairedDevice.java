package edu.virginia.stk4zn;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;

/**
 * Created by sean on 1/29/14.
 */
public class PairedDevice {

    private BluetoothDevice device;
    private BluetoothSocket socket;
    private BTInboundConnectionThread inThread;
    private BTOutboundConnectionThread outThread;
    private ConversationActivity act;

    public PairedDevice(ConversationActivity act, BluetoothSocket socket){
        this.act = act;
        this.socket = socket;
        this.device = socket.getRemoteDevice();
        this.outThread = new BTOutboundConnectionThread(act, this, socket);
        this.inThread = new BTInboundConnectionThread(act, this, socket);
    }



    public void startThreads(){
        inThread.start();
        outThread.start();
    }

    /*
    //first byte is opcode
    private void queueMessage(byte[] message){
        outThread.queueMessage(message);
    }
    */

    public void queueDisplayMessage(String message){
        outThread.queueDisplayMessage(message);
    }

    public void sendSamples(){
        outThread.queueSendSamples();
    }


    public String getAddress(){
        return device.getAddress();
    }

    public void disconnect(){

        inThread.cancel();
        outThread.cancel();

        /*

        try {
            inThread.cancel();
            //inThread.join();
            outThread.cancel();
            //outThread.join();
        } catch (InterruptedException e) {
            Log.d(Static.DEBUG, "interuppted");
        }
        */
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        act.getHandler().post(new Runnable(){

            @Override
            public void run() {
                act.removeDevice(PairedDevice.this);
            }
        });

    }

    @Override
    public boolean equals(Object o){
        if (o instanceof PairedDevice){
            return ((PairedDevice)o).getAddress().equals(getAddress());
        }
            return false;
    }

    @Override
    public int hashCode(){
        return getAddress().hashCode();
    }


}
