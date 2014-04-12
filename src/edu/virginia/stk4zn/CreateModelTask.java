package edu.virginia.stk4zn;

import android.os.AsyncTask;
import android.util.Log;
import svm.svm_scale;
import svm.svm_train;

import java.io.*;
import java.util.ArrayList;

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
            ArrayList<String> negLines = new ArrayList<String>();
           // while ((line = negIn.readLine())!=null && negLinesCounted < number_pos_samples){
            while ((line = negIn.readLine())!=null){

                    negLinesCounted++;
                negLines.add(line+"\n");
               // writer.append(line + "\n");
            }
            Log.d(Static.DEBUG,"Buffered " + negLinesCounted+" negative samples from file");


            int posLinesCounted = 0;
            ArrayList<String> posLines = new ArrayList<String>();
            while ((line = posIn.readLine())!=null){
                posLinesCounted++;
                posLines.add(line + "\n");
                //writer.append(line + "\n");
            }
            Log.d(Static.DEBUG,"Buffered " + posLinesCounted+" positive samples from file");


            if (negLines.size() == 0 || posLines.size() == 0){
                Log.d(Static.DEBUG,"0 positive or negative samples");
                return false;
            }


            int posLinesWritten  = 0;
            int negLinesWritten = 0;
            //if more positive samples, loop through negative samples to reach max
            if (posLinesCounted > negLinesCounted){

                for (String posLine : posLines){
                    posLinesWritten++;
                    writer.append(posLine);
                }
                for (int i = 0; i < posLinesCounted; i++){
                    int index = i % negLines.size();
                    negLinesWritten++;
                    writer.append(negLines.get(index));
                }
            } else if (negLinesCounted > posLinesCounted){

                for (String negLine : negLines){
                    negLinesWritten++;
                    writer.append(negLine);
                }
                for (int i = 0; i < negLinesCounted; i++){
                    int index = i % posLines.size();
                    posLinesWritten++;
                    writer.append(posLines.get(index));
                }

            } else { //if equal
                for (String negLine : negLines){
                    negLinesWritten++;
                    writer.append(negLine);
                }
                for (String posLine : posLines){
                    posLinesWritten++;
                    writer.append(posLine);
                }
            }

            Log.d(Static.DEBUG,"Wrote " + negLinesWritten+" negative samples to training set");
            Log.d(Static.DEBUG,"Wrote " + posLinesWritten+" positive samples to training set");


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
