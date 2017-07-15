//
// Created by linorz on 16-11-15.
//
#include <opencv2/opencv.hpp>
#include <iostream>
#include <string>

using namespace std;
using namespace cv;
#ifndef FINGER_NATIVE_LIB_H
#define FINGER_NATIVE_LIB_H

//缩放
void Scale(Mat &input, Mat &output, double sx, double sy) {
    long step = input.step[0];
    int channels = input.channels(), width = input.cols, height = input.rows, x3, y3;
    output.create((int) (height * sy), (int) (width * sx), input.type());
    double x2, y2, u, v;
    for (int y = 0; y < output.rows; ++y) {
        for (int x = 0; x < output.cols; ++x) {
            y2 = y / sy;
            x2 = x / sx;
            x3 = (int) x2;
            y3 = (int) y2;
            u = x2 - x3;
            v = y2 - y3;
            for (int i = 0; i < channels; ++i)
                output.data[y * output.step + x * channels + i] = (uchar)
                        ((1 - u) * (1 - v) * input.data[y3 * step + x3 * channels + i]
                         + (1 - u) * v * input.data[(y3 + 1) * step + x3 * channels + i]
                         + u * (1 - v) * input.data[y3 * step + (x3 + 1) * channels + i]
                         + u * v * input.data[(y3 + 1) * step + (x3 + 1) * channels + i]);
        }
    }
}

void MeanFilter(Mat &in, Mat &out, int window_size) {
    //处理使边长为奇数
    if (((window_size >> 2) << 2) == window_size)
        window_size = window_size + 1;
    int height, width, channels;
    unsigned char *data;
    int y, x;
    int w = (window_size - 1) / 2;
    int z = window_size * window_size;
    unsigned long value = 0;

    height = in.rows;
    width = in.cols;
    long step = (long) in.step[0];
    channels = in.channels();
    in.copyTo(out);
    data = out.data;

    //积分图
    unsigned long *s = new unsigned long[height * step];
    //行处理
    for (y = 0; y < height; y++)
        for (x = channels; x < step; x++)
            s[y * step + x] = s[y * step + x - channels] + data[y * step + x];
    //列处理
    for (y = 1; y < height; y++)
        for (x = channels; x < step; x++)
            s[y * step + x] += s[(y - 1) * step + x];

    //去噪，均值处理
    int x1 = w + 1, x2 = width - w - 1, y1 = w + 1, y2 = height - w - 1;
    bool f = true;
    for (y = 0; y < height; y++) {
        for (x = 0; x < width; x++) {
            int xx = x, yy = y, zz = z;
            if (x < x1) {
                x = x1;
                f = false;
            } else if (x > x2) {
                x = x2;
                f = false;
            }
            if (y < y1) {
                y = y1;
                f = false;
            } else if (y > y2) {
                y = y2;
                f = false;
            }
            long min_x = (x - w - 1) * channels;
            long max_x = (x + w) * channels;
            long min_y = (y - w - 1) * step;
            long max_y = (y + w) * step;
            if (!f)zz = (int) ((max_x - min_x) / channels * (max_y - min_y) / step);
            for (int i = 0; i < channels; ++i) {
                value = s[max_y + max_x + i] - s[min_y + max_x + i] -
                        s[max_y + min_x + i] + s[min_y + min_x + i];
                data[yy * step + xx * channels + i] = (unsigned char) (value / zz);
            }
            x = xx;
            y = yy;
        }
    }
    delete[]s;
}

//肤色提取，skinArea为二值化肤色图像
void skinExtract(const Mat &frame, Mat &skinArea) {
    skinArea.create(frame.rows, frame.cols, CV_8UC1);
    Mat YCbCr;
    vector<Mat> planes;

    //转换为YCrCb颜色空间
    cvtColor(frame, YCbCr, CV_RGB2YCrCb);
    //将多通道图像分离为多个单通道图像
    split(YCbCr, planes);

    //运用迭代器访问矩阵元素
    MatIterator_<uchar> it_Cb = planes[1].begin<uchar>();
    MatIterator_<uchar> it_Cb_end = planes[1].end<uchar>();
    MatIterator_<uchar> it_Cr = planes[2].begin<uchar>();
    MatIterator_<uchar> it_skin = skinArea.begin<uchar>();

    //人的皮肤颜色在YCbCr色度空间的分布范围:100<=Cb<=127, 138<=Cr<=170
    for (; it_Cb != it_Cb_end; ++it_Cr, ++it_Cb, ++it_skin) {
        if (138 <= *it_Cr && *it_Cr <= 170 && 100 <= *it_Cb && *it_Cb <= 127)
            *it_skin = 255;
        else
            *it_skin = 0;
    }

    //膨胀和腐蚀，膨胀可以填补凹洞（将裂缝桥接），腐蚀可以消除细的凸起（“斑点”噪声）
    dilate(skinArea, skinArea, Mat(5, 5, CV_8UC1), Point(-1, -1));
    erode(skinArea, skinArea, Mat(5, 5, CV_8UC1), Point(-1, -1));
}

#endif //FINGER_NATIVE_LIB_H
