package com.particlesdevs.photoncamera.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.particlesdevs.photoncamera.gallery.helper.Constants;

public class AwsUtils {

    private static AmazonS3Client sS3Client;
    private static CognitoCachingCredentialsProvider sCredProvider;
    private static TransferUtility sTransferUtility;

    static String pool_id = "";
    static String region_id = "";

    private static CognitoCachingCredentialsProvider getCredProvider(Context context) {

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        pool_id = pref.getString("aws_pool_id_config", "");
        region_id = pref.getString("aws_region_id_config", "");
        Log.d("pool id", "Pool ID: " + pool_id);
        Log.d("region id", "Region ID: " + region_id);

        if (sCredProvider == null) {
            sCredProvider = new CognitoCachingCredentialsProvider(
                    context.getApplicationContext(),
                    pool_id,
                    Regions.fromName(region_id));
        }
        return sCredProvider;
    }

    public static AmazonS3Client getS3Client(Context context) {
        if (sS3Client == null) {
            sS3Client = new AmazonS3Client(getCredProvider(context.getApplicationContext()));
            sS3Client.setRegion(Region.getRegion(Regions.fromName(region_id)));
        }
        return sS3Client;
    }

    public static TransferUtility getTransferUtility(Context context) {
        if (sTransferUtility == null) {
            sTransferUtility = new TransferUtility(getS3Client(context.getApplicationContext()),
                    context.getApplicationContext());
        }

        return sTransferUtility;
    }
}
