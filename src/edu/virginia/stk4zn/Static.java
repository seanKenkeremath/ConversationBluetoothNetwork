package edu.virginia.stk4zn;

import android.media.AudioFormat;

import java.util.UUID;

/**
 * Created by sean on 2/11/14.
 */
public class Static {

    //audio analysis
    public static final int AUDIO_RECORDER_BPP = 16;
    public static final int AUDIO_RECORDER_SAMPLERATE = 44100; //8000;
    public static final int AUDIO_RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    public static final int AUDIO_RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    public static final int AUDIO_CHANNELS = 1;
    public static final long AUDIO_BYTE_RATE = AUDIO_RECORDER_BPP * AUDIO_RECORDER_SAMPLERATE * AUDIO_CHANNELS/8;
    public static final float AUDIO_WINDOW_SIZE = 1f; //seconds
    public static final float AUDIO_FRAME_DURATION = 25f; //ms
    public static final float AUDIO_FRAME_SHIFT = 10f; //ms
    public static final float AUDIO_BUFFER_SECONDS = AUDIO_WINDOW_SIZE;
    public static final int AUDIO_BUFFER_SIZE = (int) (AUDIO_RECORDER_SAMPLERATE*AUDIO_BUFFER_SECONDS);

    //training
    public static final String TRAINING_FOLDER = "SocInt";
    public static final String TRAINING_FILENAME = "training";
    public static final String TRAINING_FILE_EXTENSION = ".train";
    //bluetooth
    public final static String BLUETOOTH_ADAPTER_NAME = "SOCINT";
    public final static String BLUETOOTH_INIT_MESSAGE = "CONNECTED: ";
    public final static int BLUETOOTH_DISCOVERY_WAIT_TIME = 30000;
    public final static String BLUETOOTH_SERVICE_NAME  = "SOCIAL_INTERACTION";
    private final static String BLUETOOTH_UUIDString = "662ab3f4-c79c-11d1-3a37-a500712cf000";
    public final static UUID BLUETOOTH_SERVICE_UUID = UUID.fromString(BLUETOOTH_UUIDString);



    //debug tag
    public final static String DEBUG = "SOCDEB";
}
