package edu.virginia.stk4zn;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import svm.libsvm.svm;
import svm.libsvm.svm_model;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

public class ConversationActivity extends Activity {
    /**
     * Called when the activity is first created.
     */

    private final static int REQUEST_ENABLE_BT = 333;

    public boolean speaking_truth;


    Button statusButton;
    private Button speakingTruthButton;
    private int noiseThreshold;
    BluetoothAdapter adapt;

    private String logName;

    private HashSet<PairedDevice> pairedDevices;
    private Handler handler;

    private BroadcastReceiver discoveryReceiver;
    private BluetoothServerThread serverThread;
    private ProcessThread processThread;
    private BTDiscoveryService discoveryThread;
    private TextView connectedText;
    private TextView mfccText;
    private TextView messageText;
    private TextView speakingTruthText;
    private ArrayList<String> messages;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        speaking_truth = false;
        logName = getIntent().getStringExtra(Static.LOG_LOGNAME_BUNDLE_KEY);
        noiseThreshold = getIntent().getIntExtra(Static.AUDIO_NOISE_THRESHOLD_BUNDLE_NAME,0);
        handler = new Handler();
        statusButton = (Button) findViewById(R.id.timeButton);
        speakingTruthButton = (Button) findViewById(R.id.speakButton);
        speakingTruthText = (TextView) findViewById(R.id.speakText);
        connectedText = (TextView) findViewById(R.id.connectedText);
        messageText = (TextView) findViewById(R.id.messageText);
        mfccText = (TextView) findViewById(R.id.mfccText);
        messages = new ArrayList<String>();
        pairedDevices = new HashSet<PairedDevice>();


        statusButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String allThreads = "";
                for (Thread thread: Thread.getAllStackTraces().keySet()){
                    allThreads+=thread.getName() +"\n";
                }
                Log.d(Static.DEBUG, allThreads);
                Log.d(Static.DEBUG, pairedDevices.size() + " devices, Threads: " + Thread.activeCount());
            }
        });


        speakingTruthButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                speaking_truth = !speaking_truth;
                if (speaking_truth){
                    speakingTruthText.setText("Speaking (Truth)");
                } else {
                    speakingTruthText.setText("Not Speaking (Truth)");
                }

            }
        });


        //if log file already exists, delete previous
        File log = new File(Static.getLogOutputPath(logName));
        if (log.exists()){
            log.delete();
        }
        try {
            createLogHeader(log);
        } catch (IOException e) {
            Log.d(Static.DEBUG,"Failed writing log header");
        }
        initBluetooth();
        startProcessing();
    }

    public void initBluetooth(){



        adapt = BluetoothAdapter.getDefaultAdapter();
        if (adapt==null){
            //no bluetooth on this device
            return;
        }


        discoveryReceiver = new BroadcastReceiver(){

            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(BluetoothDevice.ACTION_FOUND)){
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.d(Static.DEBUG,"Discovered Device: " + device.getName() + ": "+device.getAddress());

                    boolean contains = false;

                    for (PairedDevice connectedDevice: pairedDevices){
                        if (connectedDevice.getAddress().equals(device.getAddress())){
                            contains = true;
                        }
                    }

                    if (device.getName().equals(Static.BLUETOOTH_ADAPTER_NAME) && !contains){
                        Log.d(Static.DEBUG,"New Device: " + device.getName() + ": "+device.getAddress());
                        AsyncConnectTask task = new AsyncConnectTask(ConversationActivity.this);
                        task.execute(device);
                    }

                }

            }
        };



        IntentFilter discoveryFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND); //?
        registerReceiver(discoveryReceiver, discoveryFilter);


        if (!adapt.isEnabled()){
            //bluetooth not enabled
            enableBluetooth();
            return;
        }

        makeDiscoverable(0);
        startDiscoveryThread();
        hostConnection();

    }


    public void displayPairedDevices(){
        String display ="";
        for (PairedDevice device: pairedDevices){
            display+=device.getAddress()+"\n";
        }
        connectedText.setText(display);
    }

    public void displayMFCC(String mfcc){
        mfccText.setText(mfcc);
    }

    public void startDiscovery(){
        if (adapt.isDiscovering()){
            cancelDiscovery();
        }

            Log.d(Static.DEBUG,"Starting Discovery..");
            adapt.startDiscovery();

    }
    public void cancelDiscovery(){
        Log.d(Static.DEBUG,"Canceling Discovery..");
        adapt.cancelDiscovery();
    }


    public void sendMessageToAll(String message){
        for (PairedDevice device: pairedDevices){
            device.queueMessage(message.getBytes());
        }
    }

    public void getMessage(String message){
        messages.add(message);
        String displayMessage = "";
        int numMessages = 10;
        if (numMessages>messages.size()){
            numMessages=messages.size();
        }
        for (int i =0; i < numMessages;i++){
            displayMessage += messages.get(messages.size()-numMessages+i) +"\n";
        }

        messageText.setText(displayMessage);
    }

    public void addDevice(PairedDevice device){
        pairedDevices.add(device);
        device.startThreads();
        displayPairedDevices();

    }

    public void removeDevice(PairedDevice device){
        pairedDevices.remove(device);
        displayPairedDevices();
        getMessage("DISCONNECT: " + device.getAddress());
    }

    public BluetoothAdapter getBluetoothAdapter(){
        return adapt;
    }


    public void hostConnection(){
        adapt.setName(Static.BLUETOOTH_ADAPTER_NAME);
        if (serverThread!=null){
            serverThread.cancel();
            try {
                Log.d(Static.DEBUG,"joining previous serverThread");
                serverThread.join();
            } catch (InterruptedException e) {
                Log.d(Static.DEBUG, "Interrupted joining serverThread");
            }
        }
        serverThread = new BluetoothServerThread(this, adapt);
        serverThread.start();
    }

    private void startProcessing(){
        if (processThread!=null){
            processThread.cancel();
            try {
                Log.d(Static.DEBUG,"joining previous processThread");
                processThread.join();
            } catch (InterruptedException e) {
                Log.d(Static.DEBUG, "Interrupted joining processThread");

            }
        }
        processThread = new ProcessThread(this, logName, noiseThreshold);
        processThread.start();
    }

    private void startDiscoveryThread(){
        if (discoveryThread!=null){
            discoveryThread.cancel();
            try {
                Log.d(Static.DEBUG,"joining previous discoveryThread");
                discoveryThread.join();
            } catch (InterruptedException e) {
                Log.d(Static.DEBUG,"Interrupted joining discoveryThread");
            }
        }
        discoveryThread = new BTDiscoveryService(this);
        discoveryThread.start();

    }

    private void enableBluetooth(){
        Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBluetooth,REQUEST_ENABLE_BT);
    }


    private void makeDiscoverable(int duration){
        Log.d(Static.DEBUG, "Setting Discoverable..");
        Intent makeDiscoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        makeDiscoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, duration);
        startActivity(makeDiscoverableIntent);


    }

    @Override
    protected void onResume(){
        super.onResume();

    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        Log.d(Static.DEBUG, "ON DESTROY");
        this.unregisterReceiver(discoveryReceiver);
        discoveryThread.cancel();
        processThread.cancel();
        serverThread.cancel();
        adapt.cancelDiscovery();
        adapt.setName("NOT" + Static.BLUETOOTH_ADAPTER_NAME);
        for (PairedDevice device: pairedDevices){
            device.disconnect();
        }
    }



    private void createLogHeader(File file) throws IOException {
        if (!file.exists()){
            file.createNewFile();
        }
        FileWriter fp = new FileWriter(file, false);
        fp.write("Time, Classification, Truth\n");
        fp.close();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent){
            if (requestCode == REQUEST_ENABLE_BT){
                if (resultCode == RESULT_OK) {
                    Log.d(Static.DEBUG, "Successfully enabled bluetooth");
                    makeDiscoverable(0);
                    startDiscoveryThread();
                    hostConnection();
                } else{
                    Log.d(Static.DEBUG,"Failed to enable bluetooth");
                }
            }
    }

    public Handler getHandler(){
        return handler;
    }

}
