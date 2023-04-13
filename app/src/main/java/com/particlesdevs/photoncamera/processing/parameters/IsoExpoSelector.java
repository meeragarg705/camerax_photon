package com.particlesdevs.photoncamera.processing.parameters;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.util.Log;
import android.util.Range;

import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.capture.CaptureController;

import java.util.ArrayList;

public class IsoExpoSelector {
    public static final int baseFrame = 1;
    private static final String TAG = "IsoExpoSelector";
    public static boolean HDR = false;
    public static boolean useTripod = false;
    public static final int patternSize = 3;
    public static ArrayList<ExpoPair> pairs = new ArrayList<>();
    public static ArrayList<ExpoPair> fullpairs = new ArrayList<>();
    public static long lastSelectedExposure = 100;

    public static void setExpo(CaptureRequest.Builder builder, int step, CaptureController captureController) {
        Log.v(TAG, "InputParams: " +
                "expo time:" + ExposureIndex.sec2string(ExposureIndex.time2sec(captureController.mPreviewExposureTime)) +
                " iso:" + captureController.mPreviewIso+ " analog:"+getISOAnalog());
        if(step == 0) fullpairs.clear();
        ExpoPair pair = GenerateExpoPair(step,captureController);
        fullpairs.add(pair);
        Log.v(TAG, "IsoSelected:" + pair.iso +
                " ExpoSelected:" + ExposureIndex.sec2string(ExposureIndex.time2sec(pair.exposure)) + " sec step:" + step + " HDR:" + HDR);

        builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
        builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, pair.exposure);
        builder.set(CaptureRequest.SENSOR_SENSITIVITY, 3000);
        lastSelectedExposure = pair.exposure;
    }
    private static double mpy1 = 1.0;
    public static ExpoPair GenerateExpoPair(int step, CaptureController captureController) {
        double mpy = 1.0;
        ExpoPair pair = new ExpoPair(captureController.mPreviewExposureTime, getEXPLOW(), getEXPHIGH(),
                captureController.mPreviewIso, getISOLOW(), getISOHIGH(),getISOAnalog());
        double compensation = Math.pow(2.0,PhotonCamera.getSettings().exposureCompensation);
        pair.normalizeiso100();
        pair.ExpoCompensateLower(1.0/compensation);

        if (pair.exposure < ExposureIndex.sec / 40 && pair.normalizedIso() > 90.0/mpy1) {
            pair.ReduceIso();
        }
        if (pair.exposure < ExposureIndex.sec / 13 && pair.normalizedIso() > 750.0/mpy1) {
            pair.ReduceIso();
        }
        if (pair.exposure < ExposureIndex.sec / 8 && pair.normalizedIso() > 1500.0/mpy1) {
            if (step != baseFrame || !PhotonCamera.getSettings().eisPhoto) pair.ReduceIso();
        }
        if (pair.exposure < ExposureIndex.sec / 8 && pair.normalizedIso() > 1500.0/mpy1) {
            if (step != baseFrame || !PhotonCamera.getSettings().eisPhoto) pair.ReduceIso(1.25);
        }
        if (pair.normalizedIso() >= 12700.0/mpy1) {
            pair.ReduceIso();
        }
        if (CaptureController.getTargetFormat() == CaptureController.RAW_FORMAT) {
            pair.ExpoCompensateLower(mpy);
        }
        if (useTripod) {
            pair.UseIso(Math.max(pair.isoanalog/6.0,101));
        }

        double currentManExp = captureController.getParamController().getCurrentExposureValue();
        double currentManISO = captureController.getParamController().getCurrentISOValue();
        pair.exposure = currentManExp != 0 ? (long) currentManExp : pair.exposure;
        pair.iso = currentManISO != 0 ? (int) currentManISO : pair.iso;
        pair.curlayer = ExpoPair.exposureLayer.Normal;
        /*if (step%patternSize == 1 && HDR) {
            pair.ExpoCompensateLower(2.0 / 1.0);
            pair.curlayer = ExpoPair.exposureLayer.Low;
        }*/
        /*if(HDR) {
            pair.ExpoCompensateLowerExpo(2.f);
            pair.ExpoCompensateLower(1.f/2.f);
        }*/
        if (step%patternSize == 0 && HDR) {
            //pair.layerMpy = 1.5f+(float)Math.abs(Math.random())*1.0f;
            //pair.layerMpy = 1.f/1.5f;
            pair.ExpoCompensateLowerExpo(1.0 / pair.layerMpy);
            pair.curlayer = ExpoPair.exposureLayer.Normal;
        }
        if (step%patternSize == 1 && HDR) {
            pair.layerMpy = 4.f;
            pair.ExpoCompensateLowerExpo(1.0 / pair.layerMpy);
            pair.curlayer = ExpoPair.exposureLayer.High;
        }
        if (step%patternSize == 2 && HDR) {
            //pair.layerMpy = 1.f/1.5f;
            pair.layerMpy = 4.f;
            pair.ExpoCompensateLowerExpo(1.0 / pair.layerMpy);
            pair.curlayer = ExpoPair.exposureLayer.High;
        }

        if (pair.exposure < ExposureIndex.sec / 90 && PhotonCamera.getSettings().eisPhoto) {
            //HDR = true;
        }
        if (step == baseFrame) {
            if (pair.normalizedIso() <= 120.0/mpy1 && pair.exposure > ExposureIndex.sec / 70.0/mpy1 && PhotonCamera.getSettings().eisPhoto) {
                pair.ReduceExpo();
            }
            if (pair.normalizedIso() <= 245.0/mpy1 && pair.exposure > ExposureIndex.sec / 50.0/mpy1 && PhotonCamera.getSettings().eisPhoto) {
                pair.ReduceExpo();
            }
            if (pair.exposure < ExposureIndex.sec * 3.00 && pair.exposure > ExposureIndex.sec / 3 && pair.normalizedIso() < 3200.0/mpy1 && PhotonCamera.getSettings().eisPhoto) {
                pair.FixedExpo(1.0 / 8);
                if (pair.normalizeCheck())
                    PhotonCamera.showToast("Wrong parameters: iso:" + pair.iso + " exp:" + pair.exposure);
            }
        }
        if(step != -1) {
            if (step == 0) pairs.clear();
            if (pairs.size() < patternSize) {
                Log.d(TAG, "Added pair:" + pairs.size());
                pairs.add(pair);
            }
        }
        pair.denormalizeSystem();
        return pair;
    }

    public static double getMPY() {
        return 100.0 / getISOLOW();
    }

    private static int mpyIso(int in) {
        return (int) (in * getMPY());
    }

    private static int getISOHIGH() {
        Object key = CaptureController.mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
        if (key == null) return 3200;
        else {
            return (int) ((Range) (key)).getUpper();
        }
    }

    public static int getISOHIGHExt() {
        return mpyIso(getISOHIGH());
    }

    private static int getISOLOW() {
        Object key = CaptureController.mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
        if (key == null) return 100;
        else {
            return (int) ((Range) (key)).getLower();
        }
    }
    public static int getISOAnalog() {
        Object key = CaptureController.mCameraCharacteristics.get(CameraCharacteristics.SENSOR_MAX_ANALOG_SENSITIVITY);
        if (key == null) return 100;
        else {
            return (int)(key);
        }
    }

    public static int getISOLOWExt() {
        return mpyIso(getISOLOW());
    }

    public static long getEXPHIGH() {
        Object key = CaptureController.mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
        if (key == null) return ExposureIndex.sec;
        else {
            return (long) ((Range) (key)).getUpper();
        }
    }

    public static long getEXPLOW() {
        Object key = CaptureController.mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
        if (key == null) return ExposureIndex.sec / 1000;
        else {
            return (long) ((Range) (key)).getLower();
        }
    }


    //==================================Class : ExpoPair==================================//

    public static class ExpoPair {
        public enum exposureLayer{
            Low,
            Normal,
            High
        }
        public exposureLayer curlayer;
        public float layerMpy = 1.f;
        public long exposure;
        public int iso;
        long exposurehigh, exposurelow;
        int isolow, isohigh,isoanalog;

        public ExpoPair(ExpoPair pair) {
            copyfrom(pair);
        }

        public ExpoPair(long expo, long expl, long exph, int is, int islow, int ishigh, int analog) {
            exposure = expo;
            iso = is;
            exposurehigh = exph;
            exposurelow = expl;
            isolow = islow;
            isohigh = ishigh;
            isoanalog = analog;
        }
        public double Exposure(){
            return ExposureIndex.time2sec(exposure)*iso;
        }
        public void copyfrom(ExpoPair pair) {
            exposure = pair.exposure;
            exposurelow = pair.exposurelow;
            exposurehigh = pair.exposurehigh;
            iso = pair.iso;
            isolow = pair.isolow;
            isohigh = pair.isohigh;
            isoanalog = pair.isoanalog;
        }

        public void normalizeiso100() {
            double mpy = 100.0 / isolow;
            iso *= mpy;
            isoanalog *=mpy;
        }

        public void denormalizeSystem() {
            double div = 100.0 / isolow;
            iso /= div;
            isoanalog /=div;
        }
        public float normalizedIso(){
            return (float)iso/isoanalog;
        }
        public void normalize() {
            double div = 100.0 / isolow;
            if (iso / div > isohigh) iso = isohigh;
            if (iso / div < isolow) iso = isolow;
            if (exposure > exposurehigh) exposure = exposurehigh;
            if (exposure < exposurelow) exposure = exposurelow;
        }

        public boolean normalizeCheck() {
            double div = 100.0 / isolow;
            boolean wrongparams = false;
            if (iso / div > isohigh) wrongparams = true;
            if (iso / div < isolow) wrongparams = true;
            if (exposure > exposurehigh) wrongparams = true;
            if (exposure < exposurelow) wrongparams = true;
            return wrongparams;
        }

        public void ExpoCompensateLower(double k) {
            iso /= k;
            if (normalizeCheck()) {
                iso *= k;
                exposure /= k;
                if (normalizeCheck()) {
                    exposure *= k;
                    layerMpy = 1.f;
                }
            }
        }
        public void ExpoCompensateLowerExpo(double k) {
            iso /= k;
            if (normalizeCheck()) {
                iso *= k;
                exposure /= k;
                if (normalizeCheck()) {
                    exposure *= k;
                }
            }
        }

        public void MinIso() {
            UseIso(101);
        }

        public void UseIso(double isoUsed) {
            double k = iso / isoUsed;
            ReduceIso(k);
            if (normalizeCheck()) {
                iso *= (double) (exposure) / exposurehigh;
                exposure = exposurehigh;
                if (normalizeCheck()) {
                    iso = isohigh;
                }
            }
        }

        public void ReduceIso() {
            ReduceIso(2.0);
            if (normalizeCheck()) {
                ReduceIso(1.0 / 2);
            }
        }

        public void ReduceIso(double k) {
            iso /= k;
            exposure *= k;
        }

        public void ReduceExpo() {
            ReduceExpo(2.0);
            if (normalizeCheck()) ReduceExpo(1.0 / 2);
        }

        public void ReduceExpo(double k) {
            Log.d(TAG, "ExpoReducing iso:" + iso + " expo:" + ExposureIndex.sec2string(ExposureIndex.time2sec(exposure)));
            iso *= k;
            exposure /= k;
            Log.d(TAG, "ExpoReducing done iso:" + iso + " expo:" + ExposureIndex.sec2string(ExposureIndex.time2sec(exposure)));
        }

        public void FixedExpo(double expo) {
            long expol = ExposureIndex.sec2time(expo);
            double k = (double) exposure / expol;
            ReduceExpo(k);
            Log.d(TAG, "ExpoFixating iso:" + iso + " expo:" + ExposureIndex.sec2string(ExposureIndex.time2sec(exposure)));
            if (normalizeCheck()) ReduceExpo(1 / k);
        }

        public String ExposureString() {
            return ExposureIndex.sec2string(ExposureIndex.time2sec(exposure));
        }
    }
}
