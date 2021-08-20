package com.michaeltroger.featureMatchingNative.tools

import android.content.Context
import android.hardware.Camera
import android.util.Log
import android.widget.Toast
import java.lang.Exception

/**
 * The camera manager is responsible for getting an instance of the camera
 * and releasing it correctly
 * @author Michael Troger
 */
class CameraManager(
    /**
     * a reference to MyActivity activity
     */
    private val mContext: Context
) {
    /**
     * get the active camera instance
     * @return returns the active camera instance
     */
    /**
     * the active camera instance
     */
    var camera: Camera?
        private set

    /**
     * releases the active camera
     */
    private fun releaseCamera() {
        if (camera != null) {
            camera!!.release() // release the camera for other applications
            camera = null
        }
    }

    /**
     * should be called when application is paused
     * because here the camera is released
     */
    fun onPause() {
        releaseCamera()
    }

    /**
     * should be called when application is resumed
     * because here a camera instance is received again
     */
    fun onResume() {
        if (camera == null) {
            camera = cameraInstance
        }
        setCameraParameters()
    }

    /**
     * sets the camera picture sizes and preview sizes
     */
    private fun setCameraParameters() {
        val params = camera!!.parameters
        Log.i("parameters", camera!!.parameters.flatten())
        params.setPreviewSize(PREVIEW_WIDTH, PREVIEW_HEIGHT)
        params.setPictureSize(PICTURE_WIDTH, PICTURE_HEIGHT)
        params.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
        camera!!.parameters = params
        Log.d(
            TAG, """get preview size:
             ${params.previewSize.width}x${params.previewSize.height}
            get picture size:
             ${params.pictureSize.width}x${params.pictureSize.height}"""
        )
        Toast.makeText(
            mContext, "preview size = " + camera!!.parameters.previewSize.width +
                    ", " + camera!!.parameters.previewSize.height, Toast.LENGTH_SHORT
        ).show()
    }

    companion object {
        /**
         * class name for debugging with logcat
         */
        private val TAG = CameraManager::class.java.name

        /**
         * the camera preview width
         */
        private const val PREVIEW_WIDTH = 640

        /**
         * the camera preview height
         */
        private const val PREVIEW_HEIGHT = 480

        /**
         * the camera picture width
         */
        private const val PICTURE_WIDTH = 1920

        /**
         * the camera picture height
         */
        private const val PICTURE_HEIGHT = 1080// Camera is not available (in use or does not exist)
        // returns null if camera is unavailable
        // attempt to get a Camera instance
        /**
         * A safe way to get an instance of the Camera object.
         * @return returns an instance of the camera save
         */
        private val cameraInstance: Camera?
            private get() {
                var c: Camera? = null
                try {
                    c = Camera.open() // attempt to get a Camera instance
                } catch (e: Exception) {
                    // Camera is not available (in use or does not exist)
                }
                return c // returns null if camera is unavailable
            }
    }

    /**
     * create a CameraManager instance
     * @param context a reference to MyActivity activity
     */
    init {
        // save the context (MyActivity)
        // Get an instance of Camera
        camera = cameraInstance
        Log.d(TAG, "started :)")
    }
}