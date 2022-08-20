cmake_minimum_required(VERSION 3.22.1)
project(twifucker)

enable_language(CXX C)
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_C_STANDARD 11)

find_library(ANDROID_LIBS log)

option(DEX_BUILDER_BUILD_SHARED "" OFF)
option(ABSL_PROPAGATE_CXX_STD "" ON)

add_subdirectory(dex_builder)

add_library(twifucker SHARED twifucker.cc)

set_target_properties(twifucker PROPERTIES
        CXX_EXTENSIONS OFF
        POSITION_INDEPENDENT_CODE ON
        )

target_link_libraries(twifucker ${ANDROID_LIBS} dex_builder_static)
