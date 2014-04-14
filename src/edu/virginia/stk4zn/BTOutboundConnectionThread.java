package edu.virginia.stk4zn;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;


/**
 * Created by sean on 1/15/14.
 */
public class BTOutboundConnectionThread extends Thread{

    private BluetoothSocket socket;
    private PairedDevice device;
    LinkedList<byte[]> queue;
    private ConversationActivity act;
    private boolean waiting;

    public BTOutboundConnectionThread(ConversationActivity activity, PairedDevice device, BluetoothSocket socket){
        super("Outbound Thread to " +device.getAddress());
        Log.d(Static.DEBUG,"Creating outbound connection thread to: " + socket.getRemoteDevice().getAddress());
        this.device = device;
        this.act = activity;
        this.socket = socket;
        String testMessage = Static.BLUETOOTH_INIT_MESSAGE;
        queue = new LinkedList<byte[]>();
        queueDisplayMessage(testMessage);


    }
    @Override
    public void run(){
        Log.d(Static.DEBUG,"Running outbound connection thread to: " + socket.getRemoteDevice().getAddress());

        waiting = true;

        while (waiting){
            //if queue has messages
            if (!queue.isEmpty()){
                Log.d(Static.DEBUG, "preparing to send message: " + queue.peek() + " to " +
                        socket.getRemoteDevice().getAddress());
                try {
                   OutputStream out = socket.getOutputStream();
                    //out.write(queue.peek());
                    byte[] toWrite = queue.peek();
                    out.write(toWrite);
                    out.flush();
                    /*
                    try {
                        Thread.sleep(500); //need to separate between individual sends.. other phone is not separating
                    } catch (InterruptedException e) {
                       Log.d(Static.DEBUG, "Interrupted");
                    }
                    */
                    queue.poll();
                } catch (IOException e) {
                    Log.d(Static.DEBUG, "failed to write to: " + socket.getRemoteDevice().getAddress());
                    device.disconnect();
                }
            }
        }


    }




    public void cancel() {
        Log.d(Static.DEBUG, "Killing outbound thread: " + socket.getRemoteDevice().getAddress());
        waiting = false;
        try {
            socket.getOutputStream().close();
        } catch (IOException e) {
            Log.d(Static.DEBUG, "Failed closing output stream: " + socket.getRemoteDevice().getAddress());
        }


    }

    //first byte is opcode
    private void queueMessage(byte[] message){
        queue.add(message);
    }

    public void queueDisplayMessage(String message){
        byte[] messageWithoutOpcode = message.getBytes();
        byte[] fullMessage  = new byte[messageWithoutOpcode.length + 1];

        fullMessage[0] = Static.OPCODE_DISPLAY_MESSAGE;
        for (int i = 0; i < messageWithoutOpcode.length; i++){
            fullMessage[i+1] = messageWithoutOpcode[i];
        }

        queueMessage(fullMessage);
    }

    public void queueSendSamples(){

        ArrayList<Byte> messageBuilder = new ArrayList<Byte>();
        //opcode
        messageBuilder.add(Static.OPCODE_SAMPLES);
        //total size starts at zero and fills in after counting
        int totalBytes = 0;
        byte blank = 0x0;
        messageBuilder.add(blank); //place holder
        messageBuilder.add(blank); //place holder
        messageBuilder.add(blank); //place holder
        messageBuilder.add(blank); //place holder



        try {

            File posTraining = new File(Static.getPositiveTrainingFilepath());

            if (!posTraining.exists()){
                return;
            }

            BufferedReader posIn = new BufferedReader(new FileReader(posTraining));
            String line;

            String newLine = "\n";
            byte[] newLineBytes = newLine.getBytes(); //delimiter

            while ((line = posIn.readLine())!=null){

                byte[] lineBytes = line.getBytes();
                for (int i = 0; i < lineBytes.length; i++){

                    //convert + to -
                    if (i == 0){
                        String negSymbol = "-";
                        messageBuilder.add(negSymbol.getBytes()[0]);   //probably easier way to insert -
                    } else {
                        messageBuilder.add(lineBytes[i]);
                    }

                    totalBytes++;
                }

                //delimiter
                for (byte delBytes : newLineBytes){
                    messageBuilder.add(delBytes);
                    totalBytes++;
                }
            }

            byte[] size = ByteBuffer.allocate(4).putInt(totalBytes).array();

            messageBuilder.set(1, size[0]);
            messageBuilder.set(2, size[1]);
            messageBuilder.set(3, size[2]);
            messageBuilder.set(4, size[3]);

            byte[] test = new byte[4];
            for (int i = 0; i < 4; i ++){
                test[i] = messageBuilder.get(i+1);
            }

            /*
            ByteBuffer testWrap = ByteBuffer.wrap(test);
            int testUnWrap = testWrap.getInt();
            Log.d(Static.DEBUG, "Size should be: " + totalBytes + " and size is " + testUnWrap);
            */
            this.queueDisplayMessage("Fetching sample (" + totalBytes+ " bytes)");
            byte[] finalMessage = new byte[messageBuilder.size()];
            for (int i = 0 ; i < messageBuilder.size(); i++){
                finalMessage[i] = messageBuilder.get(i);
            }

            /*
            StringBuilder printMessage = new StringBuilder();
            for (byte character : finalMessage){
                printMessage.append((char)character);
            }
             Log.d(Static.DEBUG, "Sending: \n" );
            for (String printLine : printMessage.toString().split("\n")){
                Log.d(Static.DEBUG, printLine+"\n" );
            }
            */

            queueMessage(finalMessage);
            //queueDisplayMessage("Fetch Complete");


        } catch (FileNotFoundException e) {
            Log.d(Static.DEBUG, "FILENOTFOUND sending samples");
        } catch (IOException e) {
            Log.d(Static.DEBUG, "IO Exception sending samples");
        }


    }


}
