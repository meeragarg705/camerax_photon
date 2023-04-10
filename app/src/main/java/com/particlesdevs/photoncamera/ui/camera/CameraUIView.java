package com.particlesdevs.photoncamera.ui.camera;

import android.view.View;

import com.particlesdevs.photoncamera.api.CameraMode;


public interface CameraUIView {

    void activateShutterButton(boolean status);


    void refresh(boolean processing);


    void setProcessingProgressBarIndeterminate(boolean indeterminate);


    void resetCaptureProgressBar();

    void incrementCaptureProgressBar(int step);

    void setCaptureProgressBarOpacity(float alpha);

    void setCaptureProgressMax(int max);


    void setCameraUIEventsListener(CameraUIEventsListener cameraUIEventsListener);

    void showFlashButton(boolean flashAvailable);

    void destroy();


    interface CameraUIEventsListener {
        void onClick(View v);

        void onCameraModeChanged(CameraMode cameraMode);

        void onPause();
    }
}
