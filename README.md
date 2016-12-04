# Augmented Reality Feature Matching using the NDK with an async approach for >= Android 4.0

### What is this repository for?
* Uses the camera image to search for certain objects within it via a feature matching approach using the OpenCV C++ library. The detected object is marked with lines within the scene.
* uses an async approach, that means that not every frame will be searched for the object and the GUI stays fluent/responsible
* Version 1.1

### How do I get set up?
* IDE: Android Studio (tested with 2.2.2)
* Android SDK & NDK
* Dependencies: OpenCV 3.0.0 library (included)
* Template image location: res/drawable Changeable in CameraPreviewView

### Who do I talk to?
* Repo owner and developer: android@michaeltroger.com

### Credits
* The passsing of the camera frame data from Java to C++ and the async approach is based on Jay Rambhia's  https://github.com/jayrambhia/AsynCamera
