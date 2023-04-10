

package com.particlesdevs.photoncamera.ui.camera.views.settingsbar;

import com.particlesdevs.photoncamera.ui.camera.model.SettingsBarButtonModel;
import com.particlesdevs.photoncamera.ui.camera.model.SettingsBarEntryModel;

public interface SettingsBarListener {
    void onEntryUpdated(SettingsBarEntryModel settingsBarEntryModel, SettingsBarButtonModel buttonModel);
}
