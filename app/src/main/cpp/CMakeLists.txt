cmake_minimum_required(VERSION 3.22.1)
project("whisper_native")

add_library(whisper_native SHARED whisper_bridge.cpp)

find_library(log-lib log)

target_link_libraries(whisper_native ${log-lib})
