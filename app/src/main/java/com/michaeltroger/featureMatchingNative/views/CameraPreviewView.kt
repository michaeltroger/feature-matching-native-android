package com.michaeltroger.featureMatchingNative.views

import android.view.SurfaceView
import android.view.SurfaceHolder
import android.graphics.Bitmap
import android.hardware.Camera.PreviewCallback
import android.app.ActivityManager
import android.content.Context
import android.graphics.ImageFormat
import com.michaeltroger.featureMatchingNative.R
import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import android.graphics.BitmapFactory
import android.hardware.Camera
import android.os.SystemClock
import android.util.Log
import android.widget.ImageView
import kotlinx.coroutines.*
import org.opencv.android.Utils
import java.io.IOException
import java.lang.Exception
import java.nio.ByteBuffer

/**
 * A basic Camera preview class
 * @author Michael Troger
 */
class CameraPreviewView(
    context: Context,
    /**
     * the instance to the active camera
     */
    private var mCamera: Camera?, CameraPreview: ImageView?
) : SurfaceView(context), SurfaceHolder.Callback {
    /**
     * the holder of the SurfaceView
     */
    private val mHolder: SurfaceHolder

    /**
     * time of last frame - for calculating FPS
     */
    private var lastTime: Long = 0

    /**
     * the view on which the camera image is drawn
     */
    private var myCameraPreview: ImageView? = null

    /**
     * pixel data has to be converted to bitmap before it can be drawn to view.
     * pixel data of current camera frame
     */
    private var bitmap: Bitmap? = null

    /**
     * the pixels proccesed for current camera frame
     */
    private var pixels: IntArray? = null

    /**
     * the camera frame as received from android camera before any processing
     */
    private var FrameData: ByteArray? = null

    /**
     * the preview format used by camera, usually NV21(YUV420)
     */
    private var imageFormat = 0

    private val scope = CoroutineScope(
        Job() + Dispatchers.Main
    )

    /**
     * for async handling, when true calculating is already running and computing in current frame is skipped
     */
    private var bProcessing = false
    override fun surfaceCreated(holder: SurfaceHolder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera!!.setPreviewDisplay(holder)
            mCamera!!.startPreview()
        } catch (e: IOException) {
            Log.d(TAG, "Error setting camera preview: " + e.message)
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {}

    /**
     * manually called when the app is resumed
     */
    private fun onResume() {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.
        if (mHolder.surface == null) {
            // preview surface does not exist
            return
        }

        // stop preview before making changes
        try {
            mCamera!!.stopPreview()
        } catch (e: Exception) {
            // ignore: tried to stop a non-existent preview
        }
        imageFormat = mCamera!!.parameters.previewFormat

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            mCamera!!.setPreviewCallback(mPreviewCallback)
            mCamera!!.setPreviewDisplay(mHolder)
            mCamera!!.startPreview()
        } catch (e: Exception) {
            Log.d(TAG, "Error starting camera preview: " + e.message)
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        onResume()
    }

    private val mPreviewCallback = PreviewCallback { data, camera ->
        // optional logging
        if (LOG_MEM_USAGE) {
            val mi = ActivityManager.MemoryInfo()
            val activityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.getMemoryInfo(mi)
            val availableMegs = mi.availMem / 1048576L // 1024 * 1024
            //Percentage can be calculated for API 16+
            //long percentAvail = mi.availMem / mi.totalMem;
            Log.d(TAG, "available mem: $availableMegs")
        }
        if (LOG_FPS) { // optionally calc. and log FPS
            val now = SystemClock.elapsedRealtime()
            val frametime = now - lastTime
            lastTime = now
            val fps = 1000.0f / frametime
            Log.i(TAG, "fps:$fps")
        }

        // At preview mode, the frame data will push to here.
        if (imageFormat == ImageFormat.NV21) {
            //We only accept the NV21(YUV420) format.
            if (!bProcessing) {
                bProcessing = true
                FrameData = data
                scope.launch {
                    withContext(Dispatchers.IO) {
                        ImageProcessing(PREVIEW_SIZE_WIDTH, PREVIEW_SIZE_HEIGHT, FrameData, pixels)
                    }
                    bProcessing = false

                    bitmap!!.setPixels(
                        pixels!!,
                        0,
                        PREVIEW_SIZE_WIDTH,
                        0,
                        0,
                        PREVIEW_SIZE_WIDTH,
                        PREVIEW_SIZE_HEIGHT
                    )
                    myCameraPreview!!.setImageBitmap(bitmap)
                    Log.i(TAG, "bitmap set in imageview")
                }
            } else {
                Log.e(TAG, "already running")
            }
        }
    }

    /**
     * sets the given camera as the used one
     * @param camera the camera to use
     */
    fun setCamera(camera: Camera?) {
        mCamera = camera
        //previewWidth = camera.getParameters().getPreviewSize().width;
        //previewHeight = camera.getParameters().getPreviewSize().height;
    }

    /**
     * called when the camera is paused
     */
    fun onPause() {
        if (mCamera != null) {
            mCamera!!.setPreviewCallback(null)
            mCamera!!.stopPreview()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope.cancel()
    }

    /**
     * image processing
     * @param width the preview width size
     * @param height the preview height size
     * @param NV21FrameData the data of the camera in NV21 format (standard)
     * @param pixels    the processed/changed pixel data after computing
     * @return  returns true if successful
     */
    external fun ImageProcessing(
        width: Int,
        height: Int,
        NV21FrameData: ByteArray?,
        pixels: IntArray?
    ): Boolean

    /**
     * initializes the template image in C++ and pre-calculates key points and
     * descriptors of template image
     * @param addrInputImage the address to the OpenCV matrix
     */
    external fun InitTemplateImage(addrInputImage: Long)

    companion object {
        /**
         * class name for debugging with logcat
         */
        private val TAG = CameraPreviewView::class.java.name

        /**
         * the template image to use
         */
        private val TEMPLATE_IMAGE = R.drawable.coca_cola

        /**
         * if the FPS shall be calculated and printed to logcat
         */
        private const val LOG_FPS = true

        /**
         * if the FPS shall be calculated and printed to logcat
         */
        private const val LOG_MEM_USAGE = true

        /**
         * preview size width
         */
        private const val PREVIEW_SIZE_WIDTH = 640

        /**
         * preview size height
         */
        private const val PREVIEW_SIZE_HEIGHT = 480

        // loading C++ libraries
        init {
            System.loadLibrary("native_opencv")
        }
    }

    /**
     * called when a CameraPreviewView instance is created
     * @param context the context - MyActivity
     * @param camera  the camera to use
     */
    init {
        myCameraPreview = CameraPreview

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = holder
        mHolder.addCallback(this)
        // deprecated setting, but required on Android versions prior to 3.0
        //mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        //myCameraPreview = CameraPreview;
        bitmap = Bitmap.createBitmap(PREVIEW_SIZE_WIDTH, PREVIEW_SIZE_HEIGHT, Bitmap.Config.ARGB_8888)
        pixels = IntArray(PREVIEW_SIZE_WIDTH * PREVIEW_SIZE_HEIGHT)

        // load the specified image from file system in bgr color
        var bgr: Mat? = null
        try {
            bgr = Utils.loadResource(
                context.applicationContext,
                TEMPLATE_IMAGE,
                Imgcodecs.IMREAD_COLOR
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // convert the image to rgba
        val templ = Mat()
        Imgproc.cvtColor(bgr, templ, Imgproc.COLOR_BGR2GRAY) //Imgproc.COLOR_BGR2RGBA);
        InitTemplateImage(templ.nativeObjAddr)
        val bMap = BitmapFactory.decodeResource(resources, R.drawable.coca_cola)
        val bytes = bMap.byteCount //calculate how many bytes our image consists of.
        val buffer = ByteBuffer.allocate(bytes) //Create a new buffer
        bMap.copyPixelsToBuffer(buffer) //Move the byte data to the buffer
        Log.d(TAG, "started :)")
    }
}