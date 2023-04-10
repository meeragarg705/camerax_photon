package com.particlesdevs.photoncamera.api;

import androidx.annotation.StringRes;

import com.particlesdevs.photoncamera.R;

import java.util.stream.Stream;

public enum CameraMode {

    PHOTO(R.string.mode_photo);

    int stringId;

    CameraMode(@StringRes int stringId) {
        this.stringId = stringId;
    }

    public static CameraMode valueOf(int modeOrdinal) {
        for (CameraMode mode : values()) {
            if (modeOrdinal == mode.ordinal()) {
                return mode;
            }
        }
        return PHOTO;
    }

    public static Integer[] nameIds() {
        return Stream.of(values()).map(mode -> mode.stringId).toArray(Integer[]::new);
    }

}
