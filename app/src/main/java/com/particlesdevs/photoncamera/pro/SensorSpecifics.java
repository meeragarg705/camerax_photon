package com.particlesdevs.photoncamera.pro;

import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.particlesdevs.photoncamera.processing.render.SpecificSettingSensor;
import com.particlesdevs.photoncamera.settings.PreferenceKeys;
import com.particlesdevs.photoncamera.settings.SettingsManager;
import com.particlesdevs.photoncamera.util.HttpLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import static com.particlesdevs.photoncamera.util.FileManager.sPHOTON_TUNING_DIR;

public class SensorSpecifics {
    public SpecificSettingSensor[] specificSettingSensor;
    public SpecificSettingSensor selectedSensorSpecifics = new SpecificSettingSensor();
    ArrayList<String> loadNetwork(String device) throws IOException {
        ArrayList<String> inputStr = new ArrayList<String>();
        BufferedReader indevice = HttpLoader.readURL("https://raw.githubusercontent.com/eszdman/PhotonCamera/dev/app/specific/sensors/" + device + ".txt", 150);
        String str;
        while ((str = indevice.readLine()) != null) {
            Log.d("SensorSpecifics", "read:" + str);
            inputStr.add(str + "\n");
        }
        return inputStr;
    }
    ArrayList<String> loadLocal(File specifics) throws IOException {
        ArrayList<String> inputStr = new ArrayList<String>();
        String str;
        BufferedReader indevice = new BufferedReader(new FileReader(specifics));
        while ((str = indevice.readLine()) != null) {
            Log.d("SensorSpecifics", "read:" + str);
            inputStr.add(str + "\n");
        }
        return inputStr;
    }
    public void loadSpecifics(SettingsManager mSettingsManager){
        final boolean[] loaded = {mSettingsManager.getBoolean(PreferenceKeys.Key.DEVICES_PREFERENCE_FILE_NAME.mValue, "sensor_specific_loaded", false)};
        Log.d("SensorSpecifics", "loaded:"+ loaded[0]);
        int count = 0;
        String device = Build.BRAND.toLowerCase() + "/" + Build.DEVICE.toLowerCase();
        ArrayList<String> inputStr = new ArrayList<String>();
        File init = new File(sPHOTON_TUNING_DIR, "SensorSpecifics.txt");
        try {
            try {
                if(init.exists())
                    inputStr = loadLocal(init);
                else
                    inputStr = loadNetwork(device);
                count = 0;
                for (String str : inputStr) {
                    Log.d("SensorSpecifics", "read:" + str);
                    if (str.contains("sensor")) count++;
                }
                Log.d("SensorSpecifics", "SensorCount:" + count);
            } catch (Exception e){
                if(loaded[0]){
                    inputStr = mSettingsManager.getArrayList(PreferenceKeys.Key.DEVICES_PREFERENCE_FILE_NAME.mValue, "sensor_specific_loaded", new HashSet<>());
                    for (String str2 : inputStr) {
                        if (str2.contains("sensor")) count++;
                    }
                }
            }
            specificSettingSensor = new SpecificSettingSensor[count];
            count = 0;
            for (String str2 : inputStr) {
                if (str2.contains("sensor")) {
                    String[] vals = str2.split("_");
                    vals[1] = vals[1].replace("\n", "");
                    specificSettingSensor[count] = new SpecificSettingSensor();
                    specificSettingSensor[count].id = Integer.parseInt(vals[1]);
                    count++;
                } else {
                    String[] valsIn = str2.replace(" ","").replace("\n","").split("=");
                    if(valsIn.length <= 1) continue;
                    String[] istr = valsIn[1].replace("{", "").replace("}", "").split(",");
                    SpecificSettingSensor current = specificSettingSensor[count - 1];
                    AsyncTask.execute(() -> {
                        switch (valsIn[0]) {
                            case "NoiseModelA": {
                                for (int i = 0; i < 4; i++) {
                                    current.NoiseModelerArr[0][i] = Double.parseDouble(istr[i]);
                                }
                                break;
                            }
                            case "NoiseModelB": {
                                for (int i = 0; i < 4; i++) {
                                    current.NoiseModelerArr[1][i] = Double.parseDouble(istr[i]);
                                }
                                break;
                            }
                            case "NoiseModelC": {
                                for (int i = 0; i < 4; i++) {
                                    current.NoiseModelerArr[2][i] = Double.parseDouble(istr[i]);
                                }
                                break;
                            }
                            case "NoiseModelD": {
                                for (int i = 0; i < 4; i++) {
                                    current.NoiseModelerArr[3][i] = Double.parseDouble(istr[i]);
                                }
                                current.ModelerExists = true;
                                break;
                            }
                            case "captureSharpeningS": {
                                current.captureSharpeningS = (float)Double.parseDouble(valsIn[1]);
                                break;
                            }
                            case "captureSharpeningIntense": {
                                current.captureSharpeningIntense = (float)Double.parseDouble(valsIn[1]);
                                break;
                            }
                            case "aberrationCorrection": {
                                for (int i = 0; i < 8; i++) {
                                    current.aberrationCorrection[i] = (float)Double.parseDouble(istr[i]);
                                }
                                break;
                            }
                            case "calibrationTransform1": {
                                current.CalibrationTransform1 = new float[3][3];
                                for (int i = 0; i < 3; i++) {
                                    for(int j =0; j < 3;j++)
                                        current.CalibrationTransform1[i][j] = (float)Double.parseDouble(istr[i]);
                                }
                                break;
                            }
                            case "calibrationTransform2": {
                                current.CalibrationTransform2 = new float[3][3];
                                for (int i = 0; i < 3; i++) {
                                    for(int j =0; j < 3;j++)
                                        current.CalibrationTransform2[i][j] = (float)Double.parseDouble(istr[i]);
                                }
                                break;
                            }
                            case "colorTransform1": {
                                current.ColorTransform1 = new float[3][3];
                                for (int i = 0; i < 3; i++) {
                                    for(int j =0; j < 3;j++)
                                        current.ColorTransform1[i][j] = (float)Double.parseDouble(istr[i]);
                                }
                                break;
                            }
                            case "colorTransform2": {
                                current.ColorTransform2 = new float[3][3];
                                for (int i = 0; i < 3; i++) {
                                    for(int j =0; j < 3;j++)
                                        current.ColorTransform2[i][j] = (float)Double.parseDouble(istr[i]);
                                }
                                break;
                            }
                            case "forwardMatrix1": {
                                current.ForwardMatrix1 = new float[3][3];
                                for (int i = 0; i < 3; i++) {
                                    for(int j =0; j < 3;j++)
                                        current.ForwardMatrix1[i][j] = (float)Double.parseDouble(istr[i]);
                                }
                                break;
                            }
                            case "forwardMatrix2": {
                                current.ForwardMatrix2 = new float[3][3];
                                for (int i = 0; i < 3; i++) {
                                    for(int j =0; j < 3;j++)
                                        current.ForwardMatrix2[i][j] = (float)Double.parseDouble(istr[i]);
                                }
                                break;
                            }
                            case "referenceIlluminant1": {
                                current.referenceIlluminant1 = Integer.parseInt(valsIn[1]);
                                break;
                            }
                            case "referenceIlluminant2": {
                                current.referenceIlluminant2 = Integer.parseInt(valsIn[1]);
                                break;
                            }
                            case "overrideRawColors": {
                                current.overrideRawColors = Boolean.parseBoolean(valsIn[1]);
                                break;
                            }
                        }
                        current.updateTransforms();
                        loaded[0] = true;
                    });

                }
            }

            mSettingsManager.set(PreferenceKeys.Key.DEVICES_PREFERENCE_FILE_NAME.mValue, "sensor_specific_val",inputStr);
        } catch (Exception ignored) {}
        mSettingsManager.set(PreferenceKeys.Key.DEVICES_PREFERENCE_FILE_NAME.mValue, "sensor_specific_loaded", loaded[0]);
    }
    public void selectSpecifics(int id){
        if(specificSettingSensor != null) {
            for (SpecificSettingSensor specifics : specificSettingSensor) {
                if(specifics != null) {
                    if (specifics.id == id) {
                        Log.d("SensorSpecifics", "Selected:" + id);
                        selectedSensorSpecifics = specifics;
                    }
                }
            }
        }
    }

}
