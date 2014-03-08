package edu.virginia.stk4zn;

import java.util.ArrayList;

/**
 * Created by sean on 3/8/14.
 */
public class AudioSample {

    private ArrayList<Double> features;


    public AudioSample(){
        this.features = new ArrayList<Double>();
    }

    public ArrayList<Double> getFeatures(){
        return features;
    }

    public void addFeature(Double feature){
        features.add(feature);
    }

}
