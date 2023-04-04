package com.particlesdevs.photoncamera.gallery.ui.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.particlesdevs.photoncamera.R;
import com.particlesdevs.photoncamera.databinding.FragmentGalleryImageViewerBinding;
import com.particlesdevs.photoncamera.gallery.adapters.DepthPageTransformer;
import com.particlesdevs.photoncamera.gallery.adapters.ImageAdapter;
import com.particlesdevs.photoncamera.gallery.adapters.ImageGridAdapter;
import com.particlesdevs.photoncamera.gallery.compare.SSIVListener;
import com.particlesdevs.photoncamera.gallery.files.GalleryFileOperations;
import com.particlesdevs.photoncamera.gallery.files.ImageFile;
import com.particlesdevs.photoncamera.gallery.helper.Constants;
import com.particlesdevs.photoncamera.gallery.model.GalleryItem;
import com.particlesdevs.photoncamera.gallery.viewmodel.ExifDialogViewModel;
import com.particlesdevs.photoncamera.gallery.viewmodel.GalleryViewModel;
import com.particlesdevs.photoncamera.processing.ImagePath;
import com.particlesdevs.photoncamera.util.AwsUtils;


import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import io.reactivex.schedulers.Schedulers;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;


public class ImageViewerFragment extends Fragment {
    private static final String TAG = ImageViewerFragment.class.getSimpleName();
    private List<GalleryItem> galleryItems;
    private ExifDialogViewModel exifDialogViewModel;
    private ViewPager viewPager;
    private RecyclerView linearRecyclerView;
    private ImageAdapter adapter;
    private ImageGridAdapter linearGridAdapter;
    private NavController navController;
    private FragmentGalleryImageViewerBinding fragmentGalleryImageViewerBinding;
    private boolean isExifVisible;
    private String mode;
    static TransferUtility transferUtility;
    // A List of all transfers
    static List<TransferObserver> observers;
    static ArrayList<HashMap<String, Object>> transferRecordMaps;
    // Reference to the utility class
    static AwsUtils awsUtils;
    private SSIVListener ssivListener = new SSIVListener() {
        @Override
        public void onScaleChanged(float newScale, int origin) {
            updateScaleText();
        }

        @Override
        public void onCenterChanged(PointF newCenter, int origin) {

        }

        @Override
        public void onTouched(int id) {

        }
    };
    private int indexToDelete = -1;
    private GalleryViewModel viewModel;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        fragmentGalleryImageViewerBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_gallery_image_viewer, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(GalleryViewModel.class);
        initialiseDataMembers();
        getAwsData();
        setClickListeners();
        return fragmentGalleryImageViewerBinding.getRoot();
    }

    private void getAwsData() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());

        String pool_id = pref.getString("aws_pool_id_config", "");
        String region_id = pref.getString("aws_region_id_config", "");
        if (pool_id == "") {
            Toast.makeText(getContext(), "AWS Configuration Not Found", Toast.LENGTH_SHORT);
        } else {
            awsUtils = new AwsUtils();
            transferUtility = AwsUtils.getTransferUtility(getContext());
            transferRecordMaps = new ArrayList<>();
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        getParentFragmentManager().beginTransaction().remove((Fragment) ImageViewerFragment.this).commitAllowingStateLoss();
        fragmentGalleryImageViewerBinding = null;
    }

    private void initialiseDataMembers() {
        viewPager = fragmentGalleryImageViewerBinding.viewPager;
        linearRecyclerView = fragmentGalleryImageViewerBinding.bottomControlsContainer.scrollingGalleryView;
        exifDialogViewModel = new ViewModelProvider(this).get(ExifDialogViewModel.class);
        fragmentGalleryImageViewerBinding.exifLayout.setExifmodel(exifDialogViewModel.getExifDataModel());
        fragmentGalleryImageViewerBinding.setExifmodel(exifDialogViewModel.getExifDataModel());
        navController = NavHostFragment.findNavController(this);
        initImageAdapter(viewModel.getAllImageFilesData().getValue());
        initLinearRecyclerAdapter(viewModel.getAllImageFilesData().getValue());
    }

    private void initImageAdapter(List<GalleryItem> galleryItems) {
        if (galleryItems != null) {
            this.galleryItems = galleryItems;
            adapter = new ImageAdapter(this.galleryItems);
            adapter.setImageViewClickListener(ImageViewerFragment.this::onImageViewClicked);
            if (ssivListener != null) {
                adapter.setSsivListener(ssivListener);
            }
            adapter.setImageEventListener(new SubsamplingScaleImageView.DefaultOnImageEventListener() {
                @Override
                public void onReady() {
                    updateScaleText();
                }
            });
            viewPager.setAdapter(adapter);
        }
    }

    private void initLinearRecyclerAdapter(List<GalleryItem> galleryItems) {
        if (galleryItems != null) {
            linearGridAdapter = new ImageGridAdapter(galleryItems, Constants.GALLERY_ITEM_TYPE_LINEAR);
            fragmentGalleryImageViewerBinding.bottomControlsContainer.scrollingGalleryView.setAdapter(linearGridAdapter);
            linearGridAdapter.setGridAdapterCallback(new ImageGridAdapter.GridAdapterCallback() {
                @Override
                public void onItemClicked(int position, View view, GalleryItem galleryItem) {
                    viewPager.setCurrentItem(position, true);
                    LinearLayoutManager linearLayoutManager = (LinearLayoutManager) linearRecyclerView.getLayoutManager();
                    if (linearLayoutManager != null) {
                        int avg = (linearLayoutManager.findFirstCompletelyVisibleItemPosition() + (linearLayoutManager.findFirstCompletelyVisibleItemPosition() + 1) +
                                linearLayoutManager.findLastCompletelyVisibleItemPosition()) / 3;
                        if (position > avg)
                            linearRecyclerView.smoothScrollToPosition(position + 1);
                        else if (position != 0)
                            linearRecyclerView.smoothScrollToPosition(position - 1);
                        else
                            linearRecyclerView.smoothScrollToPosition(0);

                    }
                }

                @Override
                public void onImageSelectionChanged(int numOfSelectedFiles) {
                    //Not implemented
                }

                @Override
                public void onImageSelectionStopped() {
                    //Not implemented
                }
            });
        }
    }

    private void setClickListeners() {
        fragmentGalleryImageViewerBinding.bottomControlsContainer.setOnShare(this::onShareButtonClick);
        fragmentGalleryImageViewerBinding.bottomControlsContainer.setOnDelete(this::onDeleteButtonClick);
        fragmentGalleryImageViewerBinding.bottomControlsContainer.setOnExif(this::onExifButtonClick);
        fragmentGalleryImageViewerBinding.bottomControlsContainer.setOnShare(this::onShareButtonClick);
        fragmentGalleryImageViewerBinding.bottomControlsContainer.setOnEdit(this::onEditButtonClick);
        fragmentGalleryImageViewerBinding.topControlsContainer.setOnGallery(this::onGalleryButtonClick);
        fragmentGalleryImageViewerBinding.topControlsContainer.setOnBack(this::onBack);
        fragmentGalleryImageViewerBinding.topControlsContainer.setOnQuickCompare(this::onQuickCompare);
        fragmentGalleryImageViewerBinding.exifLayout.histogramView.setHistogramLoadingListener(this::isHistogramLoading);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewPager.setPageTransformer(true, new DepthPageTransformer());
        viewPager.setOffscreenPageLimit(3);
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                updateExif();
                updateScaleText();
                linearRecyclerView.smoothScrollToPosition(position);
            }
        });
        updateExif();
        Bundle bundle = getArguments();
        if (bundle != null) {
            mode = bundle.getString(Constants.MODE_KEY);
            viewPager.setCurrentItem(bundle.getInt(Constants.IMAGE_POSITION_KEY, 0));
            linearRecyclerView.scrollToPosition(bundle.getInt(Constants.IMAGE_POSITION_KEY, 0));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
//        if (isCompareMode()) {
        fragmentGalleryImageViewerBinding.setMiniExifVisible(!fragmentGalleryImageViewerBinding.getButtonsVisible());
//        }
    }

    public void setSsivListener(SSIVListener ssivListener) {
        this.ssivListener = ssivListener;
    }

    public ImageAdapter.CustomSSIV getCurrentSSIV() {
        return viewPager.findViewById(adapter.getSsivId(viewPager.getCurrentItem()));
    }

    private void onBack(View view) {
        getActivity().finish();
    }

    private void onQuickCompare(View view) {
        if (galleryItems.size() >= 2) {
            NavController navController = Navigation.findNavController(view);
            Bundle b = new Bundle(2);
            int image1pos = viewPager.getCurrentItem();
            int image2pos = image1pos + 1;
            if (image1pos == galleryItems.size() - 1) {
                image2pos = image1pos;
                image1pos -= 1;
            }
            b.putInt(Constants.IMAGE1_KEY, image1pos);
            b.putInt(Constants.IMAGE2_KEY, image2pos);
            navController.navigate(R.id.action_imageViewerFragment_to_imageCompareFragment, b);
        } else {
            Toast.makeText(getContext(), "No images to compare!", Toast.LENGTH_SHORT).show();
        }
    }

    private void onGalleryButtonClick(View view) {
        if (navController.getPreviousBackStackEntry() == null)
            navController.navigate(R.id.action_imageViewFragment_to_imageLibraryFragment);
        else navController.navigateUp();
    }

    private void onEditButtonClick(View view) {
        int position = viewPager.getCurrentItem();
        if (galleryItems != null && getContext() != null) {
            GalleryItem galleryItem = galleryItems.get(position);
            String fileName = galleryItem.getFile().getDisplayName();
            String mediaType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(FileUtils.getExtension(fileName));
            Uri uri = galleryItem.getFile().getFileUri();
            Intent editIntent = new Intent(Intent.ACTION_EDIT);
            editIntent.setDataAndType(uri, mediaType);
            String outPutFileUri = galleryItem.getFile().getFileUri().toString().replace(galleryItem.getFile().getDisplayName(), ImagePath.generateNewFileName() + '.' + FileUtils.getExtension(fileName));
            editIntent.putExtra(MediaStore.EXTRA_OUTPUT, outPutFileUri);
            editIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent chooser = Intent.createChooser(editIntent, null);
            startActivityForResult(chooser, Constants.REQUEST_EDIT_IMAGE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_EDIT_IMAGE) {
            if (resultCode == Activity.RESULT_CANCELED) {
//                if (newEditedFile != null) {
//                    if (newEditedFile.exists() && newEditedFile.length() == 0)
//                        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + ")->Dummy file deleted : " + newEditedFile.delete());
//                }
            }
            if (resultCode == Activity.RESULT_OK) {
                if (data != null && data.getData() != null) {
                    String savedFilePath = data.getData().getPath();
                    Toast.makeText(getContext(), "Saved : " + savedFilePath, Toast.LENGTH_LONG).show();
                    viewModel.fetchAllImages();
                    initImageAdapter(viewModel.getAllImageFilesData().getValue());
                    refreshLinearGridAdapter(viewModel.getAllImageFilesData().getValue());
                    updateExif();
                }
            }
            //            Log.d(TAG, "onActivityResult(): requestCode = [" + requestCode + "], resultCode = [" + resultCode + "], data = [" + data + "]");
        }
    }

    private void refreshLinearGridAdapter(List<GalleryItem> galleryItems) {
        linearGridAdapter.setGalleryItemList(galleryItems);
        linearGridAdapter.notifyDataSetChanged();
    }

    private void onDeleteButtonClick(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage(R.string.sure_delete).setTitle(android.R.string.dialog_alert_title).setIcon(R.drawable.ic_delete).setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    indexToDelete = viewPager.getCurrentItem();
                    GalleryFileOperations.deleteImageFiles(getActivity(), Collections.singletonList((ImageFile) galleryItems.get(indexToDelete).getFile()), this::handleImagesDeletedCallback);
                });
        builder.create().show();
    }

    private void onShareButtonClick(View view) {
        int position = viewPager.getCurrentItem();
        GalleryItem galleryItem = galleryItems.get(position);
        String fileName = galleryItem.getFile().getDisplayName();
        String mediaType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(FileUtils.getExtension(fileName));
        Uri uri = galleryItem.getFile().getFileUri();
        showBottomSheetDialog(uri);

    }

    private void beginUpload(File file) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());

        String bucketName = pref.getString("aws_bucket_name", "");

        if (bucketName != "") {
            TransferObserver uploadObserver =
                    transferUtility.upload(bucketName, file.getName(), file);

            uploadObserver.setTransferListener(new UploadListener(getContext()));
        } else {
            Log.d("AWS CONFIG", "Not Found");
            Toast.makeText(getContext(), "Aws Configuration Not Found!", Toast.LENGTH_SHORT).show();
        }


    }

    private File readContentToFile(Uri uri) throws IOException {
        final File file = new File(getContext().getCacheDir(), getDisplayName(uri));
        try (
                final InputStream in = getContext().getContentResolver().openInputStream(uri);
                final OutputStream out = new FileOutputStream(file, false);
        ) {
            byte[] buffer = new byte[1024];
            for (int len; (len = in.read(buffer)) != -1; ) {
                out.write(buffer, 0, len);
            }
            return file;
        }
    }

    private String getDisplayName(Uri uri) {
        final String[] projection = {MediaStore.Images.Media.DISPLAY_NAME};
        try (
                Cursor cursor = getContext().getContentResolver().query(uri, projection, null, null, null);
        ) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
            if (cursor.moveToFirst()) {
                return cursor.getString(columnIndex);
            }
        }
        // If the display name is not found for any reason, use the Uri path as a fallback.
        Log.w(TAG, "Couldnt determine DISPLAY_NAME for Uri.  Falling back to Uri path: " + uri.getPath());
        return uri.getPath();
    }

    private void showBottomSheetDialog(Uri uri) {


        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_dialog_layout);

