package com.linorz.finger;

/**
 * Created by linorz on 16-11-15.
 */

public class NDKloader {
    static {
        System.loadLibrary("native-lib");
    }

    //自带测试
    public static native String stringFromJNI();

    //变灰
    public static native int[] getGrayImage(int[] pixels, int w, int h);

    //识别手指
    public static native int[][] checkFinger(int[] pixels, int w, int h);

}
