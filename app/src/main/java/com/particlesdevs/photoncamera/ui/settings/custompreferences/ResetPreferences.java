package com.particlesdevs.photoncamera.ui.settings.custompreferences;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceDialogFragmentCompat;

import com.google.android.material.snackbar.Snackbar;
import com.particlesdevs.photoncamera.R;
import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.settings.BackupRestoreUtil;

public class ResetPreferences extends DialogPreference {
    public ResetPreferences(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        setPersistent(false);
        setDialogTitle(android.R.string.dialog_alert_title);
        setDialogMessage(R.string.reset_preferences_warning);
        setPositiveButtonText(R.string.yes);
    }

    public static class Dialog extends PreferenceDialogFragmentCompat {
        public static Dialog newInstance(Preference preference) {
            final Dialog fragment = new Dialog();
            final Bundle bundle = new Bundle(1);
            bundle.putString(ARG_KEY, preference.getKey());
            fragment.setArguments(bundle);
            return fragment;
        }

        @Override
        public void onDialogClosed(boolean positiveResult) {
            if (positiveResult) {
                if (getContext() != null && getActivity() != null) {
                    String status = getString(R.string.app_will_restart);
                    if (!BackupRestoreUtil.resetPreferences(getContext()))
                        status = "Failed";
                    Snackbar.make(getActivity().findViewById(android.R.id.content), status, Snackbar.LENGTH_SHORT).show();
                    PhotonCamera.restartWithDelay(getContext(), 1000);
                }
            }
        }
    }
}