package com.particlesdevs.photoncamera.processing.opengl.rawpipeline;

import android.media.Image;
import android.util.Log;

import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.processing.ImageFrame;
import com.particlesdevs.photoncamera.processing.opengl.GLBasePipeline;
import com.particlesdevs.photoncamera.processing.opengl.GLCoreBlockProcessing;
import com.particlesdevs.photoncamera.processing.opengl.GLFormat;
import com.particlesdevs.photoncamera.processing.opengl.GLInterface;
import com.particlesdevs.photoncamera.processing.render.Parameters;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class RawPipeline extends GLBasePipeline {
    public float sensitivity = 1.f;
    public ArrayList<ImageFrame> images;
    public ArrayList<ByteBuffer> alignments;
    public int alignAlgorithm;

    public ByteBuffer Run() {
        mParameters = PhotonCamera.getParameters();
        GLCoreBlockProcessing glproc = new GLCoreBlockProcessing(mParameters.rawSize, new GLFormat(GLFormat.DataType.UNSIGNED_16));
        //GLContext glContext = new GLContext(parameters.rawSize.x,parameters.rawSize.y);
        glint = new GLInterface(glproc);
        glint.parameters = mParameters;
        //add(new Debug(R.raw.debugraw,"DebugRaw"));
        if(alignAlgorithm == 1)
        add(new AlignAndMerge());
        if(alignAlgorithm == 2) {
            Log.d("RawPipeline","Entering hybrid alignment");
            //add(new AlignAndMergeCV());
            add(new AlignAndMerge());
        }
        return runAllRaw();
    }
}
