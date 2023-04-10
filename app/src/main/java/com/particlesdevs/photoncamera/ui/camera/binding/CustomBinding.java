package com.particlesdevs.photoncamera.ui.camera.binding;

import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.BindingAdapter;

import com.particlesdevs.photoncamera.R;
import com.particlesdevs.photoncamera.ui.camera.model.AuxButtonsModel;
import com.particlesdevs.photoncamera.ui.camera.model.CameraFragmentModel;
import com.particlesdevs.photoncamera.ui.camera.views.AuxButtonsLayout;


public class CustomBinding {


    @BindingAdapter("bindRotate")
    public static void rotateView(View view, CameraFragmentModel model) {
        if (model != null)
            view.animate().rotation(model.getOrientation()).setDuration(model.getDuration()).start();
    }


    @BindingAdapter("bindViewGroupChildrenRotate")
    public static void rotateAuxButtons(ViewGroup viewGroup, CameraFragmentModel model) {
        if (model != null) {
            int orientation = model.getOrientation();
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                viewGroup.getChildAt(i).animate().rotation(orientation).setDuration(model.getDuration()).start();
            }
        }
    }


    @BindingAdapter("android:selected")
    public static void setSelected(View view, Boolean selected) {
        if (selected != null && view != null) {
            view.setSelected(selected);
        }
    }


    @BindingAdapter("selectViewIdInViewGroup")
    public static void selectViewIdInViewGroup(ViewGroup viewGroup, int viewID) {
        if (viewGroup != null) {
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                viewGroup.getChildAt(i).setSelected(viewGroup.getChildAt(i).getId() == viewID);
            }
        }
    }

    @BindingAdapter("settingsBarVisibility")
    public static void toggleSettingsBarVisibility(ViewGroup viewGroup, boolean visible) {
        if (viewGroup != null)
            if (visible)
                viewGroup.post(() -> {
                    viewGroup.animate().setDuration(200).alpha(1).translationY(0).scaleX(1).scaleY(1).start();
                    viewGroup.setVisibility(View.VISIBLE);
                });
            else
                viewGroup.post(() -> viewGroup.animate().setDuration(200).alpha(0).translationY(-viewGroup.getResources().getDimension(R.dimen.standard_125))
                        .scaleX(0).scaleY(0).withEndAction(() -> viewGroup.setVisibility(View.INVISIBLE))
                        .start());
    }

    @BindingAdapter("setAuxButtonModel")
    public static void setAuxButtonModel(AuxButtonsLayout layout, AuxButtonsModel auxButtonsModel) {
        if (auxButtonsModel != null)
            layout.setAuxButtonsModel(auxButtonsModel);
    }

    @BindingAdapter("setActiveId")
    public static void setActiveCameraId(AuxButtonsLayout layout, String cameraId) {
        if (cameraId != null)
            layout.setActiveId(cameraId);
    }

    @BindingAdapter("layoutMarginTop")
    public static void setLayoutMarginTop(View view, float margin) {
        ViewGroup.MarginLayoutParams layoutParams = ((ViewGroup.MarginLayoutParams) view.getLayoutParams());
        layoutParams.topMargin = (int) margin;
        view.setLayoutParams(layoutParams);
    }

    @BindingAdapter("adjustCameraContainer")
    public static void adjustCameraContainer(ConstraintLayout cameraContainer, float displayAspectRatio) {
        if (displayAspectRatio <= 16f / 9) {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) cameraContainer.getLayoutParams();
            params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
            params.topToBottom = ConstraintLayout.LayoutParams.UNSET;
        }
    }

    @BindingAdapter("adjustTopBar")
    public static void adjustTopBar(View topbar, float displayAspectRatio) {
        if (displayAspectRatio > 16f / 9) {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) topbar.getLayoutParams();
            DisplayMetrics displayMetrics = topbar.getResources().getDisplayMetrics();
            float dpHeight = displayMetrics.heightPixels / displayMetrics.density;
            float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
            float dpmargin = (dpHeight - (dpWidth / 9f * 16f));
            params.topMargin = (int) dpmargin;
        }
    }
}
