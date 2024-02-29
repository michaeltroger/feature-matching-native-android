cmake_minimum_required(VERSION 3.22.1)

# register the project for the C++ language
project(app CXX)

# find the opencv native sdk inside the AAR file / maven dependency (prerequisite: prefab = true)
find_package(OpenCV REQUIRED CONFIG)

# register the custom c++ code of the app
add_library(
        app
        SHARED
        native_opencv.cpp
)

# compile with c++20
target_compile_features(
        app
        PUBLIC
        cxx_std_20
)

# link the required libs together
target_link_libraries(
        app # includes the custom c++ code of the app
        OpenCV::opencv_java4 # includes the opencv lib from the maven dependency
        log # include android logging sdk
)
