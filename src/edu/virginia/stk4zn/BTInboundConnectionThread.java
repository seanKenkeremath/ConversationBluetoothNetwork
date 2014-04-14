package edu.virginia.stk4zn;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;


/**
 * Created by sean on 1/15/14.
 */
public class BTInboundConnectionThread extends Thread{

    private BluetoothSocket socket;
    private ConversationActivity act;
    private boolean waiting;
    private PairedDevice device;
    private Handler uiHandler;
    private byte[] buffer;
    private ArrayList<Byte> messageArray;

    private int sampleBytesRemaining;

    public BTInboundConnectionThread(ConversationActivity activity, PairedDevice device, BluetoothSocket socket){
        super("Inbound Thread from " +device.getAddress());
        Log.d(Static.DEBUG,"Creating inbound connection thread to: " + socket.getRemoteDevice().getAddress());
        this.device = device;
        this.act = activity;
        this.socket = socket;
        this.uiHandler = new Handler(Looper.getMainLooper());

        int bufferSize = Static.BLUETOOTH_INPUT_BUFFER_SIZE;
        buffer = new byte[bufferSize];
        this.messageArray = new ArrayList<Byte>();

        this.sampleBytesRemaining = 0;


    }
    @Override
    public void run(){

        Log.d(Static.DEBUG, "Listening to device " +
                socket.getRemoteDevice().getAddress());


        waiting = true;

        try {
            InputStream in = socket.getInputStream();
            int bytesRead = -1;
            StringBuilder message;

            while (waiting) {
               // message = new StringBuilder();
                bytesRead = in.read(buffer);
                messageArray.clear();
                if (bytesRead != -1) {
                    while ((bytesRead == Static.BLUETOOTH_INPUT_BUFFER_SIZE) &&
                            (buffer[Static.BLUETOOTH_INPUT_BUFFER_SIZE - 1] != 0)) {

                        //since buffer is full, add all bytes to message
                        for (byte messageByte : buffer){
                            messageArray.add(messageByte);
                        }

                      //  message.append(new String(buffer, 0, bytesRead));
                        bytesRead = in.read(buffer);
                    }
                    //add remaining bytes to buffer
                    for (int i = 0; i < bytesRead; i++){
                        messageArray.add(buffer[i]);
                    }
                   // message.append(new String(buffer, 0, bytesRead - 1));
                }


                handleMessageArray();
                /*
                Log.d(Static.DEBUG, "Received message: " + message + " from device " +
                        socket.getRemoteDevice().getAddress());

                final String passMessage = message.toString();

                uiHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        act.getMessage(passMessage);
                    }
                });
                */
            }
        } catch (IOException e) {
            Log.d(Static.DEBUG, "IO exception on receipt");
            device.disconnect();

        }


    }


    public void handleMessageArray(){

        Log.d(Static.DEBUG,"Handlign message");

        if (messageArray.size() == 0){
            return;
        }

        if (sampleBytesRemaining > 0){
            Log.d(Static.DEBUG,sampleBytesRemaining + " bytes remaining to fetch");
        }

        if (sampleBytesRemaining == 0 ){
            Log.d(Static.DEBUG,"Message received has " + messageArray.size() + " bytes");
            byte opcode = messageArray.get(0);
            Log.d(Static.DEBUG,"Opcode is " + opcode);

            //trim opcode
            messageArray.remove(0);

            if (opcode == Static.OPCODE_DISPLAY_MESSAGE){
                displayReceivedMessage();
            } else if (opcode == Static.OPCODE_SAMPLES){
                initReceiveSamples();
            }
        } else {
            receiveSamples();
        }




    }

    private void initReceiveSamples(){
        //retrieve size

        if (messageArray.size() < 4){
            return;
        }

        byte[] sizeAsArray = {messageArray.get(0), messageArray.get(1), messageArray.get(2), messageArray.get(3)};
        int sizeAsInt = ByteBuffer.wrap(sizeAsArray).getInt();
        Log.d(Static.DEBUG, "Preparing to receive " + sizeAsInt +" bytes");
        sampleBytesRemaining = sizeAsInt;

        //trim off size
        messageArray.remove(0);
        messageArray.remove(0);
        messageArray.remove(0);
        messageArray.remove(0);

        receiveSamples();
    }

    private void receiveSamples(){
        try {
            Log.d(Static.DEBUG, "Writing samples from " + device.getAddress());
            String file_path = Static.getNegativeTrainingFilepath();


            File file = new File(file_path);

            if (!file.exists()){
                file.createNewFile();
            }

            //append all training
            FileWriter fp = new FileWriter(file, true);

            //for (byte character : messageArray){
            for (int i = 0; i < messageArray.size(); i++){
                byte charByte = messageArray.get(i);
                fp.write(charByte);
                //fp.write((char) charByte);
                sampleBytesRemaining --;
                if (sampleBytesRemaining == 0){

                    act.postMessage(device.getAddress() + ": Fetch Complete");
                    Log.d(Static.DEBUG, "No bytes remaining");

                    //if read messages have overlapped, create sub array and handle sub array
                    if (i > messageArray.size() -1){
                        Log.d(Static.DEBUG, "restructuring message array to rehandle");
                        ArrayList<Byte> tempArray = new ArrayList<Byte>();
                        for (int j = i; j < messageArray.size(); j++){
                            tempArray.add(messageArray.get(j));

                        }
                        this.messageArray = tempArray;
                        fp.flush();
                        fp.close();
                        handleMessageArray();
                        //recreateModel();
                        return;
                    }


                    fp.flush();
                    fp.close();
                    recreateModel();

                } else if (sampleBytesRemaining <0){
                    Log.d(Static.DEBUG, "ERROR: overreading bytes");

                }
            }

            fp.flush();
            fp.close();



        } catch (IOException e) {
            Log.d(Static.DEBUG, "Failed writing to training file");
        }
    }   
    private void recreateModel(){
        uiHandler.post(new Runnable(){

            @Override
            public void run() {
                act.reCreateModel();
            }
        });
    }

    private void displayReceivedMessage(){
        StringBuilder message = new StringBuilder();
        for (byte character : messageArray){
            message.append((char)character);
        }
        //message.append(messageArray.toArray());

        final String finMessage = message.toString();


        act.postMessage(device.getAddress()+": "+finMessage);
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
