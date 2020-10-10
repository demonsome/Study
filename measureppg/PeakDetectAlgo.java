package com.example.luolab.measureppg;

import java.util.Arrays;

public class PeakDetectAlgo {

    public static double[][] peakDetect(double[] data, double sample_rate){
        double[] Peaks = new double[2000];
        double[] Locs = new double[2000];
        int interval;
        interval = (int)(sample_rate*1.2);
        double alpha = 0.84;
        int Nr;
        Nr = (int)(0.15*sample_rate)+1;
        double temp_alpha = alpha;

        for(int i=0; i<4; i++) {
            double[] window = Arrays.copyOfRange(data, i*interval, (i+1)*(interval));
            double[] max_info = maxValue(window);
            Peaks[i] = max_info[0];
            Locs[i] = max_info[1];
        }
        double last_loc = Locs[3];
        int index = 4;
        double[] Thres = new double[data.length];
        //Find peak amplitude and location
        for(int i=4*interval; i<(data.length-1); i++) {
            if(i-last_loc > (int)1.5*sample_rate) {
                alpha = 0.7;
            }else {
                alpha = temp_alpha;
            }
            double[] last_4_Locs = Arrays.copyOfRange(Locs, index-4, index);
            double[] last_4_Peaks = Arrays.copyOfRange(Peaks, index-4, index);
            Thres[i] = defineThres(last_4_Locs, last_4_Peaks, i, Nr, alpha);
            if(data[i] >= Thres[i] && data[i] >= data[i-1] && data[i] > data[i+1]) {
                Peaks[index] = data[i];
                Locs[index] = i;
                last_loc = i;
                index = index + 1;
            }
        }
        double [][] result = {Peaks,Locs};
        return result;
    }//tested

    public static double calculateMean(double numArray[]){
        double sum = 0.0;
        int length = numArray.length;
        for(double num : numArray) {
            sum += num;
        }
        double mean = sum/length;
        return mean;
    } //tested

    public static double calculateStdev(double numArray[]){
        double sum = 0.0, standardDeviation = 0.0;
        int length = numArray.length;
        double mean = calculateMean(numArray);
        for(double num: numArray) {
            standardDeviation += Math.pow(num - mean, 2);
        }
        return Math.sqrt(standardDeviation/(length));
    } //tested



    static double[] maxValue(double[] arr) {
        double max = 0;
        double index = 0;
        for (int ktr = 0; ktr < arr.length; ktr++) {
            if (arr[ktr] > max) {
                max = arr[ktr];
                index = ktr;
            }
        }
        double[] result = {max, index};
        return result;

    } //tested

    static double defineThres(double[] locs, double[] pks, int presentIndex, double Nr, double alpha) {
        double[] diffArr = findDiff(locs);
        double timePeriod = median(diffArr);
        double thres;
        double lastPkLocs = locs[locs.length - 1];
        double lastPk = pks[pks.length - 1];
        int Indxdiff = presentIndex - (int)lastPkLocs;
        if(Indxdiff < Nr) {
            thres = lastPk;
        }else if(Nr <= Indxdiff && Indxdiff <= timePeriod*1.2){
            thres = lastPk + ((alpha-1)*lastPk*(Indxdiff-Nr)/(timePeriod-Nr));
        } else {
            thres = alpha * lastPk;
        }
        return thres;
    } //tested

    static double[] findDiff(double[] Arr) {
        double diff;
        double[] tempArr = new double[Arr.length-1];
        for(int i = 0; i < Arr.length - 1; i++ ){
            diff = (Arr[i+1] - Arr[i]);
            tempArr[i] = diff;
        }
        return tempArr;
    } //tested

    static double[] sampleToTime(double[] Arr, double sampleRate) {
        double[] tempArr = new double[Arr.length];
        for(int i = 0; i < Arr.length ; i++ ){
            tempArr[i] = Arr[i]*1000/sampleRate;
        }
        return tempArr;
    } //tested


    public static double median(double[] arr) {
        Arrays.sort(arr);
        int middle = (int)(arr.length/2);
        if (arr.length%2 == 1) {
            return arr[middle];
        } else {
            return (arr[middle-1] +arr[middle]) / 2.0;
        }
    } //tested

    public static double[] pruneZero(double[] arr) {
        for(int i=0; i<arr.length-1; i++) {
            if(arr[i]!=0 && arr[i+1]==0) {
                double[] nonZero = Arrays.copyOfRange(arr, 0, i+1);
                return nonZero;
            }

        }
        return arr;
    } //tested


    public static double[] removeDC(double[] arr) {
        double meanAmp = calculateMean(arr);
        double[] arrMax = maxValue(arr);
        for(int i=0; i<arr.length; i++) {
            double temp = arr[i];
            temp = Math.abs(temp - meanAmp);
            arr[i] = temp/arrMax[0]+temp;
        }
        return arr;
    } //tested
}
