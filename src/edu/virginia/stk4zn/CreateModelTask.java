package edu.virginia.stk4zn;

import android.os.AsyncTask;
import android.util.Log;
import svm.svm_scale;
import svm.svm_train;

import java.io.*;

/**
 * Created by sean on 2/17/14.
 */
public class CreateModelTask extends AsyncTask<Integer, Integer, Boolean> {

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
    protected Boolean doInBackground(Integer... params) {
        int number_pos_samples = params[0];

        //append negative mfcc values to positive trained
        try {
            File posFile = new File(Static.getPositiveTrainingFilepath());
            File negFile = new File(Static.getNegativeTrainingFilepath());
            File trainFile = new File(Static.getTrainingFilepath());
            Log.d(Static.DEBUG, "Combining positive and negative samples");
            FileWriter writer = new FileWriter(trainFile, false);
            //BufferedReader negIn = new BufferedReader(new InputStreamReader(act.getResources().openRawResource(R.raw.neg_samp)));
            BufferedReader negIn = new BufferedReader(new FileReader(negFile));
            BufferedReader posIn = new BufferedReader(new FileReader(posFile));
            String line;

            int negLinesCounted = 0;
            while ((line = negIn.readLine())!=null && negLinesCounted < number_pos_samples){
                negLinesCounted++;
                writer.append(line + "\n");
            }
            Log.d(Static.DEBUG,"Wrote " + negLinesCounted+" negative samples to training set");


            int posLinesCounted = 0;
            while ((line = posIn.readLine())!=null && posLinesCounted < negLinesCounted){
                posLinesCounted++;
                writer.append(line + "\n");
            }
            Log.d(Static.DEBUG,"Wrote " + posLinesCounted+" positive samples to training set");


            negIn.close();
            posIn.close();
            writer.close();

        } catch (IOException e) {
            Log.d(Static.DEBUG, "Failed creating training file");
        }




        try {
            svm_train trainer = new svm_train();
            Log.d(Static.DEBUG, "Running svm_train to create model file");
            String[] args = {Static.getTrainingFilepath(), Static.getModelFilepath()};


            trainer.run(args);
        } catch (IOException e) {
            Log.d(Static.DEBUG, "Failed creating model file");
        }

        Log.d(Static.DEBUG, "Model file created");
        return true;
    }
}
