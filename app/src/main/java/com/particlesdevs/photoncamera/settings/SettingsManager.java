
package com.particlesdevs.photoncamera.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class SettingsManager {

    public static final String SCOPE_GLOBAL = "default_scope";
    private static final String TAG = "SettingsManager";
    private final Context mContext;
    private final String mPackageName;
    private final SharedPreferences mDefaultPreferences;
    private final DefaultsStore mDefaultsStore = new DefaultsStore();
    /**
     * A List of OnSettingChangedListener's, maintained to compare to new
     * listeners and prevent duplicate registering.
     */
    private final List<OnSettingChangedListener> mListeners =
            new ArrayList<>();
    /**
     * A List of OnSharedPreferenceChangeListener's, maintained to hold pointers
     * to actually registered listeners, so they can be unregistered.
     */
    private final List<OnSharedPreferenceChangeListener> mSharedPreferenceListeners =
            new ArrayList<>();
    private SharedPreferences mCustomPreferences;

    public SettingsManager(Context context) {
        mContext = context;
        mPackageName = mContext.getPackageName();
        mDefaultPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    /**
     * Package private conversion method to turn ints into preferred
     * String storage format.
     *
     * @param value int to be stored in Settings
     * @return String which represents the int
     */
    static String convert(int value) {
        return Integer.toString(value);
    }

    /**
     * Package private conversion method to turn booleans into preferred
     * String storage format.
     *
     * @param value boolean to be stored in Settings
     * @return String which represents the boolean
     */
    static String convert(boolean value) {
        return value ? "1" : "0";
    }

    /**
     * Get the SettingsManager's default preferences.  This is useful
     * to third party modules as they are defining their upgrade paths,
     * since most third party modules will use either SCOPE_GLOBAL or a
     * custom scope.
     */
    public SharedPreferences getDefaultPreferences() {
        return mDefaultPreferences;
    }

    /**
     * Open a SharedPreferences file by custom scope.
     * Also registers any known SharedPreferenceListeners on this
     * SharedPreferences instance.
     */
    protected SharedPreferences openPreferences(String scope) {
        SharedPreferences preferences = mContext.getSharedPreferences(
                mPackageName + scope, Context.MODE_PRIVATE);
        for (OnSharedPreferenceChangeListener listener : mSharedPreferenceListeners) {
            preferences.registerOnSharedPreferenceChangeListener(listener);
        }
        return preferences;
    }

    /**
     * Close a SharedPreferences file by custom scope.
     * The file isn't explicitly closed (the SharedPreferences API makes
     * this unnecessary), so the real work is to unregister any known
     * SharedPreferenceListeners from this SharedPreferences instance.
     * <p>
     * It's important to do this as camera and modules change, because
     * we don't want old SharedPreferences listeners executing on
     * cameras/modules they are not compatible with.
     */
    protected void closePreferences(SharedPreferences preferences) {
        for (OnSharedPreferenceChangeListener listener : mSharedPreferenceListeners) {
            preferences.unregisterOnSharedPreferenceChangeListener(listener);
        }
    }

    private OnSharedPreferenceChangeListener getSharedPreferenceListener(
            final OnSettingChangedListener listener) {
        return (sharedPreferences, key) -> listener.onSettingChanged(SettingsManager.this, key);
    }

    /**
     * Add an OnSettingChangedListener to the SettingsManager, which will
     * execute onSettingsChanged when any SharedPreference has been updated.
     */
    public void addListener(final OnSettingChangedListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("OnSettingChangedListener cannot be null.");
        }
        if (mListeners.contains(listener)) {
            return;
        }
        mListeners.add(listener);
        OnSharedPreferenceChangeListener sharedPreferenceListener =
                getSharedPreferenceListener(listener);
        mSharedPreferenceListeners.add(sharedPreferenceListener);
        mDefaultPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceListener);
        if (mCustomPreferences != null) {
            mCustomPreferences.registerOnSharedPreferenceChangeListener(
                    sharedPreferenceListener);
        }
        Log.v(TAG, "listeners: " + mListeners);
    }

    /**
     * Returns the SharedPreferences file matching the scope
     * argument.
     * <p>
     * Camera and module preferences files are cached,
     * until the camera id or module id changes, then the listeners
     * are unregistered and a new file is opened.
     */
    private SharedPreferences getPreferencesFromScope(String scope) {
        if (scope.equals(SCOPE_GLOBAL)) {
            return mDefaultPreferences;
        }
        if (mCustomPreferences != null) {
            closePreferences(mCustomPreferences);
        }
        mCustomPreferences = openPreferences(scope);
        return mCustomPreferences;
    }

    /**
     * Set default and valid values for a setting, for a String default and
     * a set of String possible values that are already defined.
     * This is not required.
     */
    public void setDefaults(PreferenceKeys.Key key, String defaultValue, String[] possibleValues) {
        mDefaultsStore.storeDefaults(key.mValue, defaultValue, possibleValues);
    }


    /**
     * Retrieve a default from the DefaultsStore as a String.
     */
    public String getStringDefault(PreferenceKeys.Key key) {
        return mDefaultsStore.getDefaultValue(key.mValue);
    }

    /**
     * Retrieve a default from the DefaultsStore as an Integer.
     */
    public Integer getIntegerDefault(PreferenceKeys.Key key) {
        String defaultValueString = mDefaultsStore.getDefaultValue(key.mValue);
        return defaultValueString == null ? 0 : Integer.parseInt(defaultValueString);
    }

    /**
     * Retrieve a default from the DefaultsStore as a Float.
     */
    public Float getFloatDefault(PreferenceKeys.Key key) {
        String defaultValueString = mDefaultsStore.getDefaultValue(key.mValue);
        return defaultValueString == null ? 0.0f : Float.parseFloat(defaultValueString);
    }

    /**
     * Retrieve a default from the DefaultsStore as a boolean.
     */
    public boolean getBooleanDefault(PreferenceKeys.Key key) {
        String defaultValueString = mDefaultsStore.getDefaultValue(key.mValue);
        return defaultValueString != null && (Integer.parseInt(defaultValueString) != 0);
    }

    /**
     * Retrieve a setting's value as a String, manually specifiying
     * a default value.
     */
    public String getString(String scope, PreferenceKeys.Key key, String defaultValue) {
        SharedPreferences preferences = getPreferencesFromScope(scope);
        return preferences.getString(key.mValue, defaultValue);
    }

    public String getString(String scope, String key, String defaultValue) {
        SharedPreferences preferences = getPreferencesFromScope(scope);
        return preferences.getString(key, defaultValue);
    }

    public Set<String> getStringSet(String scope, PreferenceKeys.Key key, Set<String> defaultValue) {
        SharedPreferences preferences = getPreferencesFromScope(scope);
        return preferences.getStringSet(key.mValue, defaultValue);
    }

    public ArrayList<String> getArrayList(String scope, String key, Set<String> defaultValue) {
        SharedPreferences preferences = getPreferencesFromScope(scope);
        return new ArrayList<>(preferences.getStringSet(key, defaultValue));
    }

    /**
     * Retrieve a setting's value as a String, using the default value
     * stored in the DefaultsStore.
     */
    public String getString(String scope, PreferenceKeys.Key key) {
        return getString(scope, key, getStringDefault(key));
    }

    /**
     * Retrieve a setting's value as an Integer, manually specifiying
     * a default value.
     */
    public Integer getInteger(String scope, PreferenceKeys.Key key, Integer defaultValue) {
        String defaultValueString = Integer.toString(defaultValue);
        String value = getString(scope, key, defaultValueString);
        return Integer.parseInt(value);
    }

    /**
     * Retrieve a setting's value as an Integer, converting the default value
     * stored in the DefaultsStore.
     */
    public Integer getInteger(String scope, PreferenceKeys.Key key) {
        return getInteger(scope, key, getIntegerDefault(key));
    }

    /**
     * Retrieve a setting's value as a Float, manually specifiying
     * a default value.
     */
    public Float getFloat(String scope, PreferenceKeys.Key key, Float defaultValue) {
        String defaultValueString = Float.toString(defaultValue);
        String value = getString(scope, key, defaultValueString);
        return Float.parseFloat(value);
    }

    /**
     * Retrieve a setting's value as a Float, converting the default value
     * stored in the DefaultsStore.
     */
    public Float getFloat(String scope, PreferenceKeys.Key key) {
        return getFloat(scope, key, getFloatDefault(key));
    }

    /**
     * Retrieve a setting's value as a boolean, manually specifiying
     * a default value.
     */
    public boolean getBoolean(String scope, PreferenceKeys.Key key, boolean defaultValue) {
        String defaultValueString = defaultValue ? "1" : "0";
        String value = getString(scope, key, defaultValueString);
        return (Integer.parseInt(value) != 0);
    }

    public boolean getBoolean(String scope, String key, boolean defaultValue) {
        String defaultValueString = defaultValue ? "1" : "0";
        String value = getString(scope, key, defaultValueString);
        return (Integer.parseInt(value) != 0);
    }

    /**
     * Retrieve a setting's value as a boolean, converting the default value
     * stored in the DefaultsStore.
     */
    public boolean getBoolean(String scope, PreferenceKeys.Key key) {
        return getBoolean(scope, key, getBooleanDefault(key));
    }


    /**
     * Store a setting's value using a String value.  No conversion
     * occurs before this value is stored in SharedPreferences.
     */
    public void set(String scope, PreferenceKeys.Key key, String value) {
        SharedPreferences preferences = getPreferencesFromScope(scope);
        preferences.edit().putString(key.mValue, value).apply();
    }

    public void setInitial(String scope, PreferenceKeys.Key key, String value) {
        if (!isSet(scope, key)) {
            SharedPreferences preferences = getPreferencesFromScope(scope);
            preferences.edit().putString(key.mValue, value).apply();
        }
    }

    public void set(String scope, String key, String value) {
        SharedPreferences preferences = getPreferencesFromScope(scope);
        preferences.edit().putString(key, value).apply();
    }

    public void setInitial(String scope, String key, String value) {
        if (!isSet(scope, key)) {
            set(scope, key, value);
        }
    }

    /**
     * Store a setting's value using an Integer value.  Type conversion
     * to String occurs before this value is stored in SharedPreferences.
     */
    public void set(String scope, PreferenceKeys.Key key, int value) {
        set(scope, key, convert(value));
    }

    public void setInitial(String scope, PreferenceKeys.Key key, int value) {
        if (!isSet(scope, key)) {
            set(scope, key, convert(value));
        }
    }

    /**
     * Store a setting's value using a boolean value.  Type conversion
     * to an Integer and then to a String occurs before this value is
     * stored in SharedPreferences.
     */
    public void set(String scope, PreferenceKeys.Key key, boolean value) {
        set(scope, key, convert(value));
    }

    public void set(String scope, String key, boolean value) {
        set(scope, key, convert(value));
    }

    public void set(String scope, PreferenceKeys.Key key, Set<String> value) {
        SharedPreferences preferences = getPreferencesFromScope(scope);
        preferences.edit().putStringSet(key.mValue, value).apply();
    }

    public void set(String scope, String key, ArrayList<String> value) {
        SharedPreferences preferences = getPreferencesFromScope(scope);
        preferences.edit().putStringSet(key, new HashSet<>(value)).apply();
    }

    public void setInitial(String scope, PreferenceKeys.Key key, boolean value) {
        if (!isSet(scope, key)) {
            set(scope, key, convert(value));
        }
    }


    /**
     * Check that a setting has some value stored.
     */
    public boolean isSet(String scope, PreferenceKeys.Key key) {
        SharedPreferences preferences = getPreferencesFromScope(scope);
        return preferences.contains(key.mValue);
    }

    public boolean isSet(String scope, String key) {
        SharedPreferences preferences = getPreferencesFromScope(scope);
        return preferences.contains(key);
    }


    /**
     * Remove a setting.
     */
    public void remove(String scope, PreferenceKeys.Key key) {
        SharedPreferences preferences = getPreferencesFromScope(scope);
        preferences.edit().remove(key.mValue).apply();
    }

    /**
     * Interface with Camera Device Settings and Modules.
     */
    public interface OnSettingChangedListener {
        /**
         * Called every time a SharedPreference has been changed.
         */
        void onSettingChanged(SettingsManager settingsManager, String key);
    }
}