package edu.virginia.stk4zn;

import android.os.AsyncTask;
import android.util.Log;

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
        String positiveTrainingFilepath = params[0];

        File posFile = new File(positiveTrainingFilepath);

        try {
            FileWriter writer = new FileWriter(posFile, true);
            BufferedReader negIn = new BufferedReader(new InputStreamReader(act.getResources().openRawResource(R.raw.neg_samp)));
            String line;
            while ((line = negIn.readLine())!=null){
                writer.append(line + "\n");
            }
            negIn.close();
            writer.close();
        } catch (IOException e) {
            Log.d(Static.DEBUG, "Failed opening posFile");
        }


        return true;
    }
}
