cmake_minimum_required(VERSION 3.22.1)

project("realesrgan_ncnn")

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ncnn prebuilt package for this ABI
set(ncnn_DIR "${CMAKE_CURRENT_SOURCE_DIR}/ncnn/ncnn-20240410-android-vulkan/${ANDROID_ABI}/lib/cmake/ncnn")
find_package(ncnn REQUIRED)

add_library(realesrgan_ncnn SHARED
    realcugan.cpp
    realcugan_jni.cpp
    realesrgan.cpp
    realesrgan_jni.cpp
)

find_package(Threads REQUIRED)

target_link_libraries(realesrgan_ncnn
    ncnn
    jnigraphics
    android
    log
    ${CMAKE_THREAD_LIBS_INIT}
)

target_include_directories(realesrgan_ncnn PRIVATE
    "${CMAKE_CURRENT_SOURCE_DIR}"
    "${CMAKE_CURRENT_SOURCE_DIR}/ncnn/ncnn-20240410-android-vulkan/${ANDROID_ABI}/include"
)
