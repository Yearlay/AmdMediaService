LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := scanjni
LOCAL_SRC_FILES := scan_jni.c hz2py.c
LOCAL_LDLIBS := -llog -landroid
include $(BUILD_SHARED_LIBRARY)