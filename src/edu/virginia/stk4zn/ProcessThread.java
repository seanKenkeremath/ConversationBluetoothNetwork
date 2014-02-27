package edu.virginia.stk4zn;

import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import audio.Wave;
import audio.WaveHeader;
import audio.feature.MFCCFeatureExtract;
import audio.feature.WindowFeature;
import svm.libsvm.svm_model;
import svm.svm_predict;
import svm.svm_scale;

import java.io.*;
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
    private File dataBuffer;

    
    


    private long dt = 0;

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

        Wave storedWave = new Wave(header, audioData);
        double[] inputSignal = storedWave.getSampleAmplitudes();
        int Fs = storedWave.getWaveHeader().getSampleRate();
        double Tw = Static.AUDIO_FRAME_DURATION; // analysis frame duration (ms)
        double Ts = Static.AUDIO_FRAME_SHIFT; // analysis frame shift (ms)
        double Wl = Static.AUDIO_WINDOW_SIZE; // window duration (second)


        MFCCFeatureExtract mfccFeatures = new MFCCFeatureExtract(inputSignal,
                Tw, Ts, Fs, Wl);

        //351 mfcc features
        List<WindowFeature> lst = mfccFeatures.getListOfWindowFeature();
        /*
        //if not noise
        if (lst.get(0).windowFeature[0][0] < 65){
            //display results in UI thread
            act.getHandler().post(new Runnable(){

                @Override
                public void run() {
                    act.displayMFCC("Silence");
                }
            });

            return;
        }

        */

        if (lst.size()==0){
            Log.d(Static.DEBUG,"Failed analyzing audio");
            return;
        }

        //write mfcc features to temp file in format accepted by svm_scale
        try {
            dataBuffer = new File(Static.getTestFilepath());
            WriteDataToFile(lst, dataBuffer);
        } catch (IOException e) {
            Log.d(Static.DEBUG,"Failed writing data to buffer");
        }

        /*
        //scale mfcc features from temp file and save them in another temp file for svm_predict to read
        try {
            svm_scale scaler = new svm_scale();
            String[] args = {Static.getTestFilepath()};
            scaler.run(args, Static.getScaledTestFilepath());
        } catch (IOException e) {
            Log.d(Static.DEBUG,"Failed scaling data buffer");
        }

         */


        // run svm_predict on scaled temp file and output results into a final temp file
        try {
            //String[] args = {Static.getScaledTestFilepath(), Static.getModelFilepath(), Static.getTestOutputPath()};
            String[] args = {Static.getTestFilepath(), Static.getModelFilepath(), Static.getTestOutputPath()};

            svm_predict.main(args);
        } catch (FileNotFoundException e){
            Log.d(Static.DEBUG,"predict files not found");
        } catch (IOException e) {
            Log.d(Static.DEBUG, "failed classification");
        }

        //parse message from temp file (could be a much more efficient process)
        try {
            StringBuilder dispMessage = new StringBuilder();
            String nextLine;
            //BufferedReader read = new BufferedReader(new FileReader(outputFile));
            BufferedReader read = new BufferedReader(new FileReader(Static.getTestOutputPath()));
            while ((nextLine = read.readLine()) != null){
                dispMessage.append(nextLine + "\n");
            }

            final String finalDisp = dispMessage.toString();

            //display results in UI thread
            act.getHandler().post(new Runnable(){

                @Override
                public void run() {
                    act.displayMFCC(finalDisp);
                }
            });
        } catch (Exception e) {
            Log.d(Static.DEBUG,"Failed displaying classification data");
        }

    }

    private void WriteDataToFile(List<WindowFeature> samples, File file) throws IOException{

        if (!file.exists()){
            file.createNewFile();
        }

        FileWriter fp = new FileWriter(file, false);
        for(WindowFeature wf: samples){

            if (wf.windowFeature[0][0] == Double.NEGATIVE_INFINITY || wf.windowFeature[0][0] == Double.POSITIVE_INFINITY){
                continue;
            }
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
    }



    public void cancel() {
        Log.d(Static.DEBUG, "Killing Process Thread");
        stopRecording();
        waiting = false;
    }

}
