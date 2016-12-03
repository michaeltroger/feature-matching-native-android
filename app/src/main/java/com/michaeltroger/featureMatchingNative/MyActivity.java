package com.michaeltroger.featureMatchingNative;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;


import com.michaeltroger.featureMatchingNative.tools.CameraManager;
import com.michaeltroger.featureMatchingNative.views.CameraPreviewView;

/**
 * main activity, responsible for loading the layout and its views
 * @author Michael Troger
 */
public class MyActivity extends Activity {
    /**
     * class name for debugging with logcat
     */
    private static final String TAG = MyActivity.class.getName();
    /**
     * the SurfaceView holding the current picture of the camera
     */
    private CameraPreviewView mPreview;
    /**
     * the camera manager is responsible for getting an instance of the camera
     * and releasing it correctly
     */
    private CameraManager mCameraManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        // get a camera manager instance
        mCameraManager = new CameraManager(this);
        ImageView myCameraPreview = new ImageView(this);
        // Create the preview view
        mPreview = new CameraPreviewView(this, mCameraManager.getCamera(), myCameraPreview);
        // init the camera preview with the necessary parameters so that
        // it knows which area of the camera picture is to analyze

        // add the live picture of the camera to the prepared FrameLayout
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        preview.addView(myCameraPreview);

        Log.d(TAG, "started :)");
    }


    @Override
    protected void onPause() {
        super.onPause();

        mPreview.onPause();
        mCameraManager.onPause();

        mPreview.setVisibility(View.GONE); // this to fix freeze.
    }


    @Override
    protected void onResume() {
        super.onResume();
        mCameraManager.onResume();

        mPreview.setCamera(mCameraManager.getCamera());

        mPreview.setVisibility(View.VISIBLE); // this can fix the freeze.
    }

}