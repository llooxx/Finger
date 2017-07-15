#include <jni.h>
#include "native-lib.h"

//图像处理
extern "C" //兼容ｃ的语法
JNIEXPORT jintArray JNICALL
Java_com_linorz_finger_NDKloader_getGrayImage(
        JNIEnv *env, jobject,
        jintArray pixels_,
        jint w, jint h) {
    jint *pixels = env->GetIntArrayElements(pixels_, NULL);
    if (pixels == NULL) return NULL;
    Mat imgData(h, w, CV_8UC4, pixels);
    uchar *ptr = imgData.ptr(0);
    for (int i = 0; i < w * h; i++) {
        int grayScale = (int) (ptr[4 * i + 2] * 0.299 + ptr[4 * i + 1] * 0.587
                               + ptr[4 * i + 0] * 0.114);
        ptr[4 * i + 1] = (uchar) grayScale;
        ptr[4 * i + 2] = (uchar) grayScale;
        ptr[4 * i + 0] = (uchar) grayScale;
    }

    int size = w * h;
    jintArray result = env->NewIntArray(size);
    env->SetIntArrayRegion(result, 0, size, pixels);
    env->ReleaseIntArrayElements(pixels_, pixels, 0);
    return result;
}

extern "C"
JNIEXPORT jobjectArray  JNICALL
Java_com_linorz_finger_NDKloader_checkFinger(
        JNIEnv *env, jobject,
        jintArray buf,
        jint w, jint h) {
    jint *cbuf = env->GetIntArrayElements(buf, NULL);
    if (cbuf == NULL) return NULL;
    Mat frame(h, w, CV_8UC4, cbuf), dst, skinArea, show_img;
    env->ReleaseIntArrayElements(buf, cbuf, 0);
    env->DeleteLocalRef(buf);

    int scale = 5;
    Scale(frame, dst, 1.0 / scale, 1.0 / scale);
    frame = dst;
    MeanFilter(frame, dst, 10);

    skinExtract(dst, skinArea);
    frame.copyTo(show_img);

    vector<vector<Point> > contours;
    vector<Vec4i> hierarchy;
    //寻找轮廓
    findContours(skinArea, contours, hierarchy, CV_RETR_CCOMP, CV_CHAIN_APPROX_SIMPLE);

    // 找到最大的轮廓 index
    int index = 0;
    double area, maxArea(0);
    for (int i = 0; i < contours.size(); i++) {
        area = contourArea(Mat(contours[i]));
        if (area > maxArea) {
            maxArea = area;
            index = i;
        }
    }


    Moments moment = moments(skinArea, true);
    //图像重心
    Point center((int) (moment.m10 / moment.m00), (int) (moment.m01 / moment.m00));
    center.y *= 1.2;
    // 寻找指尖

    vector<Point> allPoints;
    if (contours.size() > 0)
//        for (int i = 0; i < contours.size(); i++)
//            allPoints.insert(allPoints.begin(), contours[i].begin(), contours[i].end());
        allPoints = contours[index];//边缘点
    vector<Point> fingerTips;//指尖
    Point currentPoint;//临时变量


    int max(0), count(0), key(0), last_dist(0);
    for (int i = 0; i < allPoints.size(); i++) {
        currentPoint = allPoints[i];
        //计算距离
        int dist = (currentPoint.x - center.x) * (currentPoint.x - center.x)
                   + (currentPoint.y - center.y) * (currentPoint.y - center.y);
        if (dist > max && dist >= last_dist) {
            max = dist;
            key = i;
            count = 0;
        } else {
            // 计算最大值保持的点数，如果大于40（这个值需要设置，本来想根据max值来设置，
            // 但是不成功，不知道为何），那么就认为这个是指尖
            count++;
            if (count > allPoints.size() / 20) {
                max = 0;
                bool flag = false;
                // 低于手心的点不算
                if (center.y < allPoints[key].y)
                    continue;
                // 离得太近的不算
                for (int j = 0; j < fingerTips.size(); j++) {
                    if (abs(allPoints[key].x - fingerTips[j].x) < allPoints.size() / 40
                        || abs(allPoints[key].y - fingerTips[j].y) < allPoints.size() / 40) {
                        flag = true;
                        break;
                    }
                }
                if (flag) continue;
                fingerTips.push_back(allPoints[key]);
            }
        }
        last_dist = dist;
    }

    jobjectArray result = env->NewObjectArray((jsize) (fingerTips.size() + 1),
                                              env->FindClass("[I"), NULL);
    int centerxy[2] = {center.x * scale, center.y * scale};
    jintArray j_centerxy = env->NewIntArray(2);
    env->SetIntArrayRegion(j_centerxy, 0, 2, centerxy);
    env->SetObjectArrayElement(result, 0, j_centerxy);
    env->DeleteLocalRef(j_centerxy);
    for (int i = 0; i < fingerTips.size(); ++i) {
        int xy[2] = {fingerTips[i].x * scale, fingerTips[i].y * scale};
        jintArray j_xy = env->NewIntArray(2);
        env->SetIntArrayRegion(j_xy, 0, 2, xy);
        env->SetObjectArrayElement(result, i + 1, j_xy);
        env->DeleteLocalRef(j_xy);
    }
    contours.swap(contours);
    hierarchy.swap(hierarchy);
    allPoints.swap(allPoints);
    fingerTips.swap(fingerTips);

    return result;
}
