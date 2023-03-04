LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE            := linux-syscall-support
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/linux-syscall-support
include $(BUILD_SHARED_LIBRARY)
