package com.michaeltroger.featureMatchingNative.tools;


import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.widget.Toast;

/**
 * The camera manager is responsible for getting an instance of the camera
 * and releasing it correctly
 * @author Michael Troger
 */
public class CameraManager {
    /**
     * class name for debugging with logcat
     */
    private static final String TAG = CameraManager.class.getName();
    /**
     * the active camera instance
     */
    private Camera mCamera;
    /**
     * a reference to MyActivity activity
     */
    private Context mContext;
    /**
     * the camera preview width
     */
    private static final int PREVIEW_WIDTH = 640;
    /**
     * the camera preview height
     */
    private static final int PREVIEW_HEIGHT = 480;
    /**
     * the camera picture width
     */
    private static final int PICTURE_WIDTH = 1920;
    /**
     * the camera picture height
     */
    private static final int PICTURE_HEIGHT = 1080;

    /**
     * create a CameraManager instance
     * @param context a reference to MyActivity activity
     */
    public CameraManager(Context context) {
        // save the context (MyActivity)
        mContext = context;
        // Get an instance of Camera
        mCamera = getCameraInstance();


        Log.d(TAG, "started :)");
    }

    /**
     * get the active camera instance
     * @return returns the active camera instance
     */
    public Camera getCamera() {
        return mCamera;
    }

    /**
     * releases the active camera
     */
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release(); // release the camera for other applications
            mCamera = null;
        }
    }

    /**
     * should be called when application is paused
     * because here the camera is released
     */
    public void onPause() {
        releaseCamera();
    }

    /**
     * should be called when application is resumed
     * because here a camera instance is received again
     */
    public void onResume() {
        if (mCamera == null) {
            mCamera = getCameraInstance();
        }

        setCameraParameters();
    }

    /**
     * sets the camera picture sizes and preview sizes
     */
    private void setCameraParameters() {
        Camera.Parameters params = mCamera.getParameters();
        Log.i("parameters", mCamera.getParameters().flatten());
        params.setPreviewSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);
        params.setPictureSize(PICTURE_WIDTH, PICTURE_HEIGHT);
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        mCamera.setParameters(params);
        Log.d(TAG, "get preview size:\n "
                        + params.getPreviewSize().width + "x"
                        + params.getPreviewSize().height
                        + "\nget picture size:\n "
                        + params.getPictureSize().width + "x"
                        + params.getPictureSize().height
        );


        Toast.makeText(mContext, "preview size = " + mCamera.getParameters().getPreviewSize().width +
                ", " + mCamera.getParameters().getPreviewSize().height, Toast.LENGTH_SHORT).show();
    }

    /**
     * A safe way to get an instance of the Camera object.
     * @return returns an instance of the camera save
     */
    private static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

}
