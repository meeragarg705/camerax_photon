package com.particlesdevs.photoncamera.ui.camera;

import static android.os.Build.VERSION.SDK_INT;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.particlesdevs.photoncamera.R;
import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.app.base.BaseActivity;
import com.particlesdevs.photoncamera.settings.MigrationManager;
import com.particlesdevs.photoncamera.settings.PreferenceKeys;
import com.particlesdevs.photoncamera.util.FileManager;
import com.particlesdevs.photoncamera.util.log.FragmentLifeCycleMonitor;

import java.util.Arrays;


public class CameraActivity extends BaseActivity {

    private static final int CODE_REQUEST_PERMISSIONS = 1;
    private static final String[] PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET
    };
    private static final String[] PERMISSIONS2 = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
    };
    private static int requestCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        
        PreferenceManager.setDefaultValues(this, R.xml.preferences, MigrationManager.readAgain);
        PreferenceKeys.setDefaults(this);
        PhotonCamera.getSettings().loadCache();

        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentLifeCycleMonitor(), true);

        if (hasAllPermissions()) {

            tryLoad();
        } else
            requestPermission(); 
    }
    private void requestPermission() {
        requestPermissions(PERMISSIONS, CODE_REQUEST_PERMISSIONS);
        
        if (SDK_INT >= Build.VERSION_CODES.R) {
            ActivityResultLauncher<Intent> startActivityIntent = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> Log.d("CameraActivity", result.toString()));
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s",getApplicationContext().getPackageName())));
                startActivityIntent.launch(intent);

            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityIntent.launch(intent);

            }
            
            
        } else {
            
            requestPermissions(PERMISSIONS2, CODE_REQUEST_PERMISSIONS+1);
        }
    }

    private boolean hasAllPermissions() { 
        return Arrays.stream(PERMISSIONS).allMatch(permission -> checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
    }

    private void tryLoad() {
        FileManager.CreateFolders();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, CameraFragment.newInstance())
                .commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d("CameraActivity", "onRequestPermissionsResult() called with: " + "requestCode = [" + requestCode + "], " + "permissions = [" + Arrays.toString(permissions) + "], " + "grantResults = [" + Arrays.toString(grantResults) + "]");
        if (requestCode == CODE_REQUEST_PERMISSIONS) {
            if (Arrays.stream(grantResults).asLongStream().anyMatch(value -> value == PackageManager.PERMISSION_DENIED)) {
                requestPermission(); 
                requestCount++;
            } else
                tryLoad();
            if (requestCount > 15)
                System.exit(0);
        }
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container);
        if (!(fragment instanceof BackPressedListener) || !((BackPressedListener) fragment).onBackPressed())
            super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

//    @Override
//    public boolean dispatchKeyEvent(KeyEvent event) {
////        int action = event.getAction();
////        int keyCode = event.getKeyCode();
////        switch (keyCode) {
////            case KeyEvent.KEYCODE_VOLUME_UP:
////            case KeyEvent.KEYCODE_VOLUME_DOWN:
////                if (action == KeyEvent.ACTION_DOWN) {
////                    View view = findViewById(R.id.shutter_button);
////                    if (view.isClickable())
////                        view.performClick();
////                }
////                return true;
////            default:
////                return super.dispatchKeyEvent(event);
//        }
//    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            DisplayMetrics dm = getResources().getDisplayMetrics();
            float displayAspectRatio = (float) Math.max(dm.heightPixels, dm.widthPixels) / Math.min(dm.heightPixels, dm.widthPixels);
            if (displayAspectRatio <= (16f / 9) || dm.densityDpi > 440) {
                hideSystemUI();
            }
        }
    }

    private void hideSystemUI() {
        
        
        
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        
                        
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    
    
    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

}

