cmake_minimum_required(VERSION 3.18.1)

project("ndi_wrapper")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Include directories
include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}/Include
)

# Find the log library
find_library(log-lib log)

# Source files
set(SOURCES
    ndi_wrapper.cpp
)

# Add the prebuilt NDI library
add_library(ndi_prebuilt SHARED IMPORTED)
set_target_properties(ndi_prebuilt PROPERTIES
    IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libndi.so
)

# Build the NDI wrapper shared library (renamed to avoid conflict)
add_library(ndi_wrapper SHARED ${SOURCES})

# Link libraries
target_link_libraries(ndi_wrapper
    ${log-lib}
    android
    dl  # For dynamic loading
    ndi_prebuilt  # Link to the NDI library
)

# Enable RTTI and exceptions for NDI compatibility
target_compile_features(ndi_wrapper PUBLIC cxx_std_17)
