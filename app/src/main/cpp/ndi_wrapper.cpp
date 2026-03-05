#include <jni.h>
#include <android/log.h>
#include <string.h>
#include <memory>
#include <dlfcn.h>

// NDI SDK headers - use dynamic loading for Android
#include "Processing.NDI.DynamicLoad.h"

#define TAG "NDI_JNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)

// Global NDI instance (using dynamic loading)
static const NDIlib_v6_3* pNDI = nullptr;
static bool ndi_initialized = false;

// NDI Sender structure (we use the handle as a pointer)
struct NDISender {
    NDIlib_send_instance_t* p_send;
    char* name;

    NDISender(const char* source_name) : p_send(nullptr), name(nullptr) {
        if (source_name) {
            name = strdup(source_name);
        }
    }

    ~NDISender() {
        if (name) {
            free(name);
        }
    }
};

extern "C" {

JNIEXPORT jboolean JNICALL Java_com_soerjo_myndicam_NDIWrapper__1nativeInitialize(JNIEnv* env, jobject thiz) {
    if (ndi_initialized && pNDI) {
        LOGD("NDI already initialized");
        return JNI_TRUE;
    }

    // Load the NDI library dynamically
    pNDI = NDIlib_v6_load();
    if (!pNDI) {
        LOGE("Failed to load NDI library");
        return JNI_FALSE;
    }

    // Initialize NDI
    if (!pNDI->initialize()) {
        LOGE("Failed to initialize NDI");
        pNDI = nullptr;
        return JNI_FALSE;
    }

    ndi_initialized = true;
    LOGD("NDI initialized successfully (version: %s)", pNDI->version());
    return JNI_TRUE;
}

JNIEXPORT jlong JNICALL Java_com_soerjo_myndicam_NDIWrapper__1nativeCreateSender(JNIEnv* env, jobject thiz, jstring source_name) {
    if (!ndi_initialized) {
        LOGE("NDI not initialized");
        return 0;
    }

    const char* source_name_str = env->GetStringUTFChars(source_name, nullptr);
    if (!source_name_str) {
        LOGE("Failed to get source name string");
        return 0;
    }

    // Create NDI sender using dynamic API
    NDIlib_send_create_t send_desc;
    send_desc.p_ndi_name = source_name_str;
    send_desc.p_groups = nullptr;
    send_desc.clock_video = false;
    send_desc.clock_audio = false;

    NDIlib_send_instance_t* p_send = reinterpret_cast<NDIlib_send_instance_t *>(pNDI->send_create(
            &send_desc));
    env->ReleaseStringUTFChars(source_name, source_name_str);

    if (!p_send) {
        LOGE("Failed to create NDI sender");
        return 0;
    }

    // Create and return the sender instance
    NDISender* sender = new NDISender(source_name_str);
    sender->p_send = p_send;

    LOGD("NDI sender created: %s (handle: %p)", source_name_str, sender);
    return reinterpret_cast<jlong>(sender);
}

JNIEXPORT jboolean JNICALL Java_com_soerjo_myndicam_NDIWrapper__1nativeSendFrame(JNIEnv* env, jobject thiz,
                                                        jlong handle,
                                                        jbyteArray data,
                                                        jint width,
                                                        jint height,
                                                        jint stride) {
    if (handle == 0) {
        LOGE("Invalid sender handle");
        return JNI_FALSE;
    }

    NDISender* sender = reinterpret_cast<NDISender*>(handle);
    if (!sender || !sender->p_send) {
        LOGE("Invalid sender instance");
        return JNI_FALSE;
    }

    // Get frame data
    jbyte* frame_data = env->GetByteArrayElements(data, nullptr);
    if (!frame_data) {
        LOGE("Failed to get frame data");
        return JNI_FALSE;
    }

    jsize data_length = env->GetArrayLength(data);

    // Create NDI video frame (v2 structure)
    NDIlib_video_frame_v2_t video_frame;
    video_frame.frame_format_type = NDIlib_frame_format_type_progressive;  // Progressive frame
    video_frame.xres = width;
    video_frame.yres = height;

    // Calculate line stride (in bytes for 4:2:2:8 format)
    video_frame.line_stride_in_bytes = stride;

    // Set up the frame data
    video_frame.p_data = reinterpret_cast<uint8_t*>(frame_data);

    // Set the frame format - use BGRA for correct colors (blue, green, red, alpha)
    video_frame.FourCC = NDIlib_FourCC_type_BGRA;

    // Set frame rate (default to 30fps)
    video_frame.frame_rate_N = 30000;
    video_frame.frame_rate_D = 1001;

    // Set picture aspect ratio (0 means square pixels)
    video_frame.picture_aspect_ratio = 0.0f;

    // Set timecode (not critical for our use)
    video_frame.timecode = NDIlib_send_timecode_synthesize;

    // Set metadata to null
    video_frame.p_metadata = nullptr;

    // Send the frame using v2 async API for better performance
    // Async scheduling allows frame processing on separate threads
    pNDI->send_send_video_async_v2(reinterpret_cast<NDIlib_send_instance_t>(sender->p_send), &video_frame);

    // Release the array elements
    env->ReleaseByteArrayElements(data, frame_data, JNI_ABORT);

    return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_com_soerjo_myndicam_NDIWrapper__1nativeDestroySender(JNIEnv* env, jobject thiz, jlong handle) {
    if (handle == 0) {
        return;
    }

    NDISender* sender = reinterpret_cast<NDISender*>(handle);
    if (sender && sender->p_send && pNDI) {
        pNDI->send_destroy(reinterpret_cast<NDIlib_send_instance_t>(sender->p_send));
        LOGD("NDI sender destroyed (handle: %p)", sender);
    }

    if (sender) {
        delete sender;
    }
}

JNIEXPORT void JNICALL Java_com_soerjo_myndicam_NDIWrapper__1nativeCleanup(JNIEnv* env, jobject thiz) {
    if (ndi_initialized && pNDI) {
        pNDI->destroy();
        // Note: NDIlib_v6_load does not have a corresponding unload function
        // The library will be unloaded when the process exits
        pNDI = nullptr;
        ndi_initialized = false;
        LOGD("NDI cleanup complete");
    }
}

} // extern "C"
