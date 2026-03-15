#include <jni.h>
#include <android/log.h>
#include <string.h>
#include <memory>
#include <dlfcn.h>
#include <pthread.h>
#include <unistd.h>
#include <atomic>

// NDI SDK headers - use dynamic loading for Android
#include "Processing.NDI.DynamicLoad.h"

#define TAG "NDI_JNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)

// Forward declaration of NDISender
struct NDISender;

// Global NDI instance (using dynamic loading)
static const NDIlib_v6_3* pNDI = nullptr;
static bool ndi_initialized = false;
static void* hNDI = nullptr;  // Handle for dynamic NDI library loading

// Tally callback infrastructure
static JavaVM* g_jvm = nullptr;
static jobject g_callback_object = nullptr;
static jmethodID g_callback_method = nullptr;
static pthread_t g_tally_thread = 0;
static std::atomic<bool> g_tally_thread_running(false);
static std::atomic<bool> g_tally_thread_stop_requested(false);
static NDISender* g_current_sender = nullptr;

// Previous tally state to detect changes
static bool g_prev_on_preview = false;
static bool g_prev_on_program = false;

// Mutex for protecting callback object access
static pthread_mutex_t g_callback_mutex = PTHREAD_MUTEX_INITIALIZER;

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

// Tally polling thread function
void* tally_poll_thread(void* arg) {
    LOGD("Tally polling thread started");

    // Attach current thread to JVM
    JNIEnv* env;
    jint result = g_jvm->AttachCurrentThread(&env, nullptr);
    if (result != JNI_OK) {
        LOGE("Failed to attach thread to JVM");
        return nullptr;
    }

    // Local reference to callback object (copied under mutex protection)
    jobject local_callback = nullptr;

    // Get the callback reference
    pthread_mutex_lock(&g_callback_mutex);
    if (g_callback_object) {
        local_callback = env->NewLocalRef(g_callback_object);
    }
    pthread_mutex_unlock(&g_callback_mutex);

    if (!local_callback) {
        LOGW("No callback object available, tally thread will wait");
    }

    // Get the TallyCallback class and method once
    jclass tally_callback_class = nullptr;
    jmethodID callback_method = nullptr;

    if (local_callback) {
        // Get the object's class
        jclass obj_class = env->GetObjectClass(local_callback);
        if (obj_class) {
            // Get the onTallyStateChange method - it should be (ZZ)V
            callback_method = env->GetMethodID(obj_class, "onTallyStateChange", "(ZZ)V");
            if (!callback_method) {
                // Check if the object implements TallyCallback interface
                if (env->ExceptionCheck()) {
                    env->ExceptionClear();
                }
                // Try to get the interface class
                jclass interface_class = env->FindClass("com/soerjo/ndi/internal/NDIWrapper$TallyCallback");
                if (interface_class) {
                    callback_method = env->GetMethodID(interface_class, "onTallyStateChange", "(ZZ)V");
                    env->DeleteLocalRef(interface_class);
                }
            }
            env->DeleteLocalRef(obj_class);
        }
    }

    if (!callback_method) {
        LOGE("Failed to get onTallyStateChange method ID");
        if (local_callback) {
            env->DeleteLocalRef(local_callback);
        }
        g_jvm->DetachCurrentThread();
        return nullptr;
    }

    // Poll for tally state changes
    while (!g_tally_thread_stop_requested.load()) {
        if (g_current_sender && g_current_sender->p_send && pNDI) {
            // Get current tally state from NDI
            NDIlib_tally_t tally_state;
            bool success = pNDI->send_get_tally(
                reinterpret_cast<NDIlib_send_instance_t>(g_current_sender->p_send),
                &tally_state,
                0  // timeout in milliseconds (0 = non-blocking)
            );

            if (success) {
                // Check if state changed
                if (tally_state.on_preview != g_prev_on_preview ||
                    tally_state.on_program != g_prev_on_program) {

                    LOGD("Tally state changed - Preview: %d, Program: %d",
                         tally_state.on_preview, tally_state.on_program);

                    // Update previous state
                    g_prev_on_preview = tally_state.on_preview;
                    g_prev_on_program = tally_state.on_program;

                    // Call Java callback if we have a valid reference
                    if (local_callback && callback_method) {
                        env->CallVoidMethod(
                            local_callback,
                            callback_method,
                            tally_state.on_preview ? JNI_TRUE : JNI_FALSE,
                            tally_state.on_program ? JNI_TRUE : JNI_FALSE
                        );

                        // Check for exception
                        if (env->ExceptionCheck()) {
                            env->ExceptionDescribe();
                            env->ExceptionClear();
                        }
                    } else {
                        // Re-fetch callback if it became null
                        pthread_mutex_lock(&g_callback_mutex);
                        if (g_callback_object && !local_callback) {
                            local_callback = env->NewLocalRef(g_callback_object);
                        }
                        pthread_mutex_unlock(&g_callback_mutex);
                    }
                }
            }
        }

        // Sleep for 100ms between polls (10 Hz polling rate)
        usleep(100000);  // 100ms in microseconds
    }

    // Clean up local references
    if (local_callback) {
        env->DeleteLocalRef(local_callback);
    }

    // Detach thread from JVM
    g_jvm->DetachCurrentThread();

    LOGD("Tally polling thread stopped");
    return nullptr;
}

