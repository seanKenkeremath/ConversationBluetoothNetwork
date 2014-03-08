package edu.virginia.stk4zn;

import android.util.Log;
import audio.Wave;
import audio.feature.WindowFeature;
import svm.svm_predict;

import java.io.*;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

/**
 * Created by sean on 1/24/14.
 */
public class ProcessThread extends GenericAudioAnalysisThread {


    private ConversationActivity act;
    private float noiseThreshold;
    private File dataBuffer;
    private String logName;
    private Queue<Double> classificationBuffer;
    

    public ProcessThread(ConversationActivity act, String logName, int noiseThreshold) { //pass null for logName to skip logging
        super("Analyze Audio Thread");
        this.logName = logName;
        this.act = act;
        this.noiseThreshold = noiseThreshold;
        this.classificationBuffer = new ArrayDeque<Double>();
    }


    @Override
    void handleSample(List<WindowFeature> lst, Wave wave) {

        boolean silence = false;


        //if not noise
        if (lst.get(0).windowFeature[0][0] < noiseThreshold){
            //display results in UI thread
            silence = true;
            act.getHandler().post(new Runnable(){

                @Override
                public void run() {
                    act.displayMFCC("Silence");
                }
            });

        }

        boolean speaking = false;
        StringBuilder dispMessage = new StringBuilder();
        dispMessage.append("MFCC 0: " + lst.get(0).windowFeature[0][0]+"\n");

        if (!silence){
            createInput(lst);
            makePrediction();
            try {
                speaking = getTestResult(dispMessage);
            } catch (IOException e) {
                Log.d(Static.DEBUG,"Failed getting prediction result");
            }

        }

        StringBuilder logMessage = new StringBuilder();

        logMessage.append(System.currentTimeMillis()+",");

        if (speaking){
            dispMessage.append("Speaking\n");
            logMessage.append(1 + ",");
        } else{
            dispMessage.append("Not Speaking\n");
            logMessage.append(0 + ",");
        }

        if (act.speaking_truth){
            logMessage.append(1 + "\n");
        } else {
            logMessage.append(0 + "\n");
        }
        final String finalDisp = dispMessage.toString();

        //display results in UI thread
        act.getHandler().post(new Runnable(){

            @Override
            public void run() {
                act.displayMFCC(finalDisp);
            }
        });

        try {
            logData(logMessage.toString(), new File(Static.getLogOutputPath(this.logName)));
        } catch (IOException e) {
            Log.d(Static.DEBUG,"Failed logging data");
        }
    }

    @Override
    void onExit() {
        //do nothing
    }

    private void bufferClassification(Double classification){
        int size = classificationBuffer.size();
        if (size < Static.TEST_CLASSIFICATION_BUFFER_SIZE){
            classificationBuffer.add(classification);
        } else {
            classificationBuffer.poll();
            classificationBuffer.add(classification);
        }
    }


    private void createInput(List<WindowFeature> lst){
        //write mfcc features to temp file in format accepted by svm_scale
        try {
            dataBuffer = new File(Static.getTestFilepath());
            writeInputToFile(lst, dataBuffer);
        } catch (IOException e) {
            Log.d(Static.DEBUG,"Failed writing data to buffer");
        }

    }

    private void makePrediction(){
        // run svm_predict and output results into a final temp file
        try {
            String[] args = {Static.getTestFilepath(), Static.getModelFilepath(), Static.getTestOutputPath()};

            svm_predict.main(args);
        } catch (FileNotFoundException e){
            Log.d(Static.DEBUG,"predict files not found");
        } catch (IOException e) {
            Log.d(Static.DEBUG, "failed classification");
        }
    }

    private boolean getTestResult(StringBuilder dispMessage) throws IOException {
        //parse message from temp file (could be a much more efficient process)
        boolean speaking = true; //set to false if any classification is negative

            String nextLine;
            BufferedReader read = new BufferedReader(new FileReader(Static.getTestOutputPath()));
            while ((nextLine = read.readLine()) != null){
                dispMessage.append(nextLine + "\n");
                bufferClassification(Double.parseDouble(nextLine));
            }

            //if any classifications are negative user is not speaking
            for (Double result: classificationBuffer){
                dispMessage.append(result+"\n");
                if (result < 0){
                    speaking = false;
                }
            }

        return speaking;

    }
    private void logData(String logLine, File file) throws IOException{
        if (!file.exists()){
            file.createNewFile();
        }
        FileWriter fp = new FileWriter(file, true);
        fp.write(logLine);

        fp.close();
    }

    private void writeInputToFile(List<WindowFeature> samples, File file) throws IOException{

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

}
