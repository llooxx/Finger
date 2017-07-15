#CMake版本信息
cmake_minimum_required(VERSION 3.4.1)


#工程路径
set(pathToProject E:/workspace/workspaceAndroid/finger)
#OpenCV-android-sdk路径
set(pathToOpenCv E:/workspace/workspaceAndroid/OpenCV-android-sdk)

#支持-std=gnu++11
set(CMAKE_VERBOSE_MAKEFILE on)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")

#配置加载native依赖
include_directories(${pathToOpenCv}/sdk/native/jni/include)

#CPP文件夹下带编译的cpp文件
add_library( native-lib SHARED src/main/cpp/native-lib.cpp )

#动态方式加载
add_library( lib_opencv SHARED IMPORTED )

#引入libopencv_java3.so文件
set_target_properties(lib_opencv PROPERTIES IMPORTED_LOCATION ${pathToProject}/app/src/main/jniLibs/${ANDROID_ABI}/libopencv_java3.so)

#C++日志
find_library( log-lib log )

#target_link_libraries( native-lib $\{log-lib} )
target_link_libraries( native-lib $\{log-lib} lib_opencv)
