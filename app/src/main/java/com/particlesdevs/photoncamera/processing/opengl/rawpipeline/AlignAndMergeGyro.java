package com.particlesdevs.photoncamera.processing.opengl.rawpipeline;

import android.graphics.Point;
import android.util.Log;

import com.particlesdevs.photoncamera.R;
import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.processing.ImageFrame;
import com.particlesdevs.photoncamera.processing.opengl.GLFormat;
import com.particlesdevs.photoncamera.processing.opengl.GLProg;
import com.particlesdevs.photoncamera.processing.opengl.GLTexture;
import com.particlesdevs.photoncamera.processing.opengl.nodes.Node;
import com.particlesdevs.photoncamera.processing.parameters.IsoExpoSelector;
import com.particlesdevs.photoncamera.processing.processor.ProcessorBase;
import com.particlesdevs.photoncamera.util.Utilities;

import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_LINEAR;

public class AlignAndMergeGyro extends Node {
    Point rawSize;
    GLProg glProg;

    public AlignAndMergeGyro() {
        super("", "AlignAndMerge");
    }

    @Override
    public void Compile() {}

    private void CorrectedRaw(GLTexture out, int number) {
        float bl = Math.min(Math.min(Math.min(PhotonCamera.getParameters().blackLevel[0],PhotonCamera.getParameters().blackLevel[1]),
                PhotonCamera.getParameters().blackLevel[2]),PhotonCamera.getParameters().blackLevel[3]);
        float mpy = minMpy / images.get(number).pair.layerMpy;
        glProg.setDefine("BL",PhotonCamera.getParameters().blackLevel);
        glProg.setDefine("WP",PhotonCamera.getParameters().whitePoint);
        glProg.setDefine("MPY",mpy);
        glProg.setDefine("BAYER",PhotonCamera.getParameters().cfaPattern);
        Log.d("Align","mpy:"+mpy);
        glProg.useAssetProgram("precorrection");
        GLTexture inraw = new GLTexture(rawSize, new GLFormat(GLFormat.DataType.UNSIGNED_16), images.get(number).buffer);
        glProg.setTexture("InputBuffer",inraw);
        glProg.setVar("WhiteLevel",(float)PhotonCamera.getParameters().whiteLevel);
        glProg.drawBlocks(out);
        inraw.close();
    }
    private static final boolean corners = true;
    private void BoxDown22(GLTexture input,GLTexture out) {
        glProg.useAssetProgram("boxdown22");
        glProg.setTexture("InputBuffer", input);
        glProg.setTexture("GainMap", GainMap);
        glProg.setVar("CfaPattern", PhotonCamera.getParameters().cfaPattern);
        glProg.drawBlocks(basePipeline.main3,out.mSize);
        //glUtils.SaveProgResult(output.mSize,"boxdown");
        //glProg.close();
        //GLTexture median = glUtils.blur(output,5.0);
        //GLTexture laplaced = glUtils.ops(median,output,"in2.rgb,3.0*(in1.a-in2.a)");
        //median.close();

        glUtils.median(basePipeline.main3,out,new Point(1,1));

        //GLTexture median = glUtils.blur(output,1.5);
    }
    private int logged = 0;
    private void GaussDown44(GLTexture input,GLTexture out,boolean median) {
        if(median) {
            if(input.mSize.x+input.mSize.y > 9) {
                glUtils.interpolate(input, basePipeline.main3,out.mSize, 1.0 / 4.0);
                glUtils.median(basePipeline.main3, out, new Point(1, 1));
            } else {
                glUtils.median(input, out, new Point(1, 1));
            }
        } else{
            glUtils.interpolate(input, out, 1.0 / 4.0);
        }
        /*
        glUtils.ConvDiff(out,basePipeline.main3,tileSize/3,true,false);
        glUtils.Maximaze(basePipeline.main3,prevV,outV);
        //glUtils.bluxVH(basePipeline.main3,outV,(float)tileSize*2,true);
        if(logged>0){
            glUtils.convertVec4(outV,"(in1.r+in1.g)/0.5");
            glUtils.SaveProgResult(outH.mSize,"convV");
            logged--;
        }

        glUtils.ConvDiff(out,basePipeline.main3,tileSize/3,false,false);
        glUtils.Maximaze(basePipeline.main3,prevH,outH);
        //glUtils.bluxVH(basePipeline.main3,outH,(float)tileSize*2,false);
        if(logged>0){
            glUtils.convertVec4(outH,"(in1.r+in1.g)/0.5");
            glUtils.SaveProgResult(outH.mSize,"convH");
            logged--;
        }*/

        //glUtils.median(basePipeline.main3,out, new Point(1,1));
    }

