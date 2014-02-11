package edu.virginia.stk4zn;

import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;
import audio.Wave;
import audio.WaveHeader;
import audio.feature.MFCCFeatureExtract;
import audio.feature.WindowFeature;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sean on 2/10/14.
 */
public class PositiveTrainingThread extends Thread {


    private static final String AUDIO_RECORDER_FOLDER = "SocInt";
    public static final String TRAINING_FILENAME = "training";


    ArrayList<WindowFeature> samples;


    private boolean waiting;

    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private byte audioData[] = null;
    private WaveHeader header;


    public PositiveTrainingThread(){
        super("Positive Training Thread");
        samples = new ArrayList<WindowFeature>();

        bufferSize = ProcessThread.BUFFER_SIZE;
        Log.d(ConversationActivity.DEBUG, "BufferSize " + bufferSize);
        audioData = new byte[bufferSize];
        header = new WaveHeader(ProcessThread.getWaveFileHeader(bufferSize, bufferSize + 36,
                ProcessThread.RECORDER_SAMPLERATE, ProcessThread.CHANNELS, ProcessThread.BYTE_RATE));
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                ProcessThread.RECORDER_SAMPLERATE, ProcessThread.RECORDER_CHANNELS, ProcessThread.RECORDER_AUDIO_ENCODING,
                bufferSize);
    }

    @Override
    public void run(){

        startRecording();

        waiting = true;

        boolean pastFirstRead = false;

        int numberBytes;
        long t1, t2;

        while(waiting) {
            try{
                numberBytes = recorder.read(audioData, 0, bufferSize);
                if(numberBytes != AudioRecord.ERROR_INVALID_OPERATION && pastFirstRead) {
                    analyzeAudio();
                }

                if (!pastFirstRead){
                    pastFirstRead = true;
                }

            }catch(Exception ex) {
                ex.printStackTrace();
            }
        }

        writeDataToFile();

    }

    public void startRecording(){
        Log.d(ConversationActivity.DEBUG,"Starting recording...");
        recorder.startRecording();
    }

    public void stopRecording(){
        if (recorder.getState()==AudioRecord.STATE_INITIALIZED){
            recorder.stop();
            recorder.release();
        }
    }

    public void cancel() {
        Log.d(ConversationActivity.DEBUG, "Killing Positive Training Thread");
        stopRecording();
        waiting = false;
    }


    private void analyzeAudio(){

        Wave storedWave = new Wave(header, audioData);

        double[] inputSignal = storedWave.getSampleAmplitudes();
        int Fs = storedWave.getWaveHeader().getSampleRate();
        double Tw = ProcessThread.FRAME_DURATION; // analysis frame duration (ms)
        double Ts = ProcessThread.FRAME_SHIFT; // analysis frame shift (ms)
        double Wl = ProcessThread.WINDOW_SIZE; // window duration (second)



        MFCCFeatureExtract mfccFeatures = new MFCCFeatureExtract(inputSignal,
                Tw, Ts, Fs, Wl);


        List<WindowFeature> lst = mfccFeatures.getListOfWindowFeature();

        if (lst.size()==0){
            Log.d(ConversationActivity.DEBUG,"Failed analyzing audio");
            return;
        }

        // if not noise ...
        
        samples.addAll(lst);

        //


    }


    private String getFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }
        return (file.getAbsolutePath() + "/" + TRAINING_FILENAME+".txt");
    }

    private void writeDataToFile(){
        try {
            Log.d(ConversationActivity.DEBUG,"Writing " + samples.size() + " positive training samples to file");

            File file = new File(getFilename());

            if (!file.exists()){
                file.createNewFile();
            }

            FileWriter fp = new FileWriter(file, false);
            for(WindowFeature wf: samples){
                fp.write("+1" + " ");

                int featureIndex = 1;	//start at 1
                for(double[] stats : wf.windowFeature){	//set of statistics of each feature
                    for(double value: stats){
                        fp.write(featureIndex + ":" + (float) value + " ");
                        featureIndex++;
                    }
            }
                fp.write("\n");
            }

            fp.flush();
            fp.close();
        } catch (IOException e) {
            Log.d(ConversationActivity.DEBUG, "Failed writing to training file");
        }

    }
}
