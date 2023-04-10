package com.particlesdevs.photoncamera.processing.opengl.postpipeline.dngprocessor;

import static com.particlesdevs.photoncamera.util.Math2.mix;

import android.graphics.Bitmap;

import com.particlesdevs.photoncamera.processing.rs.HistogramRs;

public class Histogram {
    private static final int HIST_BINS = 256;


    public final float[] sigma = new float[3];
    public float[] hist;
    public final float[] histr;
    public final float[] histInvr;
    public final float[] histg;
    public final float[] histb;
    public final int[] histIn;
    public final int[] histInr;
    public final int[] histIng;
    public final int[] histInb;


    public final float logAvgLuminance;
    public final int histSize;
    private static float getInterpolated(float[] in, float ind){
        int indi = (int)ind;
        if(ind > indi){
            return mix(in[indi],in[Math.min(indi+1,in.length-1)],ind-indi);
        } else if(ind < indi){
            return mix(in[indi],in[Math.max(indi-1,0)],indi-ind);
        } else return in[indi];
    }
    public Histogram(Bitmap bmp, int whPixels,int histSize) {
        this.histSize = histSize;

        int[][] histin = HistogramRs.getHistogram(bmp);
        histIn = histin[3];
        histInr = histin[2];
        histIng = histin[1];
        histInb = histin[0];
        final double[] logTotalLuminance = {0d};
        logAvgLuminance = (float) Math.exp(logTotalLuminance[0] * 4 / (whPixels*4));
        for (int j = 0; j < 3; j++) {
            sigma[j] /= whPixels;
        }
        hist = buildCumulativeHist(histIn);
        histr = buildCumulativeHist(histInr);
        histInvr = buildCumulativeHistInv(histInr);
        histg = buildCumulativeHist(histIng);
        histb = buildCumulativeHist(histInb);



    }

    private float[] buildCumulativeHist(int[] hist) {
        float[] cumulativeHist = new float[HIST_BINS + 1];
        for (int i = 1; i < cumulativeHist.length; i++) {
            cumulativeHist[i] = cumulativeHist[i - 1] + hist[i - 1];
        }
        float max = cumulativeHist[HIST_BINS];
        for (int i = 0; i < cumulativeHist.length; i++) {
            cumulativeHist[i] /= max;
        }
        float[] prevH = cumulativeHist.clone();
        cumulativeHist = new float[histSize];
        for(int i =0; i<cumulativeHist.length;i++){
            cumulativeHist[i] = getInterpolated(prevH,i*((float)prevH.length/(cumulativeHist.length)));
        }
        return cumulativeHist;
    }
    private float[] buildCumulativeHistInv(int[] hist) {
        float[] cumulativeHist = new float[HIST_BINS + 1];
        for (int i = 1; i < cumulativeHist.length; i++) {
            cumulativeHist[i] = cumulativeHist[i - 1] + hist[hist.length - (i - 1) - 1];
        }
        float max = cumulativeHist[HIST_BINS];
        for (int i = 0; i < cumulativeHist.length; i++) {
            cumulativeHist[i] /= max;
        }
        float[] prevH = cumulativeHist.clone();
        cumulativeHist = new float[histSize];
        for(int i =0; i<cumulativeHist.length;i++){
            cumulativeHist[i] = getInterpolated(prevH,i*((float)prevH.length/(cumulativeHist.length)));
        }
        return cumulativeHist;
    }

}
