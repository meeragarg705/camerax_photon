package com.particlesdevs.photoncamera;

import java.nio.ByteBuffer;


public class WrapperGPU {
    static {
        System.loadLibrary("hdrxgpu");
    }
    /**
     * Function to create pointers for image buffers.
     *
     * @param rows   Image rows.
     * @param cols   Image cols.
     * @param frames Image count.
     */
    public static native void init(int rows, int cols, int frames);
    public static native void initAlignments(int rows, int cols, int frames);

    /**
     * Function to load images.
     *
     * @param bufferptr Image buffer.
     */
    public static native void loadFrame(ByteBuffer bufferptr, float Exposure);
    public static native void loadFrameAlignments(ByteBuffer bufferptr, float Exposure);

    public static native void loadInterpolatedGainMap(ByteBuffer GainMap);

    public static native void outputBuffer(ByteBuffer outputBuffer);
    public static native void processFrame(float NoiseS, float NoiseO,float Smooth, float ElFactor, float BLr,float BLg,float BLb, float WLFactor,
    float wpR,float wpG, float wpB,int CfaPattern);
    public static native ByteBuffer processFrameAlignments();
}
