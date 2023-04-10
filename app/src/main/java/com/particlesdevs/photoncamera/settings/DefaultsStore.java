
package com.particlesdevs.photoncamera.settings;

import java.util.HashMap;

class DefaultsStore {

    private static class Defaults {
        private final String mDefaultValue;
        private final String[] mPossibleValues;

        public Defaults(String defaultValue, String[] possibleValues) {
            mDefaultValue = defaultValue;
            mPossibleValues = possibleValues;
        }

        public String getDefaultValue() {
            return mDefaultValue;
        }

        public String[] getPossibleValues() {
            return mPossibleValues;
        }
    }


    private static final HashMap<String, Defaults> mDefaultsInternalStore = new HashMap<>();


    public void storeDefaults(String key, String defaultValue, String[] possibleValues) {
        Defaults defaults = new Defaults(defaultValue, possibleValues);
        mDefaultsInternalStore.put(key, defaults);
    }


    public String getDefaultValue(String key) {
        Defaults defaults = mDefaultsInternalStore.get(key);
        if (defaults == null) {
            return null;
        }
        return defaults.getDefaultValue();
    }

    
    public String[] getPossibleValues(String key) {
        Defaults defaults = mDefaultsInternalStore.get(key);
        if (defaults == null) {
            return null;
        }
        return defaults.getPossibleValues();
    }
}