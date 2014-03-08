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
public class TrainingThread extends GenericAudioAnalysisThread {




    private TrainingActivity act;
    ArrayList<AudioSample> samples;
    private boolean positive;



    public TrainingThread(TrainingActivity activity, boolean positive){
        super("Training Thread");
        this.act = activity;
        this.samples = new ArrayList<AudioSample>();
        this.positive = positive;
    }

    @Override
    void handleSample(List<WindowFeature> list, Wave wave) {



        //calculate pitchvar, tonality, etc using wave object
        //reuse values for each window
        //for instance Double pitchVar = extractPitchVar(wave);


        //iterate through all windows
        for (WindowFeature window: list){


            //skip NaN and infinity (when microphone first starts)
            if (window.windowFeature[0][0] == Double.NEGATIVE_INFINITY || window.windowFeature[0][0] == Double.POSITIVE_INFINITY){
                continue;
            }

            //create blank sample object for each window and add features
            AudioSample sample = new AudioSample();

            //add all 351 mfcc values as features to sample
            for(double[] stats : window.windowFeature){
                for(double value: stats){
                    sample.addFeature(value);
                }
            }

            //add other features using sample.addFeature(Double feature)
            //for instance: sample.addFeature(pitchVar);


            samples.add(sample);

        }


    }

    @Override
    void onExit() {
        writeDataToFile();
    }


    private void writeDataToFile(){
        try {
            String training_type = "negative";
            String file_path = Static.getNegativeTrainingFilepath();
            String sample_label = "-1";
            if (positive){
                training_type = "positive";
                file_path = Static.getPositiveTrainingFilepath();
                sample_label = "+1";
            }

            int numSamplesWritten = 0;

            File file = new File(file_path);

            if (!file.exists()){
                file.createNewFile();
            }

            FileWriter fp = new FileWriter(file, false);


            for(AudioSample sample: samples){

                fp.write(sample_label + " ");

                for (int i = 0; i < sample.getFeatures().size(); i++){
                    int featureNum = i+1;
                    fp.write(featureNum + ":" +sample.getFeatures().get(i) + " ");
                }

                fp.write("\n");
                numSamplesWritten++;
            }

            fp.flush();
            fp.close();


            Log.d(Static.DEBUG,"Wrote " + numSamplesWritten + " " + training_type+" training samples to file");


            //create model if positive training
            final int fNumSamplesWritten = numSamplesWritten;
            if (positive){
            act.getHandler().post(new Runnable(){
                @Override
                public void run() {
                    CreateModelTask task = new CreateModelTask(act);
                    task.execute(fNumSamplesWritten);
                }

            });
            }


        } catch (IOException e) {
            Log.d(Static.DEBUG, "Failed writing to training file");
        }

    }
}
