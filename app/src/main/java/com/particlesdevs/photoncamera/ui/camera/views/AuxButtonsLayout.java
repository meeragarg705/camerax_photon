

package com.particlesdevs.photoncamera.ui.camera.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.particlesdevs.photoncamera.R;
import com.particlesdevs.photoncamera.ui.camera.data.CameraLensData;
import com.particlesdevs.photoncamera.ui.camera.model.AuxButtonsModel;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;


public class AuxButtonsLayout extends LinearLayout {


    private final HashMap<Integer, String> auxButtonsMap = new HashMap<>();

    private final LinearLayout.LayoutParams buttonParams;
    private AuxButtonListener auxButtonListener;
    private AuxButtonsModel auxButtonsModel;

    public AuxButtonsLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        int margin = (int) context.getResources().getDimension(R.dimen.aux_button_internal_margin);
        int size = (int) context.getResources().getDimension(R.dimen.aux_button_size);
        buttonParams = new LinearLayout.LayoutParams(size, size);
        buttonParams.setMargins(margin, margin, margin, margin);
    }

    private static String getAuxButtonName(float zoomFactor) {
        return String.format(Locale.US, "%.1fx", (zoomFactor - 0.049)).replace(".0", "");
    }

    public void setAuxButtonsModel(AuxButtonsModel auxButtonsModel) {
        this.auxButtonsModel = auxButtonsModel;
        auxButtonListener = auxButtonsModel.getAuxButtonListener();
    }

    public void setActiveId(String activeId) {
        refresh(activeId);
    }

    private void refresh(String cameraId) {
        if (!isFront(cameraId))
            this.setAuxButtons(auxButtonsModel.getBackCameras(), cameraId);
        else
            this.setAuxButtons(auxButtonsModel.getFrontCameras(), cameraId);
    }

    private boolean isFront(String cameraId) {
        return auxButtonsModel.getFrontCameras().stream().anyMatch(cameraLensData -> cameraLensData.getCameraId().equals(cameraId));
    }

    private void setAuxButtons(List<CameraLensData> cameraLensDataList, String activeId) {
        removeAllViews();
        auxButtonsMap.clear();
        cameraLensDataList.forEach(cameraLensData -> addNewButton(cameraLensData.getCameraId(), getAuxButtonName(cameraLensData.getZoomFactor())));
        setListenerAndSelected(activeId);
        updateVisibility();
    }

    private void setListenerAndSelected(String activeId) {
        View.OnClickListener auxButtonListener = this::onAuxButtonClick;
        for (int i = 0; i < getChildCount(); i++) {
            View button = getChildAt(i);
            button.setOnClickListener(auxButtonListener);
            if (activeId.equals(auxButtonsMap.get(button.getId())))
                button.setSelected(true);
        }
    }

    private void updateVisibility() {
        setVisibility(getChildCount() > 1 ? View.VISIBLE : View.INVISIBLE);
    }

    private void onAuxButtonClick(View view) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.setSelected(view.equals(child));
        }
        if (auxButtonListener != null)
            auxButtonListener.onAuxButtonClicked(auxButtonsMap.get(view.getId()));
    }

    private void addNewButton(String cameraId, String buttonText) {
        Button b = new Button(getContext());
        b.setLayoutParams(buttonParams);
        b.setText(buttonText);
        b.setTextAppearance(R.style.AuxButtonText);
        b.setBackgroundResource(R.drawable.aux_button_background);
        b.setStateListAnimator(null);
        b.setTransformationMethod(null);
        int buttonId = View.generateViewId();
        b.setId(buttonId);
        this.auxButtonsMap.put(buttonId, cameraId);
        addView(b);
    }

    public interface AuxButtonListener {
        void onAuxButtonClicked(String cameraId);
    }
}
