package edu.virginia.stk4zn;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import audio.Wave;
import audio.WaveHeader;
import audio.feature.MFCCFeatureExtract;
import audio.feature.Statistics;
import audio.feature.WindowFeature;

import java.util.List;

/**
 * Created by sean on 1/24/14.
 */
public class ProcessThread extends Thread {


    private ConversationActivity act;
    private boolean waiting;

    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private byte audioData[] = null;
    private WaveHeader header;
    
    


    private long dt = 0;

    private String displayCoeffs;

    public ProcessThread(ConversationActivity act) {
        super("Analyze Audio Thread");
        this.act = act;
        //bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
        bufferSize = Static.AUDIO_BUFFER_SIZE;
        Log.d(Static.DEBUG, "BufferSize " + bufferSize);
        audioData = new byte[bufferSize];
        header = new WaveHeader(getWaveFileHeader(bufferSize, bufferSize+36,
                Static.AUDIO_RECORDER_SAMPLERATE, Static.AUDIO_CHANNELS, Static.AUDIO_BYTE_RATE));
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                Static.AUDIO_RECORDER_SAMPLERATE, Static.AUDIO_RECORDER_CHANNELS, Static.AUDIO_RECORDER_AUDIO_ENCODING,
                bufferSize);
    }

    @Override
    public void run(){

        startRecording();

        waiting = true;

        int numberBytes;
        long t1, t2;

            while(waiting) {
                try{
                    t1 = System.currentTimeMillis();
                numberBytes = recorder.read(audioData, 0, bufferSize);
                if(numberBytes != AudioRecord.ERROR_INVALID_OPERATION) {
                    analyzeAudio();
                }
                t2 = System.currentTimeMillis();
                dt = t2 - t1;

            }catch(Exception ex) {
                    ex.printStackTrace();
                }
        }


    }



    public void startRecording(){
        Log.d(Static.DEBUG,"Starting recording...");
        recorder.startRecording();
    }

    public void stopRecording(){
        recorder.stop();
        recorder.release();
        //recorder = null;
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

    private void analyzeAudio(){

        //Log.d(ConversationActivity.DEBUG,"Analyzing..");
        Wave storedWave = new Wave(header, audioData);
        //Log.d(ConversationActivity.DEBUG, "Sample Wave Length: " + storedWave.length());
        double[] inputSignal = storedWave.getSampleAmplitudes();
        int Fs = storedWave.getWaveHeader().getSampleRate();
        double Tw = Static.AUDIO_FRAME_DURATION; // analysis frame duration (ms)
        double Ts = Static.AUDIO_FRAME_SHIFT; // analysis frame shift (ms)
        double Wl = Static.AUDIO_WINDOW_SIZE; // window duration (second)
        /*
        Log.d(ConversationActivity.DEBUG,"Signal Length: " + inputSignal.length + " FS: " + Fs);
        Log.d(ConversationActivity.DEBUG,"Byte Rate: " + storedWave.getWaveHeader().getByteRate());
        Log.d(ConversationActivity.DEBUG,"Bits Per Sample: " + storedWave.getWaveHeader().getBitsPerSample());
        */


        MFCCFeatureExtract mfccFeatures = new MFCCFeatureExtract(inputSignal,
                Tw, Ts, Fs, Wl);

        /*
		 Log.d(ConversationActivity.DEBUG,"12 MFCCs of each frame:");
		 Log.d(ConversationActivity.DEBUG,mfccFeatures.toString()); //12 MFCC for each frame
		 */


        List<WindowFeature> lst = mfccFeatures.getListOfWindowFeature();
		/*
		 * Log.d(DEBUG_TAG,"List of Window Features:"); for(WindowFeature
		 * wf:lst){ Log.d(DEBUG_TAG,wf.toString()); }
		 */

        if (lst.size()==0){
            Log.d(Static.DEBUG,"Failed analyzing audio");
            return;
        }


        /*
        double[] coeffs = new double[39];
        displayCoeffs = "";
        for (int i = 0; i < 39; i++) {
            double[] allWindows = new double[lst.size()];// do not count
            // first window
            for (int j = 0; j < lst.size(); j++) {

                allWindows[j] = lst.get(j).windowFeature[i][0];

				//Log.d(ConversationActivity.DEBUG_TAG, "mean windowFeature " + i + ": "
				//		+ lst.get(j).windowFeature[i][0]);

            }
            coeffs[i] = Statistics.mean(allWindows);
            displayCoeffs += "mean feature " + i + ": "
                    + coeffs[i] + "\n";
            //Log.d(ConversationActivity.DEBUG, message);
            //act.sendMessageToAll(message);
        }

        act.getHandler().post(new Runnable() {


            @Override
            public void run() {
                act.displayMFCC(displayCoeffs);
            }
        });
        */




    }



    public void cancel() {
        Log.d(Static.DEBUG, "Killing Process Thread");
        stopRecording();
        waiting = false;
    }

}