    private GLTexture LaplacDown44(GLTexture input) {
        glProg.useAssetProgram("laplaciandown44");
        glProg.setTexture("InputBuffer", input);
        GLTexture output = new GLTexture(new Point(input.mSize.x / 4, input.mSize.y / 4), input.mFormat, null);
        glProg.drawBlocks(output);
        glProg.close();
        return output;
    }
    private void PrepareDiffs(GLTexture in,GLTexture ref,int i){
        float rotation = (float) images.get(i).rotation;
        //glUtils.ConvDiff(in,DiffVIn,tileSize/3,true,false);
        glUtils.ConvDiff(in, DiffHVIn,0.f);
        //glUtils.ConvDiff(ref,DiffVRef,tileSize/3,true,false);
        glUtils.ConvDiff(ref, DiffHVRef,0.f);
        glUtils.Corners(DiffHVRef,CornersRef);
        glUtils.Corners(DiffHVIn,CornersIn);
        if(logged>0){
            glUtils.convertVec4(in,"in1.r+in1.g");
            glUtils.SaveProgResult(in.mSize,"in");
            glUtils.convertVec4(ref,"in1.r+in1.g");
            glUtils.SaveProgResult(in.mSize,"ref");
            logged--;
        }
    }
    GLTexture medium;
    GLTexture small;
    GLTexture vsmall;
    private void Align(int i) {
        //startT();
        Point movement = new Point((int)((images.get(0).posx-images.get(i).posx)/2.0),(int)((images.get(0).posy-images.get(i).posy)/2.0));
        PrepareDiffs(brTex128,BaseFrame128,i);
        glProg.setDefine("SCANSIZE",tileSize*2);
        glProg.setDefine("TILESIZE",tileSize);
        glProg.setDefine("PREVSCALE",0);
        glProg.setDefine("INPUTSIZE",brTex128.mSize);
        glProg.setDefine("INITIALMOVE", Utilities.div(movement,64));
        glProg.setDefine("LOWPASSCOMBINE",false);

        glProg.useAssetProgram("pyramidalign2");

        //glProg.setTexture("InputBufferH", DiffHVIn);
        //glProg.setTexture("MainBufferH", DiffHVRef);
        glProg.setTexture("CornersRef",CornersRef);
        glProg.setTexture("DiffHVIn",DiffHVIn);
        glProg.setTexture("DiffHVRef",DiffHVRef);
        //glProg.setTexture("DiffHVRef",DiffVRef);
        //glProg.setTexture("DiffHVIn",DiffVIn);
        glProg.setTexture("InputBuffer",brTex128);
        glProg.setTexture("MainBuffer",BaseFrame128);
        //glProg.setTexture("MainBuffer",BaseFrame128);

        glProg.drawBlocks(vsmall);

        PrepareDiffs(brTex32,BaseFrame32,i);
        glProg.setDefine("SCANSIZE",tileSize*2);
        glProg.setDefine("TILESIZE",tileSize);
        glProg.setDefine("PREVSCALE",4);
        glProg.setDefine("INPUTSIZE",brTex32.mSize);
        glProg.setDefine("INITIALMOVE", Utilities.div(movement,16));
        glProg.setDefine("LUCKYINPUT",false);
        glProg.setDefine("LOWPASSCOMBINE",false);
        glProg.setDefine("LOWPASSK",4);
        glProg.useAssetProgram("pyramidalign2");
        glProg.setTexture("AlignVectors",vsmall);

        //glProg.setTexture("InputBufferH", DiffHVIn);
        //glProg.setTexture("MainBufferH", DiffHVRef);
        glProg.setTexture("CornersRef",CornersRef);
        glProg.setTexture("DiffHVIn",DiffHVIn);
        glProg.setTexture("DiffHVRef",DiffHVRef);
        //glProg.setTexture("DiffHVRef",DiffVRef);
        //glProg.setTexture("DiffHVIn",DiffVIn);
        glProg.setTexture("InputBuffer",brTex32);
        glProg.setTexture("MainBuffer",BaseFrame32);
        //glProg.setTexture("MainBuffer",BaseFrame32);
        glProg.drawBlocks(small);

        PrepareDiffs(brTex8,BaseFrame8,i);
        glProg.setDefine("SCANSIZE",tileSize*2);
        glProg.setDefine("TILESIZE",tileSize);
        glProg.setDefine("PREVSCALE",0);
        glProg.setDefine("INPUTSIZE",brTex8.mSize);
        glProg.setDefine("INITIALMOVE", Utilities.div(movement,4));
        glProg.setDefine("LUCKYINPUT",false);
        glProg.setDefine("LOWPASSCOMBINE",false);
        glProg.setDefine("LOWPASSK",16);
        glProg.useAssetProgram("pyramidalign2");
        glProg.setTexture("AlignVectors",small);

        //glProg.setTexture("InputBufferH", DiffHVIn);
        //glProg.setTexture("MainBufferH", DiffHVRef);
        glProg.setTexture("CornersRef",CornersRef);
        glProg.setTexture("DiffHVIn",DiffHVIn);
        glProg.setTexture("DiffHVRef",DiffHVRef);
        //glProg.setTexture("DiffHVRef",DiffVRef);
        //glProg.setTexture("DiffHVIn",DiffVIn);

        glProg.setTexture("InputBuffer",brTex8);
        glProg.setTexture("MainBuffer",BaseFrame8);

        glProg.drawBlocks(medium);
        //small.close();
        logged = 0;
        PrepareDiffs(brTex2,BaseFrame2,i);
        glProg.setDefine("SCANSIZE",tileSize*2);
        glProg.setDefine("TILESIZE",tileSize);
        glProg.setDefine("PREVSCALE",4);
        glProg.setDefine("INPUTSIZE",brTex2.mSize);
        glProg.setDefine("INITIALMOVE", movement);
        glProg.setDefine("LUCKYINPUT",false);
        glProg.setDefine("LOWPASSCOMBINE",false);
        glProg.setDefine("LOWPASSK",64);
        glProg.useAssetProgram("pyramidalign2");
        glProg.setTexture("AlignVectors",medium);

        //glProg.setTexture("InputBufferH", DiffHVIn);
        //glProg.setTexture("MainBufferH", DiffHVRef);
        glProg.setTexture("CornersRef",CornersRef);
        glProg.setTexture("DiffHVIn",DiffHVIn);
        glProg.setTexture("DiffHVRef",DiffHVRef);
        //glProg.setTexture("DiffHVIn",DiffVIn);

        glProg.setTexture("InputBuffer",brTex2);
        glProg.setTexture("MainBuffer",BaseFrame2);

        glProg.drawBlocks(alignVector);
        alignVectorsTemporal[i-1] = alignVector.textureBuffer(alignVector.mFormat,false).asReadOnlyBuffer().asIntBuffer();
        alignVectorsTemporal[i-1].get(alignments[i-1]);
        for(int j = 0; j<alignments[i-1].length;j++){
            alignments[i-1][j] = Integer.reverseBytes(alignments[i-1][j]);
        }
    }

