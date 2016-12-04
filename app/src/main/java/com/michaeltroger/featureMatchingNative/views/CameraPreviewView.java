package com.michaeltroger.featureMatchingNative.views;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.michaeltroger.featureMatchingNative.MyActivity;
import com.michaeltroger.featureMatchingNative.R;


/**
 * A basic Camera preview class
 * @author Michael Troger
 */
public class CameraPreviewView extends SurfaceView implements SurfaceHolder.Callback {
    /**
     * class name for debugging with logcat
     */
    private static final String TAG = CameraPreviewView.class.getName();
    /**
     * the template image to use
     */
    private static final int TEMPLATE_IMAGE = R.drawable.coca_cola;
    /**
     * the holder of the SurfaceView
     */
    private SurfaceHolder mHolder;
    /**
     * the instance to the active camera
     */
    private Camera mCamera;
    /**
     * reference to the MyActivity.class
     */
    private MyActivity mContext;

    /**
     * time of last frame - for calculating FPS
     */
    private long lastTime;

    /**
     * if the FPS shall be calculated and printed to logcat
     */
    private static final boolean LOG_FPS = true;
    /**
     * if the FPS shall be calculated and printed to logcat
     */
    private static final boolean LOG_MEM_USAGE = true;
    /**
     * preview size width
     */
    private static final int PREVIEW_SIZE_WIDTH = 640;

    /**
     * preview size height
     */
    private static final int PREVIEW_SIZE_HEIGHT = 480;
    /**
     * the view on which the camera image is drawn
     */
    private ImageView myCameraPreview = null;
    /**
     * pixel data has to be converted to bitmap before it can be drawn to view.
     * pixel data of current camera frame
     */
    private Bitmap bitmap = null;
    /**
     * the pixels proccesed for current camera frame
     */
    private int[] pixels = null;
    /**
     * the camera frame as received from android camera before any processing
     */
    private byte[] FrameData = null;
    /**
     * the preview format used by camera, usually NV21(YUV420)
     */
    private int imageFormat;
    /**
     * the preview width
     */
    private int PreviewSizeWidth;
    /**
     * the preview height
     */
    private int PreviewSizeHeight;
    /**
     * for async handling, when true calculating is already running and computing in current frame is skipped
     */
    private boolean bProcessing = false;

    /**
     * called when a CameraPreviewView instance is created
     * @param context the context - MyActivity
     * @param camera  the camera to use
     */
    public CameraPreviewView(Context context, Camera camera, ImageView CameraPreview) {
        super(context);
        mCamera = camera;

        mContext = (MyActivity)context;

        myCameraPreview = CameraPreview;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        //mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        PreviewSizeWidth = PREVIEW_SIZE_WIDTH;
        PreviewSizeHeight = PREVIEW_SIZE_HEIGHT;
        //myCameraPreview = CameraPreview;
        bitmap = Bitmap.createBitmap(PreviewSizeWidth, PreviewSizeHeight, Bitmap.Config.ARGB_8888);
        pixels = new int[PreviewSizeWidth * PreviewSizeHeight];

        // load the specified image from file system in bgr color
        Mat bgr = null;
        try {
            bgr = Utils.loadResource(mContext.getApplicationContext(), TEMPLATE_IMAGE, Imgcodecs.CV_LOAD_IMAGE_COLOR);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // convert the image to rgba
        Mat templ = new Mat();
        Imgproc.cvtColor(bgr, templ, Imgproc.COLOR_BGR2GRAY);//Imgproc.COLOR_BGR2RGBA);

        InitTemplateImage(templ.getNativeObjAddr());

        Bitmap bMap= BitmapFactory.decodeResource(getResources(), R.drawable.coca_cola);

        int bytes = bMap.getByteCount();  //calculate how many bytes our image consists of.
        ByteBuffer buffer = ByteBuffer.allocate(bytes); //Create a new buffer
        bMap.copyPixelsToBuffer(buffer); //Move the byte data to the buffer
        Log.d(TAG, "started :)");
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {}

    /**
     * manually called when the app is resumed
     */
    private void onResume() {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null){
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        imageFormat = mCamera.getParameters().getPreviewFormat();

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            mCamera.setPreviewCallback(mPreviewCallback);
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
       onResume();
    }

    private Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            // optional logging
            if (LOG_MEM_USAGE) {
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                ActivityManager activityManager = (ActivityManager) mContext.getSystemService(mContext.ACTIVITY_SERVICE);
                activityManager.getMemoryInfo(mi);
                long availableMegs = mi.availMem / 1048576L; // 1024 * 1024
                //Percentage can be calculated for API 16+
                //long percentAvail = mi.availMem / mi.totalMem;
                Log.d(TAG, "available mem: " + availableMegs);
            }

            if (LOG_FPS) { // optionally calc. and log FPS
                long now = SystemClock.elapsedRealtime();
                long frametime = now - lastTime;
                lastTime = now;
                float fps = 1000.0f / frametime;
                Log.i(TAG, "fps:" + fps);
            }

            // At preview mode, the frame data will push to here.
            if (imageFormat == ImageFormat.NV21)
            {
                //We only accept the NV21(YUV420) format.
                if ( !bProcessing )
                {
                    bProcessing = true;
                    FrameData = data;
                    new ProcessPreviewDataTask().execute(data);
                } else {
                    Log.e(TAG, "already running");
                }
            }
        }
    };

    /**
     * sets the given camera as the used one
     * @param camera the camera to use
     */
    public void setCamera(Camera camera) {
        mCamera = camera;
        //previewWidth = camera.getParameters().getPreviewSize().width;
        //previewHeight = camera.getParameters().getPreviewSize().height;
    }

    /**
     * called when the camera is paused
     */
    public void onPause() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
        }
    }

    /**
     * image processing
     * @param width the preview width size
     * @param height the preview height size
     * @param NV21FrameData the data of the camera in NV21 format (standard)
     * @param pixels    the processed/changed pixel data after computing
     * @return  returns true if successful
     */
    public native boolean ImageProcessing(int width, int height, byte[] NV21FrameData, int [] pixels);

    /**
     * initializes the template image in C++ and pre-calculates key points and
     * descriptors of template image
     * @param addrInputImage the address to the OpenCV matrix
     */
    public native void InitTemplateImage(long addrInputImage);

    // loading C++ libraries
    static
    {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("native_opencv");
    }

    private class ProcessPreviewDataTask extends AsyncTask<byte[], Void, Boolean> {
        @Override
        protected Boolean doInBackground(byte[]... datas) {
            Log.i(TAG, "background process started");
            // byte[] data = datas[0];

            ImageProcessing(PreviewSizeWidth, PreviewSizeHeight, FrameData, pixels);

            // mCamera.addCallbackBuffer(data);
            bProcessing = false;
            Log.i(TAG, "doInBackground "+String.valueOf(isCancelled()));
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result){
            Log.i(TAG, "running onPostExecute");
            // set pixels
            bitmap.setPixels(pixels, 0, PreviewSizeWidth, 0, 0, PreviewSizeWidth, PreviewSizeHeight);
            myCameraPreview.setImageBitmap(bitmap);
            Log.i(TAG, "bitmap set in imageview");
        }
    }
}
