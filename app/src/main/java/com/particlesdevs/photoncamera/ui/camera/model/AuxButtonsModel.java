

package com.particlesdevs.photoncamera.ui.camera.model;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

import com.particlesdevs.photoncamera.BR;
import com.particlesdevs.photoncamera.ui.camera.data.CameraLensData;
import com.particlesdevs.photoncamera.ui.camera.views.AuxButtonsLayout;

import java.util.List;

public class AuxButtonsModel extends BaseObservable {
    private List<CameraLensData> frontCameras;
    private List<CameraLensData> backCameras;
    private String currentCameraId;
    private AuxButtonsLayout.AuxButtonListener auxButtonListener;

    public AuxButtonsLayout.AuxButtonListener getAuxButtonListener() {
        return auxButtonListener;
    }

    public void setAuxButtonListener(AuxButtonsLayout.AuxButtonListener auxButtonListener) {
        this.auxButtonListener = auxButtonListener;
    }

    public List<CameraLensData> getFrontCameras() {
        return frontCameras;
    }

    public void setFrontCameras(List<CameraLensData> frontCameras) {
        this.frontCameras = frontCameras;
    }

    public List<CameraLensData> getBackCameras() {
        return backCameras;
    }

    public void setBackCameras(List<CameraLensData> backCameras) {
        this.backCameras = backCameras;
    }

    @Bindable
    public String getCurrentCameraId() {
        return currentCameraId;
    }

    public void setCurrentCameraId(String currentCameraId) {
        this.currentCameraId = currentCameraId;
        notifyPropertyChanged(BR.currentCameraId);
    }
}
