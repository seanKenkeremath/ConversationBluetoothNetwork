package edu.virginia.stk4zn;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

public class MainActivity extends Activity {
    /**
     * Called when the activity is first created.
     */

    final static int REQUEST_ENABLE_BT = 333;
    final static String BLUETOOTH_ADAPTER_NAME = "SOCINT";
    final static String DEBUG = "SOCDEB";

    Button timeButton;


    BluetoothAdapter adapt;
    private HashSet<PairedDevice> pairedDevices;
    private Handler handler;

    private BroadcastReceiver discoveryReceiver;
    private BluetoothServerThread serverThread;
    private ProcessThread processThread;
    private BTDiscoveryService discoveryThread;
    private TextView mainText;
    private TextView messageText;
    private ArrayList<String> messages;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        handler = new Handler();
        timeButton = (Button) findViewById(R.id.timeButton);
        mainText = (TextView) findViewById(R.id.mainText);
        messageText = (TextView) findViewById(R.id.messageText);
        messages = new ArrayList<String>();
        pairedDevices = new HashSet<PairedDevice>();


        timeButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Log.d(DEBUG, pairedDevices.size() + " devices, Threads: " + Thread.activeCount());
            }
        });


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
                    Log.d(DEBUG,"Discovered Device: " + device.getName() + ": "+device.getAddress());

                    boolean contains = false;

                    for (PairedDevice connectedDevice: pairedDevices){
                        if (connectedDevice.getAddress().equals(device.getAddress())){
                            contains = true;
                        }
                    }

                    if (device.getName().equals(BLUETOOTH_ADAPTER_NAME) && !contains){
                        Log.d(DEBUG,"New Device: " + device.getName() + ": "+device.getAddress());
                        AsyncConnectTask task = new AsyncConnectTask(MainActivity.this);
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
        mainText.setText(display);
    }
    public void startDiscovery(){
        if (adapt.isDiscovering()){
            cancelDiscovery();
        }

            Log.d(DEBUG,"Starting Discovery..");
            adapt.startDiscovery();

    }
    public void cancelDiscovery(){
        Log.d(DEBUG,"Canceling Discovery..");
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

    }

    public void removeDevice(PairedDevice device){
        pairedDevices.remove(device);
        getMessage("DISCONNECT: " + device.getAddress());
    }

    public BluetoothAdapter getBluetoothAdapter(){
        return adapt;
    }


    public void hostConnection(){
        adapt.setName(BLUETOOTH_ADAPTER_NAME);
        if (serverThread!=null){
            serverThread.cancel();
        }
        serverThread = new BluetoothServerThread(this, adapt);
        serverThread.start();
    }

    private void startProcessing(){
        if (processThread!=null){
            processThread.cancel();
        }
        processThread = new ProcessThread(this);
        processThread.start();
    }

    private void startDiscoveryThread(){
        if (discoveryThread!=null){
            discoveryThread.cancel();
        }
        discoveryThread = new BTDiscoveryService(this);
        discoveryThread.start();

    }

    private void enableBluetooth(){
        Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBluetooth,REQUEST_ENABLE_BT);
    }


    private void makeDiscoverable(int duration){
        Log.d(DEBUG, "Setting Discoverable..");
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
        Log.d(DEBUG, "ON DESTROY");
        this.unregisterReceiver(discoveryReceiver);
        discoveryThread.cancel();
        processThread.cancel();
        serverThread.cancel();
        adapt.cancelDiscovery();
        adapt.setName("NOT" + BLUETOOTH_ADAPTER_NAME);
        for (PairedDevice device: pairedDevices){
            device.disconnect();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent){
            if (requestCode == REQUEST_ENABLE_BT){
                if (resultCode == RESULT_OK) {
                    Log.d(DEBUG, "Successfully enabled bluetooth");
                    makeDiscoverable(0);
                    startDiscoveryThread();
                    hostConnection();
                } else{
                    Log.d(DEBUG,"Failed to enable bluetooth");
                }
            }
    }

    public Handler getHandler(){
        return handler;
    }

}
