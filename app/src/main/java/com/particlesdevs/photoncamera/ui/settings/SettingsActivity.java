package com.particlesdevs.photoncamera.ui.settings;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.google.android.material.snackbar.Snackbar;
import com.hunter.library.debug.HunterDebug;
import com.particlesdevs.photoncamera.R;
import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.app.base.BaseActivity;
import com.particlesdevs.photoncamera.pro.SupportedDevice;
import com.particlesdevs.photoncamera.settings.BackupRestoreUtil;
import com.particlesdevs.photoncamera.settings.PreferenceKeys;
import com.particlesdevs.photoncamera.settings.SettingsManager;
import com.particlesdevs.photoncamera.ui.settings.custompreferences.ResetPreferences;
import com.particlesdevs.photoncamera.util.log.FragmentLifeCycleMonitor;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import static com.particlesdevs.photoncamera.settings.PreferenceKeys.Key.ALL_DEVICES_NAMES_KEY;
import static com.particlesdevs.photoncamera.settings.PreferenceKeys.SCOPE_GLOBAL;

public class SettingsActivity extends BaseActivity implements PreferenceFragmentCompat.OnPreferenceStartScreenCallback {
    public static boolean toRestartApp;
    @HunterDebug
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getDelegate().setLocalNightMode(PreferenceKeys.getThemeValue());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, new SettingsFragment())
                .commit();
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentLifeCycleMonitor(), true);

    }

    public void back(View view) {
        onBackPressed();
    }
    @HunterDebug
    @Override
    public boolean onPreferenceStartScreen(@NonNull PreferenceFragmentCompat preferenceFragmentCompat,
                                           PreferenceScreen preferenceScreen) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.animate_slide_left_enter, R.anim.animate_slide_left_exit
                        , R.anim.animate_card_enter, R.anim.animate_slide_right_exit);
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, preferenceScreen.getKey());
        fragment.setArguments(args);
        ft.replace(R.id.settings_container, fragment, preferenceScreen.getKey());
        ft.addToBackStack(preferenceScreen.getKey());
        ft.commit();
        return true;
    }

    @Override
    public void onBackPressed() {
        if (toRestartApp) {
            PhotonCamera.restartApp(this);
        }
        super.onBackPressed();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener, PreferenceManager.OnPreferenceTreeClickListener {
        private static final String KEY_MAIN_PARENT_SCREEN = "prefscreen";
        private Activity activity;
        private SettingsManager mSettingsManager;
        private Context mContext;
        private View mRootView;
        private SupportedDevice supportedDevice;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            activity = getActivity();
            mContext = getContext();
            mSettingsManager = Objects.requireNonNull(PhotonCamera.getInstance(activity)).getSettingsManager();
            supportedDevice = Objects.requireNonNull(PhotonCamera.getInstance(activity)).getSupportedDevice();
            Objects.requireNonNull(getPreferenceScreen().getSharedPreferences())
                    .registerOnSharedPreferenceChangeListener(this);
            showHideHdrxSettings();
            setFramesSummary();
            setVersionDetails();
        }

        private void showHideHdrxSettings() {
            if (PreferenceKeys.isHdrXOn())
                removePreferenceFromScreen(mContext.getString(R.string.pref_category_jpg_key));
            else
                removePreferenceFromScreen(mContext.getString(R.string.pref_category_hdrx_key));
        }

        @NonNull
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            if (container != null) container.removeAllViews();
            return super.onCreateView(inflater, container, savedInstanceState);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            mRootView = view;
        }
        @HunterDebug
        @Override
        public void onResume() {
            Toolbar toolbar = activity.findViewById(R.id.settings_toolbar);
                toolbar.setTitle(getPreferenceScreen().getTitle());
                setHdrxTitle();
                checkEszdTheme();
                setTelegramPref();
                setGithubPref();
                setBackupPref();
                setRestorePref();
                setSupportedDevices();
                setProTitle();
                setThisDevice();
            super.onResume();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            getParentFragmentManager().beginTransaction().remove(SettingsFragment.this).commitAllowingStateLoss();
        }

        private void setTelegramPref() {
            AsyncTask.execute(()-> {
                Preference myPref = findPreference(PreferenceKeys.Key.KEY_TELEGRAM.mValue);
                if (myPref != null)
                    myPref.setOnPreferenceClickListener(preference -> {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/photon_camera_channel"));
                        startActivity(browserIntent);
                        return true;
                    });
            });
        }

        private void setGithubPref() {
            AsyncTask.execute(()-> {
            Preference github = findPreference(PreferenceKeys.Key.KEY_CONTRIBUTORS.mValue);
            if (github != null)
                github.setOnPreferenceClickListener(preference -> {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/eszdman/PhotonCamera"));
                    startActivity(browserIntent);
                    return true;
                });
            });
        }

        private void setRestorePref() {
                AsyncTask.execute(()-> {
            Preference restorePref = findPreference(mContext.getString(R.string.pref_restore_preferences_key));
            if (restorePref != null) {
                restorePref.setOnPreferenceChangeListener((preference, newValue) -> {
                    String restoreResult = BackupRestoreUtil.restorePreferences(mContext, newValue.toString());
                    Snackbar.make(mRootView, restoreResult, Snackbar.LENGTH_LONG).show();
                    return true;
                });
            }
          });
        }

        private void setBackupPref() {
            AsyncTask.execute(()-> {
                Preference backupPref = findPreference(mContext.getString(R.string.pref_backup_preferences_key));
                if (backupPref != null) {
                    backupPref.setOnPreferenceChangeListener((preference, newValue) -> {
                        String backupResult = BackupRestoreUtil.backupSettings(mContext, newValue.toString());
                        Snackbar.make(mRootView, backupResult, Snackbar.LENGTH_LONG).show();
                        return true;
                    });
                }
           });
        }
        @HunterDebug
        private void setSupportedDevices() {
            AsyncTask.execute(()-> {
                Preference preference = findPreference(PreferenceKeys.Key.ALL_DEVICES_NAMES_KEY.mValue);
                if (preference != null) {
                    preference.setSummary((mSettingsManager.getStringSet(PreferenceKeys.Key.DEVICES_PREFERENCE_FILE_NAME.mValue,
                            ALL_DEVICES_NAMES_KEY, Collections.singleton(mContext.getString(R.string.list_not_loaded)))
                            .stream().map(s -> s + "\n").reduce("\n", String::concat)));
                }
           });
        }

        private void setProTitle() {
                AsyncTask.execute(()-> {
                    Preference preference = findPreference(mContext.getString(R.string.pref_about_key));
                    if (preference != null && supportedDevice.isSupportedDevice()) {
                        preference.setTitle(R.string.device_support);
                    }
            });
        }

        private void setThisDevice() {
            Preference preference = findPreference(mContext.getString(R.string.pref_this_device_key));
            if (preference != null) {
                preference.setSummary(mContext.getString(R.string.this_device, SupportedDevice.THIS_DEVICE));
            }
        }

        private void removePreferenceFromScreen(String preferenceKey) {
            PreferenceScreen parentScreen = findPreference(SettingsFragment.KEY_MAIN_PARENT_SCREEN);
            if (parentScreen != null)
                if (parentScreen.findPreference(preferenceKey) != null) {
                    parentScreen.removePreference(Objects.requireNonNull(parentScreen.findPreference(preferenceKey)));
                }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(PreferenceKeys.Key.KEY_SAVE_PER_LENS_SETTINGS.mValue)) {
                setHdrxTitle();
                if (PreferenceKeys.isPerLensSettingsOn()) {
                    PreferenceKeys.loadSettingsForCamera(PreferenceKeys.getCameraID());
                    restartActivity();
                }
            }
            if (key.equalsIgnoreCase(PreferenceKeys.Key.KEY_THEME.mValue)) {
                restartActivity();
            }
            if (key.equalsIgnoreCase(PreferenceKeys.Key.KEY_THEME_ACCENT.mValue)) {
                checkEszdTheme();
                restartActivity();
                toRestartApp = true;
            }
            if (key.equalsIgnoreCase(PreferenceKeys.Key.KEY_SHOW_GRADIENT.mValue)) {
                toRestartApp = true;
            }
            if (key.equalsIgnoreCase(PreferenceKeys.Key.KEY_FRAME_COUNT.mValue)) {
                setFramesSummary();
            }
        }

        private void checkEszdTheme() {
            Preference p = findPreference(PreferenceKeys.Key.KEY_SHOW_GRADIENT.mValue);
            if (p != null)
                p.setEnabled(!mSettingsManager.getString(SCOPE_GLOBAL, PreferenceKeys.Key.KEY_THEME_ACCENT).equalsIgnoreCase("eszdman"));
        }

        private void setHdrxTitle() {
            Preference p = findPreference(mContext.getString(R.string.pref_category_hdrx_key));
            if (p != null) {
                if (PreferenceKeys.isPerLensSettingsOn()) {
                    p.setTitle(mContext.getString(R.string.hdrx) + "\t(Lens: " + PreferenceKeys.getCameraID() + ')');
                } else {
                    p.setTitle(mContext.getString(R.string.hdrx));
                }
            }
        }

        private void setFramesSummary() {
            Preference frameCountPreference = findPreference(PreferenceKeys.Key.KEY_FRAME_COUNT.mValue);
            if (frameCountPreference != null) {
                if (mSettingsManager.getInteger(PreferenceKeys.SCOPE_GLOBAL, PreferenceKeys.Key.KEY_FRAME_COUNT) == 1) {
                    frameCountPreference.setSummary(mContext.getString(R.string.unprocessed_raw));
                } else {
                    frameCountPreference.setSummary(mContext.getString(R.string.frame_count_summary));
                }
            }
        }

        private void restartActivity() {
            if (getActivity() != null) {
                Intent intent = new Intent(mContext, getActivity().getClass());
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent,
                        ActivityOptions.makeCustomAnimation(mContext, R.anim.fade_in, R.anim.fade_out).toBundle());
            }
        }

        private void setVersionDetails() {
            AsyncTask.execute(() -> {
                Preference about = findPreference(mContext.getString(R.string.pref_version_key));
                if (about != null) {
                    try {
                        PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
                        String versionName = packageInfo.versionName;
                        long versionCode = packageInfo.versionCode;

                        Date date = new Date(packageInfo.lastUpdateTime);
                        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss z", Locale.US);
                        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

                        about.setSummary(mContext.getString(R.string.version_summary, versionName + "." + versionCode, sdf.format(date)));

                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }

                }
            });

        }

        @Override
        public boolean onPreferenceTreeClick(@NonNull Preference preference) {
            return true;
        }

        @Override
        public void onDisplayPreferenceDialog(@NonNull Preference preference) {
            if (preference instanceof ResetPreferences) {
                DialogFragment dialogFragment = ResetPreferences.Dialog.newInstance(preference);
                dialogFragment.setTargetFragment(this, 0);
                dialogFragment.show(getParentFragmentManager(), null);
            } else {
                super.onDisplayPreferenceDialog(preference);
            }
        }

    }
}
