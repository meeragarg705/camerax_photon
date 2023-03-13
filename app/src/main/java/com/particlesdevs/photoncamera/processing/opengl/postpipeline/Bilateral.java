package com.particlesdevs.photoncamera.processing.opengl.postpipeline;

import android.util.Log;

import com.particlesdevs.photoncamera.R;
import com.particlesdevs.photoncamera.processing.opengl.GLTexture;
import com.particlesdevs.photoncamera.processing.opengl.nodes.Node;
import com.particlesdevs.photoncamera.processing.render.NoiseModeler;

public class Bilateral extends Node {

    public Bilateral() {
        super("bilateral", "Denoise");
    }

    @Override
    public void Compile() {
    }

    @Override
    public void Run() {
        NoiseModeler modeler = basePipeline.mParameters.noiseModeler;
        float noiseS = modeler.computeModel[0].first.floatValue()+
                modeler.computeModel[1].first.floatValue()+
                modeler.computeModel[2].first.floatValue();
        float noiseO = modeler.computeModel[0].second.floatValue()+
                modeler.computeModel[1].second.floatValue()+
                modeler.computeModel[2].second.floatValue();
        noiseS/=3.f;
        noiseO/=3.f;
        GLTexture map = glUtils.medianDown(previousNode.WorkingTexture,4);
        Log.d(Name,"NoiseS:"+noiseS+", NoiseO:"+noiseO);
        glProg.setDefine("NOISES",noiseS);
        glProg.setDefine("NOISEO",noiseO);
        glProg.setDefine("INTENSE", (float) basePipeline.mSettings.noiseRstr);
        glProg.setDefine("INSIZE",previousNode.WorkingTexture.mSize);
        glProg.useAssetProgram("bilateral");
        glProg.setTexture("NoiseMap",map);
        glProg.setTexture("InputBuffer",previousNode.WorkingTexture);
        WorkingTexture = basePipeline.getMain();
        glProg.drawBlocks(WorkingTexture);
        glProg.closed = true;
        map.close();
    }
}