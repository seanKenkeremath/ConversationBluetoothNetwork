package edu.virginia.stk4zn;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by sean on 2/10/14.
 */
public class TrainingActivity extends Activity {

    private TrainingThread posTrainThread;
    private TrainingThread negTrainThread;
    private Button posTraining;
    private boolean isPosTraining;
    private Button negTraining;
    private boolean isNegTraining;
    private Button stopTraining;
    private Button clearAllTraining;
    private Button toConversation;
    private EditText logNameField;
    private EditText thresholdField;
    private Handler handler;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.training);
        handler = new Handler();
        init();
    }

    public void enableTraining(){
        stopTraining.setEnabled(false);
        posTraining.setEnabled(true);
        negTraining.setEnabled(true);
        toConversation.setEnabled(true);
    }

    public void disableTraining(boolean stopEnabled){
        posTraining.setEnabled(false);
        negTraining.setEnabled(false);
        stopTraining.setEnabled(stopEnabled);
        toConversation.setEnabled(false);
    }
    private void init(){

        isPosTraining = false;
        isNegTraining =false;

        posTraining = (Button) findViewById(R.id.training_pos_train);
        negTraining = (Button) findViewById(R.id.training_neg_train);
        stopTraining = (Button) findViewById(R.id.training_stop);
        toConversation = (Button) findViewById(R.id.training_toConversation);
        logNameField = (EditText) findViewById(R.id.training_log_name);
        thresholdField = (EditText) findViewById(R.id.training_threshold);
        clearAllTraining = (Button) findViewById(R.id.training_clear_all);
        posTraining.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                disableTraining(true);
                startPositiveTraining();
            }
        });

        negTraining.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                disableTraining(true);
                startNegativeTraining();
            }
        });

        stopTraining.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                enableTraining();
                stopTraining();
            }
        });

        clearAllTraining.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                deleteTrainingFiles();
            }
        });

        toConversation.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Intent convIntent = new Intent(TrainingActivity.this,ConversationActivity.class);
                String logName = logNameField.getText().toString();
                if (logName==""){
                    logName = "log";
                }
                int threshold = 0;
                if (thresholdField.getText().toString()!=null && !thresholdField.getText().toString().equals("")){
                    threshold = Integer.parseInt(thresholdField.getText().toString());
                }
                convIntent.putExtra(Static.LOG_LOGNAME_BUNDLE_KEY,logName);
                convIntent.putExtra(Static.AUDIO_NOISE_THRESHOLD_BUNDLE_NAME, threshold);
                TrainingActivity.this.startActivity(convIntent);
                TrainingActivity.this.finish();
            }
        });
        enableTraining();

    }

    private void startPositiveTraining(){
        isPosTraining = true;
        if (posTrainThread!=null){
            posTrainThread.cancel();
            try {
                Log.d(Static.DEBUG, "joining previous Positive Training Thread");
                posTrainThread.join();
            } catch (InterruptedException e) {
                Log.d(Static.DEBUG, "Interrupted joining Positive Training Thread");

            }
        }
        posTrainThread = new TrainingThread(this, true);
        posTrainThread.start();
    }

    private void startNegativeTraining(){
        isNegTraining = true;
        if (negTrainThread!=null){
            negTrainThread.cancel();
            try {
                Log.d(Static.DEBUG, "joining previous Negative Training Thread");
                negTrainThread.join();
            } catch (InterruptedException e) {
                Log.d(Static.DEBUG, "Interrupted joining Negative Training Thread");

            }
        }
        negTrainThread = new TrainingThread(this, false);
        negTrainThread.start();
    }

    private void stopTraining(){
        if (isPosTraining){
            disableTraining(false);
            if (posTrainThread!=null){
                posTrainThread.cancel();
            }
            isPosTraining = false;
        }
        else if (isNegTraining){
            if (negTrainThread!=null){
                negTrainThread.cancel();
            }
            isNegTraining = false;
        }


    }

    private void deleteTrainingFiles(){
        File posTraining = new File(Static.getPositiveTrainingFilepath());
        posTraining.delete();

        File negTraining = new File(Static.getNegativeTrainingFilepath());
        negTraining.delete();

    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        Log.d(Static.DEBUG, "Training ON DESTROY");
        stopTraining();
    }

    public Handler getHandler(){
        return handler;
    }



}
