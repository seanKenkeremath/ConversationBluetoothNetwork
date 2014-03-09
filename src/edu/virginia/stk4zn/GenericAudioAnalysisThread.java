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
abstract class GenericAudioAnalysisThread extends Thread {



    protected String threadName;
    private boolean waiting;

    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private byte audioData[] = null;
    private WaveHeader header;



    public GenericAudioAnalysisThread(String threadName){
        super(threadName);
        this.threadName = threadName;
        bufferSize = Static.AUDIO_BUFFER_SIZE;
        audioData = new byte[bufferSize];
        header = new WaveHeader(getWaveFileHeader(bufferSize, bufferSize + 36,
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

        onExit();

    }

    public void startRecording(){
        Log.d(Static.DEBUG,"Starting recording for " +threadName+"...");
        recorder.startRecording();
    }

    public void stopRecording(){
        if (recorder.getState()==AudioRecord.STATE_INITIALIZED){
            recorder.stop();
            recorder.release();
        }
    }

    public void cancel() {
        Log.d(Static.DEBUG, "Killing " + threadName);
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
            Log.d(Static.DEBUG,threadName+": Failed analyzing audio");
            return;
        }

        handleSample(lst, storedWave);

    }


    public static byte[] getWaveFileHeader(long totalAudioLen,
                                           long totalDataLen, long longSampleRate, int channels,
                                           long byteRate){

        byte[] header = new byte[44];

        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (channels * Static.AUDIO_RECORDER_BPP / 8);  // block align
        header[33] = 0;
        header[34] = Static.AUDIO_RECORDER_BPP;  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        return header;
    }


    abstract void handleSample(List<WindowFeature> list, Wave wave);

    abstract void onExit();


    //MOHSIN: add extra features using the addFeature method
    public AudioSample createSampleFromAudio(WindowFeature window, Wave wave){
        AudioSample sample = new AudioSample();

        /*
        //adding all 351 mfcc values as features to sample
        for(double[] stats : window.windowFeature){
            for(double value: stats){
                sample.addFeature(value);
            }
        }
        */

        //taking only the mean of mfcc values
        for(double[] stats : window.windowFeature){
                sample.addFeature(stats[0]);
        }


        //add other features using sample.addFeature(Double feature)

        //for instance: double pitchvar = extractPitchVar(wave);
        //for instance: sample.addFeature(pitchvar);

        return sample;
    }

}
