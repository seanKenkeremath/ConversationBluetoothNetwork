package svm;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


import audio.Wave;
import audio.feature.MFCCFeatureExtract;
import audio.feature.WindowFeature;


public class SVMDataSetGenerator {
	

	
	public static final String SPACE = " ";
	public static final String NEWLINE = "\n";
	
	
	

	public static StringBuilder generateDataSet(String label, List<WindowFeature> windowFeatureList, DataOutputStream fp) throws IOException{
		
		StringBuilder sb = new StringBuilder();
		for(WindowFeature wf: windowFeatureList){
			sb.append(label+SPACE);
			fp.writeBytes(label+SPACE);
        	
    		int featureIndex = 1;	//start at 1
    		for(double[] stats : wf.windowFeature){	//set of statistics of each feature
    			for(double value: stats){
    				sb.append(featureIndex+":"+(float)value+SPACE);
    				fp.writeBytes(featureIndex+":"+(float)value+SPACE);
    				featureIndex++;
    			}
    		}
    		sb.append(NEWLINE);
    		fp.writeBytes(NEWLINE);
        }
		return sb;
		
	}
	
	public static List<WindowFeature> getWindowFeatureListFromFiles(List<String> audioFiles){
		  List<WindowFeature> windowFeatureList = new ArrayList<WindowFeature>();
			for(String file: audioFiles){
				Wave wave = new Wave(file);
				int Fs = wave.getWaveHeader().getSampleRate();
				double[] inputSignal = wave.getSampleAmplitudes();
				MFCCFeatureExtract mfccFeature = new MFCCFeatureExtract(inputSignal,Fs);
				windowFeatureList.addAll(mfccFeature.getListOfWindowFeature());
			}
		return windowFeatureList;
	}
	
	public static List<String> getListOfFilesInFolder(final File folder) {
		List<String> fileList = new ArrayList<String>();
	    for (final File fileEntry : folder.listFiles()) {
	        if (fileEntry.isDirectory()) {
	        	getListOfFilesInFolder(fileEntry);
	        } else {
	        	fileList.add(fileEntry.getParent()+"/"+fileEntry.getName());
//	            System.out.println(fileEntry.getParent()+"/"+fileEntry.getName());
	        }
	    }
	    return fileList;
	}
	
	public static void main(String[] argv) throws IOException {
		//generate training dataset and save to a text file
		List<String> positiveSamples;
		List<String> negativeSamples;
		
		final String pathToPositiveSamples = "./dataset/positive/";
		final String pathToNegativeSamples = "./dataset/negative/";
		File folder = new File(pathToPositiveSamples);
		positiveSamples = getListOfFilesInFolder(folder);
		folder = new File(pathToNegativeSamples);
		negativeSamples = getListOfFilesInFolder(folder);
		
		
        DataOutputStream fp;
        
        fp = new DataOutputStream(new BufferedOutputStream(new FileOutputStream("training_samples")));
        
        
        //generate training dataset and save to a text file 
        //generate positive dataset
        List<WindowFeature> positiveWindowFeatureList = getWindowFeatureListFromFiles(positiveSamples);
		SVMDataSetGenerator.generateDataSet("+1", positiveWindowFeatureList,fp);
		
		//generate negative dataset
		List<WindowFeature> negativeWindowFeatureList = getWindowFeatureListFromFiles(negativeSamples);
		SVMDataSetGenerator.generateDataSet("-1", negativeWindowFeatureList,fp);
	
		fp.close();

		
	}

}
