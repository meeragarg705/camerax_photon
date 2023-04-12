package com.particlesdevs.photoncamera.circularbarlib.control;

import java.util.Observable;


public class ManualParamModel extends Observable {
    public static final double EXPOSURE_AUTO = 0;
    public static final double EV_AUTO = 0;
    public static final double ISO_AUTO = 0;
    public static final double FOCUS_AUTO = -1.0d;
    public static final String ID_FOCUS = "focus";
    public static final String ID_EV = "ev";
    public static final String ID_SHUTTER = "shutter";
    public static final String ID_ISO = "iso";
    private double currentFocusValue;
    private double currentEvValue;
    private double currentExposureValue;
    private double currentISOValue=200;

    public ManualParamModel() {
    }

    public double getCurrentFocusValue() {
        this.currentFocusValue = 0;
        return currentFocusValue;
    }

    public void setCurrentFocusValue(double currentFocusValue) {
        this.currentFocusValue = currentFocusValue;
        notifyObservers(ID_FOCUS);
    }

    public double getCurrentEvValue() {
        return currentEvValue;
    }

    public void setCurrentEvValue(double currentEvValue) {
        this.currentEvValue = currentEvValue;
        notifyObservers(ID_EV);
    }

    public double getCurrentExposureValue() {
        return currentExposureValue;
    }

    public void setCurrentExposureValue(double currentExposureValue) {
        this.currentExposureValue = currentExposureValue;
        notifyObservers(ID_SHUTTER);
    }

    public double getCurrentISOValue() {

        /*
        Set ISO value here;

         */

        return currentISOValue;
    }

    public void setCurrentISOValue(double currentISOValue) {
        this.currentISOValue = currentISOValue;
        notifyObservers(ID_ISO);
    }

    public boolean isManualMode() {
        return !(getCurrentExposureValue() == EXPOSURE_AUTO
                && getCurrentFocusValue() == FOCUS_AUTO
                && getCurrentISOValue() == ISO_AUTO
                && getCurrentEvValue() == EV_AUTO);
    }

    public void reset() {
        currentFocusValue = FOCUS_AUTO;
        currentEvValue = EV_AUTO;
        currentExposureValue = EXPOSURE_AUTO;
        currentISOValue = ISO_AUTO;
    }

    @Override
    public void notifyObservers(Object arg) {
        setChanged();
        super.notifyObservers(arg);
    }
}
