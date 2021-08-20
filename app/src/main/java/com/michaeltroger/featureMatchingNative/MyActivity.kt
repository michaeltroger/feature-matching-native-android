package com.michaeltroger.featureMatchingNative

import android.Manifest
import android.content.pm.PackageManager
import com.michaeltroger.featureMatchingNative.views.CameraPreviewView
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.michaeltroger.featureMatchingNative.tools.CameraManager

/**
 * main activity, responsible for loading the layout and its views
 * @author Michael Troger
 */
class MyActivity : ComponentActivity() {
    /**
     * the SurfaceView holding the current picture of the camera
     */
    private var mPreview: CameraPreviewView? = null

    /**
     * the camera manager is responsible for getting an instance of the camera
     * and releasing it correctly
     */
    private var mCameraManager: CameraManager? = null

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                onPermissionGranted()
            } else {
                checkPermissonAndInitialize()
            }
        }

    private fun checkPermissonAndInitialize() {
        if (ContextCompat.checkSelfPermission(baseContext, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            onPermissionGranted()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun onPermissionGranted() {
        mCameraManager!!.onResume()
        mPreview!!.setCamera(mCameraManager!!.camera)
        mPreview!!.visibility = View.VISIBLE // this can fix the freeze.
    }

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

        checkPermissonAndInitialize()
    }

    companion object {
        /**
         * class name for debugging with logcat
         */
        private val TAG = MyActivity::class.java.name
    }
}