//        LinearLayout copy = bottomSheetDialog.findViewById(R.id.copyLinearLayout);
        LinearLayout shareAws = bottomSheetDialog.findViewById(R.id.shareLinearLayout);
        LinearLayout uploadGoogle = bottomSheetDialog.findViewById(R.id.uploadLinearLayout);
        LinearLayout download = bottomSheetDialog.findViewById(R.id.download);
//        LinearLayout delete = bottomSheetDialog.findViewById(R.id.delete);

        bottomSheetDialog.show();


        shareAws.setOnClickListener(v -> {
//            Toast.makeText(getContext(), "Amazon S3 is Clicked", Toast.LENGTH_LONG).show();
            File file = null;
            try {
                file = readContentToFile(uri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            bottomSheetDialog.dismiss();
            beginUpload(file);


        });

        assert uploadGoogle != null;
        uploadGoogle.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Google Photos is Clicked", Toast.LENGTH_LONG).show();
            bottomSheetDialog.dismiss();
        });

        assert download != null;
        download.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Download is Clicked", Toast.LENGTH_LONG).show();
            bottomSheetDialog.dismiss();
        });

    }

    private void onExifButtonClick(View view) {
        isExifVisible = !isExifVisible;
        fragmentGalleryImageViewerBinding.setExifDialogVisible(isExifVisible);
        updateExif();
    }

    private void onImageViewClicked(View view) {
        if (isCompareMode()) {
            onExifButtonClick(null);
            fragmentGalleryImageViewerBinding.setMiniExifVisible(!isExifVisible);
        } else {
            fragmentGalleryImageViewerBinding.setButtonsVisible(!fragmentGalleryImageViewerBinding.getButtonsVisible());
            fragmentGalleryImageViewerBinding.setMiniExifVisible(!fragmentGalleryImageViewerBinding.getButtonsVisible());
            if (isExifVisible) {
                fragmentGalleryImageViewerBinding.setExifDialogVisible(fragmentGalleryImageViewerBinding.getButtonsVisible());
                updateExif();
            }
        }
    }

    public void updateScaleText() {
        SubsamplingScaleImageView view = getCurrentSSIV();
        if (view != null) {
            fragmentGalleryImageViewerBinding.setScale(String.format(Locale.ROOT, "%.0f%%", (view.getScale() * 100)));
        }
    }

    public void resetScaleText() {
        fragmentGalleryImageViewerBinding.setScale("");
    }

    private void updateExif() {
        int position = viewPager.getCurrentItem();
        if (galleryItems.size() > 0) {
            GalleryItem galleryItem = galleryItems.get(position);
            exifDialogViewModel.updateModel(requireContext().getContentResolver(), galleryItem.getFile());
            if (fragmentGalleryImageViewerBinding.getExifDialogVisible()) {
                exifDialogViewModel.updateHistogramView((ImageFile) galleryItem.getFile());
            }
        }
    }

    private void isHistogramLoading(boolean loading) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (loading) {
                fragmentGalleryImageViewerBinding.exifLayout.histoLoading.setVisibility(View.VISIBLE);
            } else {
                fragmentGalleryImageViewerBinding.exifLayout.histoLoading.setVisibility(View.INVISIBLE);
            }
        });

    }

    private boolean isCompareMode() {
        return mode != null && mode.equalsIgnoreCase(Constants.COMPARE);
    }

    public void handleImagesDeletedCallback(boolean isDeleted) {
        if (isDeleted && indexToDelete >= 0) {
            galleryItems.remove(indexToDelete);
            initImageAdapter(galleryItems);
            refreshLinearGridAdapter(galleryItems);
            //auto scroll to the next photo
            viewPager.setCurrentItem(indexToDelete, true);
            updateExif();
            Toast.makeText(getContext(), R.string.image_deleted, Toast.LENGTH_SHORT).show();
            indexToDelete = -1;
        } else {
            Toast.makeText(getContext(), "Deletion Failed!", Toast.LENGTH_SHORT).show();
        }
    }


}

class UploadListener implements TransferListener {
    Context uploadContext;

    public UploadListener(Context context) {
        uploadContext = context;
    }

    // Simply updates the UI list when notified.
    @Override
    public void onError(int id, Exception e) {
        Log.e(TAG, "Error during upload: " + id, e);
        Toast.makeText(uploadContext, "Image Upload Failed."+ e.getMessage(), Toast.LENGTH_LONG).show();
//        updateList();
    }

    @Override
    public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
        Log.d(TAG, String.format("onProgressChanged: %d, total: %d, current: %d",
                id, bytesTotal, bytesCurrent));
//        updateList();
    }

    @Override
    public void onStateChanged(int id, TransferState newState) {
        Log.d(TAG, "onStateChanged: " + id + ", " + newState);
//        updateList();
        if (newState == TransferState.COMPLETED) {
            Toast.makeText(uploadContext, "Image Uploaded To S3", Toast.LENGTH_LONG).show();
        }
    }
}