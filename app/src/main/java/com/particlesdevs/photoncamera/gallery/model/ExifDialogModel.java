package com.particlesdevs.photoncamera.gallery.model;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.databinding.library.baseAdapters.BR;

import com.particlesdevs.photoncamera.gallery.views.Histogram;


public class ExifDialogModel extends BaseObservable {
    private String title;
    private String res;
    private String res_mp;
    private String device;
    private String date;
    private String exposure;
    private String iso;
    private String fnum;
    private String focal;
    private String file_size;
    private String miniText;
    private Histogram.HistogramModel histogramModel;

    public String getMiniText() {
        return miniText;
    }

    public void setMiniText(String miniText) {
        this.miniText = miniText;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getRes() {
        return res;
    }

    public void setRes(String res) {
        this.res = res;
    }

    public String getRes_mp() {
        return res_mp;
    }

    public void setRes_mp(String res_mp) {
        this.res_mp = res_mp;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getFile_size() {
        return file_size;
    }

    public void setFile_size(String file_size) {
        this.file_size = file_size;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getExposure() {
        return exposure;
    }

    public void setExposure(String exposure) {
        this.exposure = exposure;
    }

    public String getIso() {
        return iso;
    }

    public void setIso(String iso) {
        this.iso = iso;
    }

    public String getFnum() {
        return fnum;
    }

    public void setFnum(String fnum) {
        this.fnum = fnum;
    }

    public String getFocal() {
        return focal;
    }

    public void setFocal(String focal) {
        this.focal = focal;
    }

    @Bindable
    public Histogram.HistogramModel getHistogramModel() {
        return histogramModel;
    }

    public void setHistogramModel(Histogram.HistogramModel histogramModel) {
        this.histogramModel = histogramModel;
        notifyPropertyChanged(BR.histogramModel);
    }
}
