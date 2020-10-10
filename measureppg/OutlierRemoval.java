package com.example.luolab.measureppg;

import java.util.Arrays;

import static com.example.luolab.measureppg.PeakDetectAlgo.calculateMean;
import static com.example.luolab.measureppg.PeakDetectAlgo.calculateStdev;

public class OutlierRemoval {

    public static double[] calculateZscore(double numArray[]){
        double std = calculateStdev(numArray);
        double mean = calculateMean(numArray);
        double z[] = new double[numArray.length];
        for(int i=0; i<numArray.length; i++) {
            z[i] = (numArray[i]-mean)/std;
        }
        return z;
    } //tested

    public static double[][] excludeZScoreAbove(double numArray[][], double boundary){
        double interval[] = new double[numArray[0].length];
        double zScore[] = new double[numArray[0].length];
        Arrays.fill(interval, 0);
        Arrays.fill(zScore, 0);
        int index=0;
        for(int i=0; i<numArray[0].length; i++) {
            if(numArray[1][i] <= boundary) {
                interval[index] = numArray[0][i];
                zScore[index] = numArray[1][i];
                index = index+1;
            }
        }
        double result[][] = {interval,zScore};
        return result;
    } //tested
}
