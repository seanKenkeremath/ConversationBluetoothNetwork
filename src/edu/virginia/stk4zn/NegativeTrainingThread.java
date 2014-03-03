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
public class NegativeTrainingThread extends Thread {




    private TrainingActivity act;
    ArrayList<WindowFeature> samples;


    private boolean waiting;

    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private byte audioData[] = null;
    private WaveHeader header;


    public NegativeTrainingThread(TrainingActivity activity){
        super("Positive Training Thread");
        this.act = activity;
        samples = new ArrayList<WindowFeature>();

        bufferSize = Static.AUDIO_BUFFER_SIZE;
        Log.d(Static.DEBUG, "BufferSize " + bufferSize);
        audioData = new byte[bufferSize];
        header = new WaveHeader(ProcessThread.getWaveFileHeader(bufferSize, bufferSize + 36,
                Static.AUDIO_RECORDER_SAMPLERATE, Static.AUDIO_CHANNELS, Static.AUDIO_BYTE_RATE));
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                Static.AUDIO_RECORDER_SAMPLERATE, Static.AUDIO_RECORDER_CHANNELS, Static.AUDIO_RECORDER_AUDIO_ENCODING,
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
        Log.d(Static.DEBUG,"Starting recording...");
        recorder.startRecording();
    }

    public void stopRecording(){
        if (recorder.getState()==AudioRecord.STATE_INITIALIZED){
            recorder.stop();
            recorder.release();
        }
    }

    public void cancel() {
        Log.d(Static.DEBUG, "Killing Negative Training Thread");
        stopRecording();
        waiting = false;
    }


    private void analyzeAudio(){

        Wave storedWave = new Wave(header, audioData);

        double[] inputSignal = storedWave.getSampleAmplitudes();
        int Fs = storedWave.getWaveHeader().getSampleRate();
        double Tw = Static.AUDIO_FRAME_DURATION; // analysis frame duration (ms)
        double Ts = Static.AUDIO_FRAME_SHIFT; // analysis frame shift (ms)
        double Wl = Static.AUDIO_WINDOW_SIZE; // window duration (second)



        MFCCFeatureExtract mfccFeatures = new MFCCFeatureExtract(inputSignal,
                Tw, Ts, Fs, Wl);


        List<WindowFeature> lst = mfccFeatures.getListOfWindowFeature();

        if (lst.size()==0){
            Log.d(Static.DEBUG,"Failed analyzing audio");
            return;
        }


        /*
        // if not noise ...
        if (lst.get(0).windowFeature[0][0] < 65){
            return;
        }
        */

        samples.addAll(lst);

        //


    }




    private void writeDataToFile(){
        try {
            Log.d(Static.DEBUG,"Writing " + samples.size() + " negative training samples to file");

            File file = new File(Static.getNegativeTrainingFilepath());

            if (!file.exists()){
                file.createNewFile();
            }

            FileWriter fp = new FileWriter(file, false);
            for(WindowFeature wf: samples){

                if (wf.windowFeature[0][0] == Double.NEGATIVE_INFINITY || wf.windowFeature[0][0] == Double.POSITIVE_INFINITY){
                    continue;
                }
                fp.write("-1" + " ");

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
            Log.d(Static.DEBUG, "Failed writing to negative training file");
        }

    }
}