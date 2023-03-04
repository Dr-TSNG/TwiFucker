LOCAL_PATH := $(call my-dir)
define walk
  $(wildcard $(1)) $(foreach e, $(wildcard $(1)/*), $(call walk, $(e)))
endef

include $(CLEAR_VARS)
LOCAL_MODULE           := twifucker
FILE_LIST              := $(filter %.cpp, $(call walk, $(LOCAL_PATH)/src))
LOCAL_SRC_FILES        := $(FILE_LIST:COMMON_FILE_LIST:$(LOCAL_PATH)/%=%)
LOCAL_STATIC_LIBRARIES := cxx nativehelper_header_only linux-syscall-support
LOCAL_LDLIBS           := -llog
include $(BUILD_SHARED_LIBRARY)

include src/main/cpp/external/Android.mk
$(call import-module,prefab/cxx)
$(call import-module,prefab/nativehelper)
