package com.michaeltroger.featureMatchingNative

import android.app.Activity
import com.michaeltroger.featureMatchingNative.views.CameraPreviewView
import android.os.Bundle
import android.util.Log
import android.view.View
import com.michaeltroger.featureMatchingNative.R
import android.widget.FrameLayout
import android.widget.ImageView
import com.michaeltroger.featureMatchingNative.MyActivity
import com.michaeltroger.featureMatchingNative.tools.CameraManager

/**
 * main activity, responsible for loading the layout and its views
 * @author Michael Troger
 */
class MyActivity : Activity() {
    /**
     * the SurfaceView holding the current picture of the camera
     */
    private var mPreview: CameraPreviewView? = null

    /**
     * the camera manager is responsible for getting an instance of the camera
     * and releasing it correctly
     */
    private var mCameraManager: CameraManager? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my)

        // get a camera manager instance
        mCameraManager = CameraManager(this)
        val myCameraPreview = ImageView(this)
        // Create the preview view
        mPreview = CameraPreviewView(this, mCameraManager!!.camera, myCameraPreview)
        // init the camera preview with the necessary parameters so that
        // it knows which area of the camera picture is to analyze

        // add the live picture of the camera to the prepared FrameLayout
        val preview = findViewById<View>(R.id.camera_preview) as FrameLayout
        preview.addView(mPreview)
        preview.addView(myCameraPreview)
        Log.d(TAG, "started :)")
    }

    override fun onPause() {
        super.onPause()
        mPreview!!.onPause()
        mCameraManager!!.onPause()
        mPreview!!.visibility = View.GONE // this to fix freeze.
    }

    override fun onResume() {
        super.onResume()
        mCameraManager!!.onResume()
        mPreview!!.setCamera(mCameraManager!!.camera)
        mPreview!!.visibility = View.VISIBLE // this can fix the freeze.
    }

    companion object {
        /**
         * class name for debugging with logcat
         */
        private val TAG = MyActivity::class.java.name
    }
}