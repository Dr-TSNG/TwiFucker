#include <fcntl.h>
#include <jni.h>
#include <linux_syscall_support.h>
#include <nativehelper/scoped_utf_chars.h>
#include <thread>
#include <unistd.h>

#include "genuine.h"
#include "log.h"

[[gnu::always_inline]]
static inline bool isApkSigBlock42(const char* buffer) {
    // APK Sig Block 42
    return *buffer == 'A'
           && *++buffer == 'P'
           && *++buffer == 'K'
           && *++buffer == ' '
           && *++buffer == 'S'
           && *++buffer == 'i'
           && *++buffer == 'g'
           && *++buffer == ' '
           && *++buffer == 'B'
           && *++buffer == 'l'
           && *++buffer == 'o'
           && *++buffer == 'c'
           && *++buffer == 'k'
           && *++buffer == ' '
           && *++buffer == '4'
           && *++buffer == '2';
}

[[gnu::always_inline]]
static inline int checkSignature(const char* path) {
    unsigned char buffer[0x11] = {0};
    uint32_t size4;
    uint64_t size8, size_of_block;

#ifdef DEBUG
    LOGI("check signature for %s", path);
#endif

    int sign = -1;
    int fd = (int) sys_openat(AT_FDCWD, path, O_RDONLY, 0);
    if (fd < 0) {
        return sign;
    }

    sign = 1;
    // https://en.wikipedia.org/wiki/Zip_(file_format)#End_of_central_directory_record_(EOCD)
    for (int i = 0;; ++i) {
        unsigned short n;
        sys_lseek(fd, -i - 2, SEEK_END);
        sys_read(fd, &n, 2);
        if (n == i) {
            sys_lseek(fd, -22, SEEK_CUR);
            sys_read(fd, &size4, 4);
            if ((size4 ^ 0xcafebabeu) == 0xccfbf1eeu) {
                break;
            }
        }
        if (i == 0xffff) {
            goto clean;
        }
    }

    sys_lseek(fd, 12, SEEK_CUR);
    // offset
    sys_read(fd, &size4, 0x4);
    sys_lseek(fd, (off_t) (size4 - 0x18), SEEK_SET);

    sys_read(fd, &size8, 0x8);
    sys_read(fd, buffer, 0x10);
    if (!isApkSigBlock42((char*) buffer)) {
        goto clean;
    }

    sys_lseek(fd, (off_t) (size4 - (size8 + 0x8)), SEEK_SET);
    sys_read(fd, &size_of_block, 0x8);
    if (size_of_block != size8) {
        goto clean;
    }

    for (;;) {
        uint32_t id;
        uint32_t offset;
        sys_read(fd, &size8, 0x8); // sequence length
        if (size8 == size_of_block) {
            break;
        }
        sys_read(fd, &id, 0x4); // id
        offset = 4;
        if ((id ^ 0xdeadbeefu) == 0xafa439f5u || (id ^ 0xdeadbeefu) == 0x2efed62f) {
            sys_read(fd, &size4, 0x4); // signer-sequence length
            sys_read(fd, &size4, 0x4); // signer length
            sys_read(fd, &size4, 0x4); // signed data length
            offset += 0x4 * 3;

            sys_read(fd, &size4, 0x4); // digests-sequence length
            sys_lseek(fd, (off_t) (size4), SEEK_CUR);// skip digests
            offset += 0x4 + size4;

            sys_read(fd, &size4, 0x4); // certificates length
            sys_read(fd, &size4, 0x4); // certificate length
            offset += 0x4 * 2;
#if defined(GENUINE_SIZE) && defined(GENUINE_HASH)
            if (size4 == GENUINE_SIZE) {
                int hash = 1;
                signed char c;
                for (unsigned i = 0; i < size4; ++i) {
                    sys_read(fd, &c, 0x1);
                    hash = 31 * hash + c;
                }
                offset += size4;
                if ((((unsigned) hash) ^ 0x14131211u) == GENUINE_HASH) {
                    sign = 0;
                    break;
                }
            }
#else
            sign = 0;
            break;
#endif
        }
        sys_lseek(fd, (off_t) (size8 - offset), SEEK_CUR);
    }

    clean:
    close(fd);

    return sign;
}

extern "C"
JNIEXPORT void JNICALL
Java_icu_nullptr_twifucker_hook_HookEntry_nativeInit(JNIEnv* env, jobject thiz, jobject context, jstring module_path) {
    auto path = ScopedUtfChars(env, module_path);
    if (checkSignature(path.c_str()) != 0) {
        std::thread([] {
            sleep(5);
            exit(0);
        }).detach();
        return;
    }
    auto mmkv = env->FindClass("com/tencent/mmkv/MMKV");
    auto init = env->GetStaticMethodID(mmkv, "initialize", "(Landroid/content/Context;)Ljava/lang/String;");
    env->CallStaticObjectMethod(mmkv, init, context);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_icu_nullptr_twifucker_UtilsKt_nativeLoadPrefs(JNIEnv* env, jclass clazz) {
    auto mmkv = env->FindClass("com/tencent/mmkv/MMKV");
    auto mmkvWithID = env->GetStaticMethodID(mmkv, "mmkvWithID", "(Ljava/lang/String;)Lcom/tencent/mmkv/MMKV;");
    auto name = env->NewStringUTF("twifucker");
    auto mmkvInstance = env->CallStaticObjectMethod(mmkv, mmkvWithID, name);
    return mmkvInstance;
}
