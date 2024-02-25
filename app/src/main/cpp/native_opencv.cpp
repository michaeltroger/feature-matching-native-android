/*
*  ImageProcessing.cpp
*/
#include <jni.h>
#include <android/log.h>

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <opencv2/calib3d/calib3d.hpp>
#include "opencv2/imgproc/imgproc_c.h"

using namespace std;
using namespace cv;

#define  LOG_TAG    "native_opencv"
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define  LOGW(...)  __android_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)

Mat* mCanny = NULL;

const Scalar RGBA_DRAWING_COLOR = Scalar(0, 255, 0, 255);

Ptr<ORB> fd_de;
BFMatcher dm(NORM_HAMMING);

Mat templ;

vector<KeyPoint> keypointsTemplate;
Mat descriptorsTemplate;

vector<KeyPoint> keypointsCamera;
Mat descriptorsCamera;

vector<DMatch> matches;
vector<Point2f> obj_corners(4);

extern "C" {
    void Java_com_michaeltroger_featureMatchingNative_views_CameraPreviewView_InitTemplateImage(
            JNIEnv *env, jobject instance, jlong addrInputImage
    ) {
        templ = *(Mat*)addrInputImage;

        LOGD( "called initTemplate %d x %d", templ.cols, templ.rows );

        fd_de = ORB::create();

        if (!templ.empty())
            LOGD("not empty");

        fd_de->detectAndCompute(templ, Mat(), keypointsTemplate, descriptorsTemplate);

        //-- Get the corners from the image_1 ( the object to be "detected" )
        obj_corners[0] = cv::Point (0,0);
        obj_corners[1] = cv::Point( templ.cols, 0 );
        obj_corners[2] = cv::Point( templ.cols, templ.rows );
        obj_corners[3] = cv::Point( 0, templ.rows );
    }

    jboolean Java_com_michaeltroger_featureMatchingNative_views_CameraPreviewView_ImageProcessing(
            JNIEnv* env, jobject thiz,
            jint width, jint height,
            jbyteArray NV21FrameData, jintArray outPixels
    ) {
        jbyte* pNV21FrameData = env->GetByteArrayElements(NV21FrameData, 0);
        jint* poutPixels = env->GetIntArrayElements(outPixels, 0);


        if ( mCanny == NULL )
        {
            LOGD( "created mCanny" );
            mCanny = new Mat(height, width, CV_8UC1);   // single channel array with 8 bit unsigned integers
        }

        Mat mGray(height, width, CV_8UC1, (unsigned char*)pNV21FrameData); // single channel array with 8 bit unsigned integers
        Mat yuvImg(height+height/2, width, CV_8UC1, (unsigned char *)pNV21FrameData);
        Mat rgbaImg;
        cvtColor(yuvImg, rgbaImg, CV_YUV2RGBA_NV21);

        Mat mResult(height, width, CV_8UC4, (unsigned char*)poutPixels);   // 4 channel array with 8 bit unsigned integers

        fd_de->detectAndCompute(mGray, Mat(), keypointsCamera, descriptorsCamera);

        if (!descriptorsCamera.empty()) {
            dm.match(descriptorsTemplate, descriptorsCamera, matches);

            if (matches.size() >= 4) {
                float max_dist = 0;
                float min_dist = 100;

                //-- Draw only "good" matches (i.e. whose distance is less than 3*min_dist )
                vector<DMatch> good_matches;

                for(int i = 0; i < descriptorsTemplate.rows; i++ )
                {
                    if (matches[i].distance < 3 * min_dist)
                    {
                        good_matches.push_back(matches[i]);
                    }
                }

                if (!good_matches.empty()) {
                    //-- Localize the object
                    vector<Point2f> obj;
                    vector<Point2f> scene;

                    for( int i = 0; i < good_matches.size(); i++ )
                    {
                        //-- Get the keypoints from the good matches
                        obj.push_back( keypointsTemplate[ good_matches[i].queryIdx ].pt );
                        scene.push_back( keypointsCamera[ good_matches[i].trainIdx ].pt );
                    }
                    Mat H = findHomography( obj, scene, RANSAC, 10 );


                    vector<Point2f> scene_corners(4);
                    try {
                        perspectiveTransform( obj_corners, scene_corners, H);

                        line( rgbaImg,
                              scene_corners[0],
                              scene_corners[1],
                              RGBA_DRAWING_COLOR,
                              4 );
                        line( rgbaImg,
                              scene_corners[1],
                              scene_corners[2],
                              RGBA_DRAWING_COLOR,
                              4 );
                        line( rgbaImg,
                              scene_corners[2],
                              scene_corners[3],
                              RGBA_DRAWING_COLOR,
                              4 );
                        line( rgbaImg,
                              scene_corners[3],
                              scene_corners[0],
                              RGBA_DRAWING_COLOR,
                              4 );
                    }

                    catch (Exception e) {
                        // ignore for now
                    }
                }

            }
        }

        cvtColor(rgbaImg, mResult, CV_RGBA2BGRA);

        env->ReleaseByteArrayElements(NV21FrameData, pNV21FrameData, 0);
        env->ReleaseIntArrayElements(outPixels, poutPixels, 0);

        return JNI_TRUE;
    }
}