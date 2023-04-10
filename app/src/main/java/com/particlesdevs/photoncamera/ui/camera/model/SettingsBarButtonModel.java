

package com.particlesdevs.photoncamera.ui.camera.model;

import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.StringRes;

public class SettingsBarButtonModel {
    private final int buttonDrawableId;
    private final int buttonStateNameStringId;
    private final int buttonValue;
    private final int id;
    private View.OnClickListener buttonClickListener;
    private boolean selected;


    private SettingsBarButtonModel(@IdRes int id, @DrawableRes int buttonDrawableId, @StringRes int buttonStateNameStringId, int buttonValue) {
        this.id = id;
        this.buttonDrawableId = buttonDrawableId;
        this.buttonStateNameStringId = buttonStateNameStringId;
        this.buttonValue = buttonValue;
    }

    public static SettingsBarButtonModel newButtonModel(@IdRes int id, @DrawableRes int buttonDrawableId, @StringRes int buttonStateNameStringId, int buttonValue, SettingsBarEntryModel entryModel) {
        SettingsBarButtonModel buttonModel = new SettingsBarButtonModel(id, buttonDrawableId, buttonStateNameStringId, buttonValue);
        buttonModel.setButtonClickListener(v -> entryModel.select(buttonModel));
        return buttonModel;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public int getButtonDrawableId() {
        return buttonDrawableId;
    }

    public int getButtonStateNameStringId() {
        return buttonStateNameStringId;
    }

    public int getButtonValue() {
        return buttonValue;
    }

    public View.OnClickListener getButtonClickListener() {
        return buttonClickListener;
    }

    public void setButtonClickListener(View.OnClickListener buttonClickListener) {
        this.buttonClickListener = buttonClickListener;
    }

    public int getId() {
        return id;
    }

}
