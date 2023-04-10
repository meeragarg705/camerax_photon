package com.particlesdevs.photoncamera.gallery.ui.fragments;

import static com.amazonaws.mobile.auth.core.internal.util.ThreadUtils.runOnUiThread;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import androidx.recyclerview.widget.RecyclerView;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.particlesdevs.photoncamera.R;
import com.particlesdevs.photoncamera.databinding.FragmentGalleryImageLibraryBinding;
import com.particlesdevs.photoncamera.gallery.adapters.ImageGridAdapter;
import com.particlesdevs.photoncamera.gallery.files.GalleryFileOperations;
import com.particlesdevs.photoncamera.gallery.files.ImageFile;
import com.particlesdevs.photoncamera.gallery.helper.Constants;
import com.particlesdevs.photoncamera.gallery.model.GalleryItem;
import com.particlesdevs.photoncamera.gallery.viewmodel.GalleryViewModel;
import com.particlesdevs.photoncamera.util.AwsUtils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import io.reactivex.schedulers.Schedulers;


public class ImageLibraryFragment extends Fragment implements ImageGridAdapter.GridAdapterCallback {
    private static final String TAG = ImageViewerFragment.class.getSimpleName();
    private FragmentGalleryImageLibraryBinding fragmentGalleryImageLibraryBinding;
    private NavController navController;
    private ImageGridAdapter imageGridAdapter;
    private RecyclerView recyclerView;
    private boolean isFABOpen;
    private List<GalleryItem> galleryItems;
    private GalleryViewModel viewModel;
    static TransferUtility transferUtility;
    static AwsUtils awsUtils;
    static ArrayList<HashMap<String, Object>> transferRecordMaps;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        fragmentGalleryImageLibraryBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_gallery_image_library, container, false);
        navController = NavHostFragment.findNavController(this);
        return fragmentGalleryImageLibraryBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(GalleryViewModel.class);
        recyclerView = fragmentGalleryImageLibraryBinding.imageGridRv;
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(10);
        observeAllMediaFiles();
        getAwsData();

        initListeners();
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

    private void observeAllMediaFiles() {
        viewModel.getAllImageFilesData().observe(getViewLifecycleOwner(), this::initImageAdapter);
    }

    private void initImageAdapter(List<GalleryItem> galleryItems) {
        if (galleryItems != null) {
            this.galleryItems = galleryItems;
            imageGridAdapter = new ImageGridAdapter(this.galleryItems, Constants.GALLERY_ITEM_TYPE_GRID);
            imageGridAdapter.setHasStableIds(true);
            imageGridAdapter.setGridAdapterCallback(this);
            recyclerView.setAdapter(imageGridAdapter);
        }
    }

    private void initListeners() {
        fragmentGalleryImageLibraryBinding.fabGroup.numberFab.setOnLongClickListener(v -> {
            onImageSelectionStopped();
            return true;
        });
        fragmentGalleryImageLibraryBinding.fabGroup.setOnNumFabClicked(this::onNumFabClicked);
        fragmentGalleryImageLibraryBinding.fabGroup.setOnShareFabClicked(this::onShareFabClicked);
        fragmentGalleryImageLibraryBinding.fabGroup.setOnDeleteFabClicked(this::onDeleteFabClicked);
        fragmentGalleryImageLibraryBinding.fabGroup.setOnCompareFabClicked(this::onCompareFabClicked);
    }

    private void onCompareFabClicked(View view) {
        List<GalleryItem> selectedItems = imageGridAdapter.getSelectedItems();
        if (selectedItems.size() == 2) {
            NavController navController = Navigation.findNavController(view);
            Bundle b = new Bundle(2);
            int image1pos = galleryItems.indexOf(selectedItems.get(0));
            int image2pos = galleryItems.indexOf(selectedItems.get(1));
            b.putInt(Constants.IMAGE1_KEY, image1pos);
            b.putInt(Constants.IMAGE2_KEY, image2pos);
            navController.navigate(R.id.action_imageLibraryFragment_to_imageCompareFragment, b);
        }
    }

    private void onDeleteFabClicked(View view) {
        List<GalleryItem> filesToDelete = imageGridAdapter.getSelectedItems();
        String numOfFiles = String.valueOf(filesToDelete.size());
        String totalFileSize = FileUtils.byteCountToDisplaySize((int) filesToDelete.stream().mapToLong(value -> value.getFile().getSize()).sum());
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder
                .setMessage(getContext().getString(R.string.sure_delete_multiple, numOfFiles, totalFileSize))
                .setTitle(android.R.string.dialog_alert_title)
                .setIcon(R.drawable.ic_delete)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.yes, (dialog, which) -> GalleryFileOperations.deleteImageFiles(getActivity(), filesToDelete.stream().map(galleryItem -> (ImageFile) galleryItem.getFile()).collect(Collectors.toList()), this::handleImagesDeletedCallback))
                .create()
                .show();
    }

    private void onShareFabClicked(View view) {
        ArrayList<Uri> imageUris = (ArrayList<Uri>) imageGridAdapter.getSelectedItems().stream().map(galleryItem -> galleryItem.getFile().getFileUri()).collect(Collectors.toList());
        closeFABMenu();
        showBottomSheetDialog(imageUris);


    }


    private void showBottomSheetDialog(ArrayList<Uri> imageUris) {


        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_dialog_layout);


        LinearLayout shareAws = bottomSheetDialog.findViewById(R.id.shareLinearLayout);
        LinearLayout uploadGoogle = bottomSheetDialog.findViewById(R.id.uploadLinearLayout);
        LinearLayout download = bottomSheetDialog.findViewById(R.id.download);


        bottomSheetDialog.show();


        shareAws.setOnClickListener(v -> {

            bottomSheetDialog.dismiss();

            uploads(imageUris);


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

    private void onNumFabClicked(View view) {
        if (!isFABOpen) {
            showFABMenu();
        } else {
            closeFABMenu();
        }
    }

    private void showFABMenu() {
        isFABOpen = true;
        fragmentGalleryImageLibraryBinding.fabGroup.deleteFab.animate().translationY(-getResources().getDimension(R.dimen.standard_65));
        fragmentGalleryImageLibraryBinding.fabGroup.shareFab.animate().translationY(-getResources().getDimension(R.dimen.standard_125));
    }

    private void closeFABMenu() {
        isFABOpen = false;
        fragmentGalleryImageLibraryBinding.fabGroup.deleteFab.animate().translationY(0);
        fragmentGalleryImageLibraryBinding.fabGroup.shareFab.animate().translationY(0);
    }

    @Override
    public void onItemClicked(int position, View view, GalleryItem galleryItem) {
        Bundle b = new Bundle();
        b.putInt(Constants.IMAGE_POSITION_KEY, position);
        NavController navController = Navigation.findNavController(view);
        navController.navigate(R.id.action_imageLibraryFragment_to_imageViewerFragment, b);

    }

    @Override
    public void onImageSelectionChanged(int numOfSelectedFiles) {
        fragmentGalleryImageLibraryBinding.setButtonsVisible(true);
        fragmentGalleryImageLibraryBinding.fabGroup.setSelectedCount(String.valueOf(numOfSelectedFiles));
        fragmentGalleryImageLibraryBinding.fabGroup.setCompareVisible(numOfSelectedFiles == 2);
    }

    @Override
    public void onImageSelectionStopped() {
        imageGridAdapter.deselectAll();
        if (isFABOpen) {
            closeFABMenu();
        }
        fragmentGalleryImageLibraryBinding.setButtonsVisible(false);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isFABOpen) {
            closeFABMenu();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getParentFragmentManager().beginTransaction().remove(ImageLibraryFragment.this).commitAllowingStateLoss();
        fragmentGalleryImageLibraryBinding = null;
    }

    public void handleImagesDeletedCallback(boolean isDeleted) {
        if (isDeleted) {
            List<GalleryItem> filesToDelete = imageGridAdapter.getSelectedItems();

            String numOfFiles = String.valueOf(filesToDelete.size());
            String totalFileSize = FileUtils.byteCountToDisplaySize((int) filesToDelete.stream().mapToLong(value -> value.getFile().getSize()).sum());
            galleryItems.removeAll(filesToDelete);
            imageGridAdapter.setGalleryItemList(galleryItems);
            imageGridAdapter.notifyItemRangeChanged(0, imageGridAdapter.getItemCount());

            onImageSelectionStopped();

            Snackbar.make(getView(),
                    getString(R.string.multiple_deleted_success, numOfFiles, totalFileSize),
                    Snackbar.LENGTH_SHORT).show();
        } else {
            Snackbar.make(getView(),
                    "Deletion Failed!",
                    Snackbar.LENGTH_SHORT).show();
        }
    }

    private File readContentToFile(Uri uri) throws IOException {
        final File file = new File(getContext().getCacheDir(), getDisplayName(uri));
        try (
                final InputStream in = getContext().getContentResolver().openInputStream(uri);
                final OutputStream out = new FileOutputStream(file, false)
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
                Cursor cursor = getContext().getContentResolver().query(uri, projection, null, null, null)
        ) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
            if (cursor.moveToFirst()) {
                return cursor.getString(columnIndex);
            }
        }

        Log.w(TAG, "Couldnt determine DISPLAY_NAME for Uri.  Falling back to Uri path: " + uri.getPath());
        return uri.getPath();
    }

    @SuppressLint("CheckResult")
    public void uploads(ArrayList<Uri> uri) {
        Toast.makeText(getContext(), "Upload Started", Toast.LENGTH_SHORT).show();
        Map<String, File> map = uri.stream().collect(Collectors.toMap(e -> new File(e.getPath()).getName(), e -> {
            File file = null;

            try {
                file = readContentToFile(e);
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            return file;
        }));
        Objects.requireNonNull(new MultiUploaderS3().uploadMultiple(map, requireContext()))
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(
                        () ->
                                runOnUiThread(() -> Toast.makeText(getContext(), "Upload Completed", Toast.LENGTH_LONG).show())
                        , throwable -> runOnUiThread(() -> Toast.makeText(getContext(), "Upload Failed!", Toast.LENGTH_LONG).show())

                );
    }

}