    private void Weights() {
        GLTexture out = Weights;
        GLTexture alt = WeightsAlt;
        GLTexture t = Weights;
        glProg.useAssetProgram("sumweights");
        for(int i =1; i<images.size();i++){
            glProg.setTexture("WeightsIn", Weight[i-1]);
            glProg.setTexture("WeightsOut", out);
            glProg.drawBlocks(alt);
            t = alt;
            alt = out;
            out = t;
        }
        Weights = t;
    }
    private int MirrorCoords(int in){
        if(in < 0) {
            in = -in;
        } else {
            if(in > alignments.length-1)
                in = -in+alignments.length-1;
        }
        in = Math.min(Math.max(in,0),alignments.length-1);
        return in;
    }
    private void FilterTemporal() {
        //TODO Detect frames time difference
    }
    private void Weight(int num) {
        glProg.setDefine("TILESIZE","("+tileSize+")");
        glProg.setDefine("WEIGHTSIZE","("+tileSize*4+")");
        glProg.setDefine("FRAMECOUNT",images.size());
        glProg.useAssetProgram("spatialweights");
        glProg.setTexture("CornersRef", CornersRef);
        glProg.setTexture("CornersIn", CornersIn);
        glProg.setTexture("AlignVectors", alignVector);
        glProg.drawBlocks(Weight[num-1]);
        //glUtils.convertVec4(Weight[num-1],"(in1*10.0)");
        //glUtils.SaveProgResult(Weight[num-1].mSize,"WGht");
    }
    private GLTexture Merge(GLTexture Output, GLTexture inputRaw,int num) {
        //startT();
        glProg.setDefine("TILESIZE","("+tileSize+")");
        glProg.setDefine("MIN",minMpy);
        glProg.setDefine("MPY",minMpy / images.get(num).pair.layerMpy);
        glProg.setDefine("WP",PhotonCamera.getParameters().whitePoint);
        glProg.setDefine("BAYER",PhotonCamera.getParameters().cfaPattern);
        glProg.setDefine("HDR",IsoExpoSelector.HDR);
        glProg.setDefine("ROTATIOn", (float) images.get(num).rotation);
        glProg.useAssetProgram("spatialmerge");

        /*short[] bufin = new short[alignVectors[0].mSize.x*alignVectors[0].mSize.y*4];
        for(int k =0; k<alignVectors[0].mSize.x*alignVectors[0].mSize.y*4; k+=4){
            bufin[k] = (short) ((images.get(0).posx-images.get(num).posx)/2.0);
            bufin[k+1] = (short) ((images.get(0).posy-images.get(num).posy)/2.0);
            bufin[k+2] = 0;
            bufin[k+3] = 0;
        }
        //alignVectorsTemporal[num-1].get(bufin);
        Log.d("AlignAndMerge","Vectors->"+ Arrays.toString(bufin));*/
        GLTexture filteredVectors = new GLTexture(alignVector.mSize,alignVector.mFormat, IntBuffer.wrap(alignments[num-1]));
        Log.d("AlignAndMerge","Vectors->"+ Arrays.toString(alignments[num-1]));
        //GLTexture temporalFilteredVector = new GLTexture(alignVectors[num-1].mSize,alignVectors[num-1].mFormat,alignVectorsTemporal[num-1]);
        glProg.setTexture("AlignVectors", filteredVectors);
        glProg.setTexture("SumWeights", Weights);
        glProg.setTexture("Weight", Weight[num-1]);

        glProg.setTexture("MainBuffer", BaseFrame);
        glProg.setTexture("InputBuffer", inputRaw);

        glProg.setTexture("InputBuffer22", brTex2);
        //glProg.setTexture("MainBuffer22", BaseFrame2);

        if(num == 1){
            glProg.setTexture("OutputBuffer", BaseFrame);
        } else glProg.setTexture("OutputBuffer", Output);
        glProg.setVar("alignk", 1.f / (float) (((RawPipeline) (basePipeline)).images.size()));
        glProg.setVar("number",num+1);
        glProg.setVarU("rawsize", rawSize);
        glProg.setVarU("alignsize", alignVector.mSize);
        GLTexture output = basePipeline.getMain();
        glProg.drawBlocks(output);
        //temporalFilteredVector.close();
        //glProg.drawBlocks(Output,128,true);
        //Output.close();
        //endT("Merge");
        return output;
    }

