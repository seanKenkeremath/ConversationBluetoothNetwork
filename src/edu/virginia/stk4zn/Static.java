package edu.virginia.stk4zn;

import android.media.AudioFormat;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by sean on 2/11/14.
 */
public class Static {


    //create model task
    public static final int CREATE_MODEL_MODE_TRAINING = 0;
    public static final int CREATE_MODEL_MODE_CONVERSATION = 1;

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
    public static final String AUDIO_NOISE_THRESHOLD_BUNDLE_NAME = "Threshold";



    //training
    public static final String TRAINING_FOLDER = "SocInt";
    private static final String TRAINING_FILENAME = "training";
    private static final String TRAINING_FILE_EXTENSION = ".train";
    private static final String TRAINING_NEGATIVE_FILENAME = "negative_samples";
    private static final String TRAINING_POSITIVE_FILENAME = "positive_samples";
    public static final String TRAINING_MODEL_FILENAME = "model";
    public static final String TRAINING_MODEL_FILE_EXT = ".model";




    public static String getNegativeTrainingFilepath(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,Static.TRAINING_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        String negative_filepath = (file.getAbsolutePath() + "/" +
                Static.TRAINING_NEGATIVE_FILENAME+Static.TRAINING_FILE_EXTENSION);

        return negative_filepath;
    }

    public static String getPositiveTrainingFilepath(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,Static.TRAINING_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        String positive_filepath = (file.getAbsolutePath() + "/" +
                Static.TRAINING_POSITIVE_FILENAME+Static.TRAINING_FILE_EXTENSION);

        return positive_filepath;
    }
    public static String getTrainingFilepath(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,Static.TRAINING_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }
        return (file.getAbsolutePath() + "/" + Static.TRAINING_FILENAME+Static.TRAINING_FILE_EXTENSION);
    }


    public static String getModelFilepath(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,Static.TRAINING_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        String modelFilepath = file.getAbsolutePath() + "/" +
                Static.TRAINING_MODEL_FILENAME+Static.TRAINING_MODEL_FILE_EXT;
        File modelFile = new File(modelFilepath);

        if (!modelFile.exists()){
            Log.d(Static.DEBUG, "model file does not exist.. creating new file");
            try {
                modelFile.createNewFile();
            } catch (IOException e) {
                Log.d(Static.DEBUG,"Failed creating new model file");
            }
        }
        return (modelFilepath);
    }


    //ui
    public final static int UI_MAX_DISPLAYED_MESSAGES = 4;

    //bluetooth
    public final static String BLUETOOTH_ADAPTER_NAME = "SOCINT";
    public final static String BLUETOOTH_INIT_MESSAGE = "CONNECTED: ";
    public final static int BLUETOOTH_DISCOVERY_WAIT_TIME = 30000;
    public final static String BLUETOOTH_SERVICE_NAME  = "SOCIAL_INTERACTION";
    private final static String BLUETOOTH_UUIDString = "662ab3f4-c79c-11d1-3a37-a500712cf000";
    public final static UUID BLUETOOTH_SERVICE_UUID = UUID.fromString(BLUETOOTH_UUIDString);
    private final static String BLUETOOTH_RECOGNIZED_MACS_FILENAME = "MAC";
    public final static int BLUETOOTH_INPUT_BUFFER_SIZE = 1024;

    public static ArrayList<String> BLUETOOTH_RECOGNIZED_MACS;

    public static String getRecognizedMACsPath(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,Static.TRAINING_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        return (file.getAbsolutePath() + "/" + Static.BLUETOOTH_RECOGNIZED_MACS_FILENAME+".txt");
    }

    //opcodes
    public final static byte OPCODE_DISPLAY_MESSAGE = 0x1;
    public final static byte OPCODE_SAMPLES = 0x2;
    public final static byte OPCODE_SPEECH_VECTOR = 0x3;



    //log
    public static final String LOG_LOGNAME_BUNDLE_KEY = "Logname";
    public static final String LOG_FOLDER = TRAINING_FOLDER+"/log";
    public static final String LOG_FILE_EXT = ".csv";

    public static String getLogOutputPath(String logname){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,Static.LOG_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }
        return (file.getAbsolutePath() + "/" + logname+Static.LOG_FILE_EXT);
    }

    //testing
    public final static int TEST_CLASSIFICATION_BUFFER_SIZE = 6;
    public final static String TEST_FILENAME = "TEMPDATA";
    public final static String TEST_SCALED_FILENAME = "scaled_TEMPDATA";
    public final static String TEST_OUTPUT_FILENAME = "TEMPOUT";
    public final static String TEST_OUTPUT_EXT = ".txt";
    public final static String TEST_FILE_EXT = Static.TRAINING_FILE_EXTENSION;

    public static String getTestOutputPath(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,Static.TRAINING_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }
        return (file.getAbsolutePath() + "/" + Static.TEST_OUTPUT_FILENAME+Static.TEST_OUTPUT_EXT);
    }

    public static String getTestFilepath(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,Static.TRAINING_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }
        return (file.getAbsolutePath() + "/" + Static.TEST_FILENAME+Static.TEST_FILE_EXT);
    }

    public static String getScaledTestFilepath(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,Static.TRAINING_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        String scaledTestFile = file.getAbsolutePath() + "/" +
                Static.TEST_SCALED_FILENAME+Static.TEST_FILE_EXT;
        File scaledFile = new File(scaledTestFile);

        if (!scaledFile.exists()){
            Log.d(Static.DEBUG, "scaled file does not exist.. creating new file");
            try {
                scaledFile.createNewFile();
            } catch (IOException e) {
                Log.d(Static.DEBUG,"Failed creating new scaled file");
            }
        }
        return (scaledTestFile);
    }

    //debug tag
    public final static String DEBUG = "SOCDEB";
}
