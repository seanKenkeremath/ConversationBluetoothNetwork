package edu.virginia.stk4zn;

import android.os.AsyncTask;
import android.util.Log;
import svm.svm_scale;
import svm.svm_train;

import java.io.*;

/**
 * Created by sean on 2/17/14.
 */
public class CreateModelTask extends AsyncTask<String, Integer, Boolean> {

    private TrainingActivity act;

    public CreateModelTask(TrainingActivity activity){
        super();
        this.act = activity;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (result){
            act.enableTraining();
        }
    }

    @Override
    protected void onPreExecute() {
        act.disableTraining(false);
    }

    @Override
    protected Boolean doInBackground(String... params) {
        String trainingFilepath = params[0];


        //append negative mfcc values to positive trained
        try {
            File posFile = new File(trainingFilepath);
            File negFile = new File(Static.getNegativeTrainingFilepath());
            Log.d(Static.DEBUG, "Combining positive and negative samples");
            FileWriter writer = new FileWriter(posFile, true);
            //BufferedReader negIn = new BufferedReader(new InputStreamReader(act.getResources().openRawResource(R.raw.neg_samp)));
            BufferedReader negIn = new BufferedReader(new FileReader(negFile));
            String line;
            while ((line = negIn.readLine())!=null){
                writer.append(line + "\n");
            }
            negIn.close();
            writer.close();

        } catch (IOException e) {
            Log.d(Static.DEBUG, "Failed creating training file");
        }




        /*
        //scale appended dataset into new file
        try {
            svm_scale scaler = new svm_scale();
            Log.d(Static.DEBUG,"running svm_scale");
            String[] args = {Static.getTrainingFilepath()};
            scaler.run(args, Static.getScaledTrainingFilepath());
        } catch (IOException e) {
            Log.d(Static.DEBUG,"Failed scaling training file");
        }
               */

        //train on scaled data
        try {
            svm_train trainer = new svm_train();
            Log.d(Static.DEBUG, "Running svm_train to create model file");
            //String[] args = {Static.getScaledTrainingFilepath(), Static.getModelFilepath()};
            String[] args = {Static.getTrainingFilepath(), Static.getModelFilepath()};


            trainer.run(args);
        } catch (IOException e) {
            Log.d(Static.DEBUG, "Failed creating model file");
        }

        Log.d(Static.DEBUG, "Model file created");
        return true;
    }
}
