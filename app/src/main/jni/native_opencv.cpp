/*
*  ImageProcessing.cpp
*/
#include <jni.h>
#include <android/log.h>

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <opencv2/calib3d/calib3d.hpp>

using namespace std;
using namespace cv;

#define  LOG_TAG    "native_opencv"
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define  LOGW(...)  __android_log_print(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)

Mat* mCanny = NULL;
const int CANNY_THRESHOLD_LOW = 80;
const int CANNY_THRESHOLD_HIGH = 100;

const Scalar RGBA_GREEN = Scalar(0, 255, 0, 255);

//Ptr<FeatureDetector> fd;
//Ptr<ORB> de;
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
    void
    Java_be_pxl_troger_nativeAR_views_CameraPreviewView_InitTemplateImage(JNIEnv *env, jobject instance,
                                                                          jlong addrInputImage

    ) {

        templ = *(Mat*)addrInputImage;

        LOGD( "called initTemplate %d x %d", templ.cols, templ.rows );

        //fd = FastFeatureDetector::create(15);
        //de = ORB::create();
        fd_de = ORB::create();
        //dm = BFMatcher::create(NORM_HAMMING)


        if (!templ.empty())
            LOGD("not empty");


        //fd_de->detect(templ, keypointsTemplate);
        //fd_de->compute(templ, keypointsTemplate, descriptorsTemplate);
        fd_de->detectAndCompute(templ, Mat(), keypointsTemplate, descriptorsTemplate);

        //-- Get the corners from the image_1 ( the object to be "detected" )

        obj_corners[0] = cvPoint(0,0);
        obj_corners[1] = cvPoint( templ.cols, 0 );
        obj_corners[2] = cvPoint( templ.cols, templ.rows );
        obj_corners[3] = cvPoint( 0, templ.rows );
    }



    jboolean
    Java_be_pxl_troger_nativeAR_views_CameraPreviewView_ImageProcessing(
            JNIEnv* env, jobject thiz,
            jint width, jint height,
            jbyteArray NV21FrameData, jintArray outPixels)
    {
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


        //LOGD("resolution %d x %d", mGray.cols, mGray.rows);

        //cvtColor(yuvImg, mResult, CV_YUV2BGRA_NV21);


        //IplImage srcImg = mGray;
        //IplImage CannyImg = *mCanny;
        //IplImage ResultImg = mResult;

        /** // heavy (unnecessary) lifting for testing a-sync
        for (int i = 1; i < 10000; i++) {
            for (int j = 1; j < 10000; j++) {
                double x = 10 / 3.23;
            }
        }*/



        //Canny(mGray, *mCanny, CANNY_THRESHOLD_LOW, CANNY_THRESHOLD_HIGH);
       // cvtColor(*mCanny, mResult, CV_GRAY2BGRA);

        /*
        if (!templ.empty()) {
            LOGD("empty");
            Mat resized;
            resize(templ, resized, Size(mGray.cols, mGray.rows));
            cvtColor(resized, mResult, CV_GRAY2BGRA);
        }
        else {
            LOGD("not empty");
            cvtColor(mGray, mResult, CV_GRAY2BGRA);
        }
*/





       // fd_de->detect(mGray, keypointsCamera);

       // if (!keypointsCamera.empty()) {
           // fd_de->compute(mGray, keypointsCamera, descriptorsCamera);
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

                        //Mat img_matches;
                        //drawMatches(templ, keypointsTemplate, mGray, keypointsCamera, good_matches, img_matches);


                        Mat H = findHomography( obj, scene, CV_RANSAC, 10 );


/*
                    LOGD("object corners");
                    for (int j = 0; j < obj_corners.size(); j++) {
                        LOGD("value x %f y %f", obj_corners[j].x, obj_corners[j].y);
                    }
*/
                        vector<Point2f> scene_corners(4);
                        try {
                            perspectiveTransform( obj_corners, scene_corners, H);

                            /*
                             LOGD("calculated perspective transform");
                             for (int j = 0; j < scene_corners.size(); j++) {
                                 LOGD("value x %f y %f", scene_corners[j].x, scene_corners[j].y);
                             }
                             */

                            line( rgbaImg,
                                  scene_corners[0],
                                  scene_corners[1],
                                  RGBA_GREEN,
                                  4 );
                            line( rgbaImg,
                                  scene_corners[1],
                                  scene_corners[2],
                                  RGBA_GREEN,
                                  4 );
                            line( rgbaImg,
                                  scene_corners[2],
                                  scene_corners[3],
                                  RGBA_GREEN,
                                  4 );
                            line( rgbaImg,
                                  scene_corners[3],
                                  scene_corners[0],
                                  RGBA_GREEN,
                                  4 );

                            //-- Draw lines between the corners (the mapped object in the scene - image_2 )
                            /*
                            line( img_matches,
                                  scene_corners[0] + Point2f( templ.cols, 0),
                                  scene_corners[1] + Point2f( templ.cols, 0),
                                  Scalar(0, 255, 0),
                                  4 );
                            line( img_matches,
                                  scene_corners[1] + Point2f( templ.cols, 0),
                                  scene_corners[2] + Point2f( templ.cols, 0),
                                  Scalar( 0, 255, 0),
                                  4 );
                            line( img_matches,
                                  scene_corners[2] + Point2f( templ.cols, 0),
                                  scene_corners[3] + Point2f( templ.cols, 0),
                                  Scalar( 0, 255, 0),
                                  4 );
                            line( img_matches,
                                  scene_corners[3] + Point2f( templ.cols, 0),
                                  scene_corners[0] + Point2f( templ.cols, 0),
                                  Scalar( 0, 255, 0),
                                  4 );
                        */
                        }

                        catch (Exception e) {
                            // ignore for now
                        }










                        //Mat resized;
                        //resize(img_matches, resized, Size(mGray.cols, mGray.rows));
                        //cvtColor(resized, mResult, CV_RGB2BGRA);

                        //cvtColor(mGray, mResult, CV_GRAY2BGRA);
                    }

                }








            }


       // }



        cvtColor(rgbaImg, mResult, CV_RGBA2BGRA);



        // drawing unnecessary line for testing purpose
        //line(mResult, Point(20,20), Point(100,100), Scalar(255,255,255,255), 10, 0, 0);


        env->ReleaseByteArrayElements(NV21FrameData, pNV21FrameData, 0);
        env->ReleaseIntArrayElements(outPixels, poutPixels, 0);


        return JNI_TRUE;
    }
}