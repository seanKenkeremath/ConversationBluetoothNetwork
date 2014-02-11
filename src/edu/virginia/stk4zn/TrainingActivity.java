package edu.virginia.stk4zn;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

/**
 * Created by sean on 2/10/14.
 */
public class TrainingActivity extends Activity {

    private PositiveTrainingThread trainThread;
    private Button startTraining;
    private Button stopTraining;
    private Button toConversation;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.training);


        init();
    }

    private void init(){

        startTraining = (Button) findViewById(R.id.training_train);
        stopTraining = (Button) findViewById(R.id.training_stop);
        toConversation = (Button) findViewById(R.id.training_toConversation);


        startTraining.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                v.setEnabled(false);
                stopTraining.setEnabled(true);
                toConversation.setEnabled(false);
                startTraining();
            }
        });

        stopTraining.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                v.setEnabled(false);
                startTraining.setEnabled(true);
                toConversation.setEnabled(true);
                stopTraining();
            }
        });

        toConversation.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                Intent convIntent = new Intent(TrainingActivity.this,ConversationActivity.class);
                TrainingActivity.this.startActivity(convIntent);
                TrainingActivity.this.finish();
            }
        });

    }

    private void startTraining(){
        if (trainThread!=null){
            trainThread.cancel();
            try {
                Log.d(ConversationActivity.DEBUG, "joining previous Positive Training Thread");
                trainThread.join();
            } catch (InterruptedException e) {
                Log.d(ConversationActivity.DEBUG, "Interrupted joining Positive Training Thread");

            }
        }
        trainThread = new PositiveTrainingThread();
        trainThread.start();
    }

    private void stopTraining(){
        if (trainThread!=null){
            trainThread.cancel();
        }

    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        Log.d(ConversationActivity.DEBUG, "Training ON DESTROY");
        stopTraining();
    }

}