    private GLTexture RawOutput(GLTexture input) {
        //startT();
        float[] outBL = new float[4];
        for(int i=0;i<outBL.length;i++) outBL[i] = PhotonCamera.getParameters().blackLevel[i]*(ProcessorBase.FAKE_WL/((float)PhotonCamera.getParameters().whiteLevel));
        glProg.setDefine("BL",outBL);
        glProg.setDefine("BAYER",PhotonCamera.getParameters().cfaPattern);
        glProg.useAssetProgram("toraw");
        glProg.setTexture("InputBuffer", input);
        glProg.setVar("whitelevel", ProcessorBase.FAKE_WL);
        GLTexture output = new GLTexture(rawSize, new GLFormat(GLFormat.DataType.UNSIGNED_16), null);
        glProg.drawBlocks(output);
        glProg.closed = true;
        //endT("RawOutput");
        return output;
    }
    ArrayList<ImageFrame> images;
    GLTexture BaseFrame, BaseFrame2, BaseFrame8, BaseFrame32,BaseFrame128;
    GLTexture brTex2, brTex8, brTex32,brTex128;
    IntBuffer[] alignVectorsTemporal;
    GLTexture alignVector;
    GLTexture Weights,WeightsAlt;
    GLTexture[] Weight;
    GLTexture GainMap;
    GLTexture DiffVRef, DiffHVRef,CornersRef;
    GLTexture DiffVIn, DiffHVIn,CornersIn;
    int[][] alignments;
    final int tileSize = 128;
    float minMpy = 1000.f;
    @Override
    public void Run() {
        glProg = basePipeline.glint.glProgram;
        RawPipeline rawPipeline = (RawPipeline) basePipeline;
        rawSize = rawPipeline.glint.parameters.rawSize;
        images = rawPipeline.images;

        for (int i = 0; i < IsoExpoSelector.fullpairs.size(); i++) {
            if (IsoExpoSelector.fullpairs.get(i).layerMpy < minMpy) {
                minMpy = IsoExpoSelector.fullpairs.get(i).layerMpy;
            }
        }
        if (images.get(0).pair.layerMpy != minMpy) {
            for (int i = 1; i < images.size(); i++) {
                if (images.get(i).pair.layerMpy == minMpy) {
                    ImageFrame frame = images.get(0);
                    images.set(0, images.get(i));
                    images.set(i, frame);
                    break;
                }
            }
        }
        long time = System.currentTimeMillis();
        BaseFrame = new GLTexture(rawSize,new GLFormat(GLFormat.DataType.FLOAT_16));
        CorrectedRaw(BaseFrame,0);
        basePipeline.main2 = new GLTexture(BaseFrame);
        basePipeline.main1 = new GLTexture(BaseFrame);
        GainMap = new GLTexture(basePipeline.mParameters.mapSize, new GLFormat(GLFormat.DataType.FLOAT_16,4),
                FloatBuffer.wrap(basePipeline.mParameters.gainMap),GL_LINEAR,GL_CLAMP_TO_EDGE);
        Log.d("AlignAndMerge", "Corrected raw elapsed time:" + (System.currentTimeMillis() - time) + " ms");
        BaseFrame2 = new GLTexture(BaseFrame.mSize.x/2,BaseFrame.mSize.y/2,new GLFormat(GLFormat.DataType.FLOAT_16,2),GL_LINEAR,GL_CLAMP_TO_EDGE);
        DiffVRef = new GLTexture(BaseFrame2);
        DiffHVRef = new GLTexture(BaseFrame2);
        DiffVIn = new GLTexture(BaseFrame2);
        DiffHVIn = new GLTexture(BaseFrame2);
        CornersRef = new GLTexture(BaseFrame2);
        CornersIn = new GLTexture(BaseFrame2);
        BaseFrame8 = new GLTexture(BaseFrame2.mSize.x/4, BaseFrame2.mSize.y/4, BaseFrame2.mFormat,GL_LINEAR,GL_CLAMP_TO_EDGE);
        BaseFrame32 = new GLTexture(BaseFrame8.mSize.x/4, BaseFrame8.mSize.y/4, BaseFrame2.mFormat,GL_LINEAR,GL_CLAMP_TO_EDGE);
        BaseFrame128 = new GLTexture(BaseFrame32.mSize.x/4, BaseFrame32.mSize.y/4, BaseFrame2.mFormat,GL_LINEAR,GL_CLAMP_TO_EDGE);

        basePipeline.main3 = new GLTexture(BaseFrame2);
        BoxDown22(BaseFrame, BaseFrame2);
        GaussDown44(BaseFrame2, BaseFrame8,false);
        GaussDown44(BaseFrame8, BaseFrame32,false);
        GaussDown44(BaseFrame32,BaseFrame128,false);

        GLTexture Output = basePipeline.getMain();
        CorrectedRaw(Output,0);
        Log.d("AlignAndMerge", "Resize elapsed time:" + (System.currentTimeMillis() - time) + " ms");
        time = System.currentTimeMillis();
        Log.d("AlignAndMerge","ImagesCount:"+images.size());
        brTex2 = new GLTexture(BaseFrame2);
        brTex8 = new GLTexture(BaseFrame8);
        brTex32 = new GLTexture(BaseFrame32);
        brTex128 = new GLTexture(BaseFrame128);

        int added = 1;
        Weight = new GLTexture[images.size()-1];
        alignVectorsTemporal = new IntBuffer[images.size()-1];
        alignVector = new GLTexture(new Point((brTex2.mSize.x / (tileSize))+added, (brTex2.mSize.y / (tileSize))+added), new GLFormat(GLFormat.DataType.SIGNED_32, 4));
        for(int i = 1; i<images.size();i++){
            Weight[i-1] = new GLTexture(alignVector.mSize,new GLFormat(GLFormat.DataType.FLOAT_16),GL_LINEAR,GL_CLAMP_TO_EDGE);
        }

        medium = new GLTexture(new Point((brTex8.mSize.x / (tileSize))+added, (brTex8.mSize.y / (tileSize))+added), alignVector.mFormat);
        small = new GLTexture(new Point((brTex32.mSize.x / (tileSize))+added, (brTex32.mSize.y / (tileSize))+added),alignVector.mFormat);
        vsmall = new GLTexture(new Point((brTex128.mSize.x / (tileSize))+added, (brTex128.mSize.y / (tileSize))+added), alignVector.mFormat);
        Weights = new GLTexture(alignVector.mSize,new GLFormat(GLFormat.DataType.FLOAT_16),GL_LINEAR,GL_CLAMP_TO_EDGE);
        WeightsAlt = new GLTexture(Weights);
        GLTexture inputraw = new GLTexture(BaseFrame);
        alignments = new int[images.size()-1][alignVector.getByteCount()/4];
        for (int i = 1; i < images.size(); i++) {
            CorrectedRaw(inputraw, i);
            BoxDown22(inputraw, brTex2);
            GaussDown44(brTex2, brTex8, false);
            GaussDown44(brTex8, brTex32, false);
            GaussDown44(brTex32, brTex128, false);
            Align(i);
            Weight(i);
        }
        Weights();
        //FilterTemporal();
        for (int i = 1; i < images.size(); i++) {
            CorrectedRaw(inputraw,i);
            images.get(i).image.close();
            Output = Merge(Output, inputraw,i);
        }
        for(int i = 0; i<images.size()-1;i++){
            Weight[i].close();
        }
        alignVector.close();
        inputraw.close();
        brTex2.close();
        brTex8.close();
        brTex32.close();
        BaseFrame2.close();
        BaseFrame8.close();
        BaseFrame32.close();
        BaseFrame128.close();
        Log.d("AlignAndMerge", "AlignmentAndMerge elapsed time:" + (System.currentTimeMillis() - time) + " ms");
        WorkingTexture = RawOutput(Output);
    }
}
