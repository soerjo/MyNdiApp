cmake_minimum_required(VERSION 3.22.1)

project("nativelib")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# LAME encoder
include_directories(${CMAKE_SOURCE_DIR}/module/mp3/lame)
aux_source_directory(${CMAKE_SOURCE_DIR}/module/mp3/lame SRC_LAME)

add_library(
        nativelib
        SHARED

        ${SRC_LAME}
        utils/logger.cpp
        module/yuv/yuv.cpp
        module/mp3/mp3.cpp
        proxy/proxy_yuv.cpp
        proxy/proxy_mp3.cpp
        nativelib.cpp)

# Add compiler flags to fix LAME MP3 encoder issues with NDK 27
# Use C_FLAGS for C files and CXX_FLAGS for C++ files
target_compile_options(nativelib PRIVATE
    -Wno-error=implicit-function-declaration
    -Wno-error=missing-declarations
)

find_library(
        log-lib
        log)

target_link_libraries(
        nativelib
        ${log-lib})