// Start the tally polling thread
void start_tally_thread() {
    if (g_tally_thread_running.load()) {
        LOGD("Tally thread already running");
        return;
    }

    g_tally_thread_stop_requested.store(false);
    int result = pthread_create(&g_tally_thread, nullptr, tally_poll_thread, nullptr);
    if (result == 0) {
        g_tally_thread_running.store(true);
        LOGD("Tally polling thread created successfully");
    } else {
        LOGE("Failed to create tally polling thread");
    }
}

// Stop the tally polling thread
void stop_tally_thread() {
    if (!g_tally_thread_running.load()) {
        return;
    }

    LOGD("Stopping tally polling thread...");
    g_tally_thread_stop_requested.store(true);

    // Wait for thread to finish
    if (g_tally_thread != 0) {
        pthread_join(g_tally_thread, nullptr);
        g_tally_thread = 0;
    }

    g_tally_thread_running.store(false);
    LOGD("Tally polling thread stopped");
}

extern "C" {

JNIEXPORT jboolean JNICALL Java_com_soerjo_ndi_internal_NDIWrapper_nativeInitialize(JNIEnv* env, jobject thiz) {
    if (ndi_initialized && pNDI) {
        LOGD("NDI already initialized");
        return JNI_TRUE;
    }

    // Get Java VM reference for callback thread
    jint result = env->GetJavaVM(&g_jvm);
    if (result != JNI_OK) {
        LOGE("Failed to get Java VM reference");
        return JNI_FALSE;
    }

    // Explicitly load the NDI library first
    hNDI = dlopen("libndi.so", RTLD_NOW | RTLD_LOCAL);
    if (!hNDI) {
        LOGE("Failed to load NDI library: %s", dlerror());
        return JNI_FALSE;
    }

    // Load the NDI functions dynamically
    auto NDIlib_v6_load = (NDIlib_v6_3*(*)())dlsym(hNDI, "NDIlib_v6_load");
    if (!NDIlib_v6_load) {
        LOGE("Failed to find NDIlib_v6_load symbol: %s", dlerror());
        dlclose(hNDI);
        hNDI = nullptr;
        return JNI_FALSE;
    }

    pNDI = NDIlib_v6_load();
    if (!pNDI) {
        LOGE("Failed to load NDI library via NDIlib_v6_load");
        dlclose(hNDI);
        hNDI = nullptr;
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

JNIEXPORT jlong JNICALL Java_com_soerjo_ndi_internal_NDIWrapper_nativeCreateSender(JNIEnv* env, jobject thiz, jstring source_name) {
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

    // Set as current sender for tally polling
    g_current_sender = sender;

    LOGD("NDI sender created: %s (handle: %p)", source_name_str, sender);
    return reinterpret_cast<jlong>(sender);
}

// YUV_420_888 to NV12 conversion
JNIEXPORT jbyteArray JNICALL Java_com_soerjo_ndi_internal_NDIWrapper_nativeConvertYuv420ToNv12(
    JNIEnv* env,
    jobject thiz,
    jobject y_plane,
    jint y_row_stride,
    jint y_pixel_stride,
    jobject u_plane,
    jint u_row_stride,
    jint u_pixel_stride,
    jobject v_plane,
    jint v_row_stride,
    jint v_pixel_stride,
    jint width,
    jint height
) {
    if (!y_plane || !u_plane || !v_plane || width <= 0 || height <= 0) {
        LOGE("Invalid YUV plane parameters");
        return nullptr;
    }

    void* y_buf = env->GetDirectBufferAddress(y_plane);
    void* u_buf = env->GetDirectBufferAddress(u_plane);
    void* v_buf = env->GetDirectBufferAddress(v_plane);

    if (!y_buf || !u_buf || !v_buf) {
        LOGE("Failed to get direct buffer addresses");
        return nullptr;
    }

    const uint8_t* y_plane_data = static_cast<const uint8_t*>(y_buf);
    const uint8_t* u_plane_data = static_cast<const uint8_t*>(u_buf);
    const uint8_t* v_plane_data = static_cast<const uint8_t*>(v_buf);

    int y_size = width * height;
    int uv_width = width / 2;
    int uv_height = height / 2;
    int uv_size = uv_width * uv_height * 2;

    jbyteArray nv12_array = env->NewByteArray(y_size + uv_size);
    if (!nv12_array) {
        LOGE("Failed to allocate NV12 buffer");
        return nullptr;
    }

    jbyte* nv12_data = env->GetByteArrayElements(nv12_array, nullptr);
    if (!nv12_data) {
        LOGE("Failed to get NV12 array elements");
        env->DeleteLocalRef(nv12_array);
        return nullptr;
    }

    uint8_t* nv12_dst = reinterpret_cast<uint8_t*>(nv12_data);

    if (y_pixel_stride == 1 && y_row_stride == width) {
        memcpy(nv12_dst, y_plane_data, y_size);
    } else {
        for (int y = 0; y < height; y++) {
            const uint8_t* src_row = y_plane_data + y * y_row_stride;
            uint8_t* dst_row = nv12_dst + y * width;
            for (int x = 0; x < width; x++) {
                dst_row[x] = src_row[x * y_pixel_stride];
            }
        }
    }

    uint8_t* uv_dst = nv12_dst + y_size;
    for (int y = 0; y < uv_height; y++) {
        const uint8_t* u_row = u_plane_data + y * u_row_stride;
        const uint8_t* v_row = v_plane_data + y * v_row_stride;
        uint8_t* dst_row = uv_dst + y * width;

        for (int x = 0; x < uv_width; x++) {
            int src_u_idx = x * u_pixel_stride;
            int src_v_idx = x * v_pixel_stride;
            dst_row[x * 2] = u_row[src_u_idx];
            dst_row[x * 2 + 1] = v_row[src_v_idx];
        }
    }

    env->ReleaseByteArrayElements(nv12_array, nv12_data, 0);
    return nv12_array;
}

JNIEXPORT jboolean JNICALL Java_com_soerjo_ndi_internal_NDIWrapper_nativeSendFrame(JNIEnv* env, jobject thiz,
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

    // Calculate line stride (in bytes for UYVY format: 2 bytes per pixel)
    video_frame.line_stride_in_bytes = stride;

    // Set up the frame data
    video_frame.p_data = reinterpret_cast<uint8_t*>(frame_data);

    // Set the frame format - use NV12 for optimized performance
    // NV12 reduces bandwidth by 25% vs UYVY (12bpp vs 16bpp)
    video_frame.FourCC = NDIlib_FourCC_type_NV12;

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

JNIEXPORT void JNICALL Java_com_soerjo_ndi_internal_NDIWrapper_nativeDestroySender(JNIEnv* env, jobject thiz, jlong handle) {
    if (handle == 0) {
        return;
    }

    NDISender* sender = reinterpret_cast<NDISender*>(handle);

    // Stop tally thread if this was the current sender
    if (g_current_sender == sender) {
        stop_tally_thread();
        g_current_sender = nullptr;
        g_prev_on_preview = false;
        g_prev_on_program = false;
    }

    if (sender && sender->p_send && pNDI) {
        pNDI->send_destroy(reinterpret_cast<NDIlib_send_instance_t>(sender->p_send));
        LOGD("NDI sender destroyed (handle: %p)", sender);
    }

    if (sender) {
        delete sender;
    }
}

JNIEXPORT void JNICALL Java_com_soerjo_ndi_internal_NDIWrapper_nativeCleanup(JNIEnv* env, jobject thiz) {
    // Stop tally thread
    stop_tally_thread();

    // Clean up callback references
    pthread_mutex_lock(&g_callback_mutex);
    if (g_callback_object) {
        // Need to get JNIEnv for DeleteGlobalRef
        JNIEnv* current_env;
        bool attached = false;
        jint result = g_jvm->GetEnv((void**)&current_env, JNI_VERSION_1_6);
        if (result == JNI_EDETACHED) {
            result = g_jvm->AttachCurrentThread(&current_env, nullptr);
            if (result == JNI_OK) {
                attached = true;
            }
        }

        if (current_env) {
            current_env->DeleteGlobalRef(g_callback_object);
            g_callback_object = nullptr;
        }

        if (attached) {
            g_jvm->DetachCurrentThread();
        }
    }
    g_callback_method = nullptr;
    pthread_mutex_unlock(&g_callback_mutex);

    if (ndi_initialized && pNDI) {
        pNDI->destroy();
        pNDI = nullptr;
        ndi_initialized = false;
        LOGD("NDI cleanup complete");
    }

    // Unload the NDI library if it was loaded
    if (hNDI) {
        dlclose(hNDI);
        hNDI = nullptr;
        LOGD("NDI library unloaded");
    }
}

JNIEXPORT void JNICALL Java_com_soerjo_ndi_internal_NDIWrapper_nativeSetTallyCallback(JNIEnv* env, jobject thiz, jlong handle, jobject callback) {
    if (handle == 0) {
        LOGE("Invalid sender handle for tally callback");
        return;
    }

    if (!callback) {
        LOGE("Callback object is null");
        return;
    }

    // Verify the callback object implements the TallyCallback interface
    jclass callback_class = env->GetObjectClass(callback);
    if (!callback_class) {
        LOGE("Failed to get callback class");
        return;
    }

    // Get the onTallyStateChange method to verify it exists
    jmethodID test_method = env->GetMethodID(callback_class, "onTallyStateChange", "(ZZ)V");
    env->DeleteLocalRef(callback_class);

    if (!test_method) {
        LOGE("Callback object does not have onTallyStateChange(ZZ)V method");
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
        }
        return;
    }

    // Store the callback object with mutex protection
    pthread_mutex_lock(&g_callback_mutex);

    // Delete old global reference if exists
    if (g_callback_object) {
        env->DeleteGlobalRef(g_callback_object);
    }

    // Create a new global reference to the callback object
    g_callback_object = env->NewGlobalRef(callback);
    if (!g_callback_object) {
        LOGE("Failed to create global reference to callback object");
        pthread_mutex_unlock(&g_callback_mutex);
        return;
    }

    pthread_mutex_unlock(&g_callback_mutex);

    // Start the tally polling thread
    start_tally_thread();

    LOGD("Tally callback registered successfully and polling thread started");
}

} // extern "C"
