
#
# Copyright (C) The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

cmake_minimum_required(VERSION 3.4.1)

# create a variable for accessing the opencv library dir
set(
        lib_src_dir
        ${CMAKE_SOURCE_DIR}/../../../../opencv-3-4-15
)

# register a library to import...
add_library(
        lib_opencv_precompiled
        SHARED
        IMPORTED
)
# ...and assign the corresponding location of the precompiled library
set_target_properties(
        lib_opencv_precompiled
        PROPERTIES
        IMPORTED_LOCATION
        ${lib_src_dir}/native/libs/${ANDROID_ABI}/libopencv_java3.so
)

# register another library to import...
add_library(
        lib_opencv_include
        SHARED
        native_opencv.cpp # register the source code of the custom c++ code
)
# ...and assign the location of the c++ text files of the opencv lib
target_include_directories(
        lib_opencv_include
        PRIVATE
        ${lib_src_dir}/native/jni/include
)

# link the required libs together
target_link_libraries(
        lib_opencv_include # includes source code and opencv source file
        lib_opencv_precompiled # includes the precompiled opencv lib
        android # include android sdk
        log # include android logging sdk
)
