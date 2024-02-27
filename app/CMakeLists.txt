
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

project(FeatureMatchingNative)

# create a variable for accessing the opencv library dir
set(
        lib_src_dir
        ${CMAKE_SOURCE_DIR}/../opencv/native
)

# register the opencv header files...
include_directories(${lib_src_dir}/jni/include)

# register a new library to import...
add_library(
        lib_opencv
        SHARED
        IMPORTED
)
# ...and assign the corresponding location of the precompiled opencv library
set_target_properties(
        lib_opencv
        PROPERTIES IMPORTED_LOCATION
        ${lib_src_dir}/libs/${ANDROID_ABI}/libopencv_java4.so
)

# register the custom c++ code of the app as library
add_library(
        native_opencv
        SHARED
        src/main/cpp/native_opencv.cpp
)

# link the required libs together
target_link_libraries(
        native_opencv # includes the custom c++ code of the app
        lib_opencv # includes the precompiled opencv lib
        android # include android sdk
        log # include android logging sdk
)
