package edu.virginia.stk4zn;

import android.bluetooth.BluetoothDevice;

/**
 * Created by sean on 1/28/14.
 */
public class SpeechVector {

    BluetoothDevice source;
    long startTime;
    long endTime;
    int mood;

    public SpeechVector(BluetoothDevice source, long startTime, long endTime, int mood){
        this.source = source;
        this.startTime = startTime;
        this.endTime = endTime;
        this.mood = mood;
    }


}
