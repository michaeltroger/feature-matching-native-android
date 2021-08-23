# Augmented Reality Template Matching using the NDK with an async approach (Coroutines) for >= Android 4.0
## Approach: Feature Matching  with Brute-Force matcher
Attention: This app was created in 2016. I was a beginner to Android development and Computer Vision back then. So don't expect a perfect code please. In 2021 I updated the project to build with the latest Android Studio (2020.3.1), updated most dependencies and converted it to Kotlin, while the business logic remained unchanged.


<img src="/screenshots/demo.gif" alt="Feature matching native" width="800px"/>
Copyright of the logo: The Coca-Cola Company

### What is this repository for?
* Uses the camera image to search for a specified template image within it via a feature matching approach using the OpenCV C++ library. The detected object is marked with lines within the scene. This can be used to e.g. find a logo.
* uses an async approach (Coroutines), that means that not every frame will be searched for the object and the GUI stays fluent/responsible
* the OpenCV Java library is used as well for loading the template image. The processing on the other hand is done by the OpenCV C++ library only. 
* currently the async calculated image is painted over the live camera preview which can be seen on the edge. This might be changed in the future.

### How do I get set up?
* IDE: Android Studio (tested with 2020.3.1)
* Android SDK & NDK
* Dependencies: OpenCV 3 library (included) [License](/opencv-3-4-15/LICENSE)
* Template image location: res/drawable Changeable in CameraPreviewView

### Default template image
<img src="/app/src/main/res/drawable/coca_cola.bmp" alt="" width="400px"/>
Copyright of the logo: The Coca-Cola Company

### Who do I talk to?
* Repo owner and developer: android@michaeltroger.com

### Credits
* The passing of the camera frame data from Java to C++ and the async approach is based on Jay Rambhia's AsyncCamera  https://github.com/jayrambhia/AsynCamera
* The feature matching is based on this official OpenCV tutorial http://docs.opencv.org/2.4/doc/tutorials/features2d/feature_homography/feature_homography.html Unlike in this application their version is using OpenCV 2 and is for use with normal images
* This C++ library integration builds on code from the Android Open Source Project: https://github.com/googlesamples/android-ndk/tree/master/hello-libs
