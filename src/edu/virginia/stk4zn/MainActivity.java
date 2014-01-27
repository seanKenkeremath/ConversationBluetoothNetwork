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
    String deviceList = "";

    Button discButton;
    Button timeButton;


    BluetoothAdapter adapt;
    private HashSet<BluetoothDevice> pairedDevices;
    private LinkedList<BluetoothDevice> toConnect;
    private ArrayList<BTOutboundConnectionThread> outThreads;
    private ArrayList<BTInboundConnectionThread> inThreads;

    private BroadcastReceiver discoveryReceiver;
    private BroadcastReceiver discoveryEnder;
    private BluetoothServerThread serverThread;
    private ProcessThread processThread;
    private TextView mainText;
    private TextView messageText;
    private ArrayList<String> messages;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        discButton = (Button) findViewById(R.id.discButton);
        timeButton = (Button) findViewById(R.id.timeButton);
        mainText = (TextView) findViewById(R.id.mainText);
        messageText = (TextView) findViewById(R.id.messageText);
        messages = new ArrayList<String>();
        pairedDevices = new HashSet<BluetoothDevice>();
        outThreads = new ArrayList<BTOutboundConnectionThread>();
        inThreads = new ArrayList<BTInboundConnectionThread>();
        toConnect = new LinkedList<BluetoothDevice>();


        timeButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Log.d(DEBUG,pairedDevices.size() + " devices, Threads: " + Thread.activeCount());
            }
        });

        discButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Log.d(DEBUG, "Discovering Devices..");
                discoverDevices();
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

        discoveryEnder = new BroadcastReceiver(){

            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)){
                    Log.d(DEBUG, "Discovery ended: ");
                    connectDevicesInQueue();
                }
            }
        };

        discoveryReceiver = new BroadcastReceiver(){

            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(BluetoothDevice.ACTION_FOUND)){
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.d(DEBUG,"Discovered Device: " + device.getName() + ": "+device.getAddress());
                    if (device.getName().equals(BLUETOOTH_ADAPTER_NAME) && !pairedDevices.contains(device)){
                        Log.d(DEBUG,"New Device: " + device.getName() + ": "+device.getAddress());
                        deviceList+=device.getName()+ ": " + device.getAddress()+"\n";
                        toConnect.addLast(device);

                    }

                    mainText.setText(deviceList);
                }

            }
        };

        IntentFilter discoveryFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND); //?
        registerReceiver(discoveryReceiver, discoveryFilter);

        IntentFilter discoveryEndedFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(discoveryEnder,discoveryEndedFilter);

        if (!adapt.isEnabled()){
            //bluetooth not enabled
            enableBluetooth();
            return;
        }

        makeDiscoverable(0);
        hostConnection();

    }

    public void sendMessageToAll(String message){
        for (BTOutboundConnectionThread thread: outThreads){
            thread.queueMessage(message.getBytes());
        }
    }

    public void getMessage(String message){
        messages.add(message);
        String displayMessage = "";
        for (String concat: messages){
            displayMessage+=concat+"\n";
        }
        messageText.setText(displayMessage);
    }

    public void addDevice(BluetoothDevice device){
        pairedDevices.add(device);
    }

    public void removeDevice(BluetoothDevice device){
        pairedDevices.remove(device);
    }

    public void addInboundThread(BTInboundConnectionThread thread){
        inThreads.add(thread);
        thread.start();
    }

    public void addOutboundThread(BTOutboundConnectionThread thread){
        outThreads.add(thread);
        thread.start();
    }

    private void hostConnection(){
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

    private void connectDevicesInQueue(){
        Log.d(DEBUG,"Attempting connections to all " + toConnect.size() + " devices in queue");
        for (BluetoothDevice device: toConnect){
            AsyncConnectTask task = new AsyncConnectTask(this);
            task.execute(device);

        }
    }

    private void enableBluetooth(){
        Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBluetooth,REQUEST_ENABLE_BT);
    }

    private void discoverDevices(){
        adapt.cancelDiscovery();
        deviceList = "";
        adapt.startDiscovery();
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
        this.unregisterReceiver(discoveryEnder);
        //destroy thread
        processThread.cancel();
        serverThread.cancel();
        for (BTOutboundConnectionThread thread: outThreads){
            try {
                thread.getSocket().close();
                Log.d(DEBUG,"Closed socket " + thread.getDevice());
            } catch (IOException e) {
                Log.d(DEBUG,"Could not close socket " + thread.getDevice());
            }
            thread.cancel();
        }

        for (BTInboundConnectionThread thread: inThreads){
            try {
                thread.getSocket().close();
                Log.d(DEBUG,"Closed socket " + thread.getDevice());
            } catch (IOException e) {
                Log.d(DEBUG, "Could not close socket " + thread.getDevice());
            }
            thread.cancel();

        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent){
            if (requestCode == REQUEST_ENABLE_BT){
                if (resultCode == RESULT_OK) {
                    Log.d(DEBUG, "Successfully enabled bluetooth");
                    makeDiscoverable(0);
                    hostConnection();
                } else{
                    Log.d(DEBUG,"Failed to enable bluetooth");
                }
            }
    }
}
