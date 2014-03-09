package edu.virginia.stk4zn;

import android.util.Log;
import audio.Wave;
import audio.feature.WindowFeature;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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


        //iterate through all windows
        for (WindowFeature window: list){


            //skip NaN and infinity (when microphone first starts)
            if (window.windowFeature[0][0] == Double.NEGATIVE_INFINITY || window.windowFeature[0][0] == Double.POSITIVE_INFINITY){
                continue;
            }

            samples.add(createSampleFromAudio(window, wave));

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
