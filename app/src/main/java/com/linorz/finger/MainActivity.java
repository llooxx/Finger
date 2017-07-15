package com.linorz.finger;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import java.io.ByteArrayOutputStream;
import java.util.List;

@SuppressLint("NewApi")
public class MainActivity extends Activity implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private Camera mCamera;
    private SurfaceView mPreview;
    private DrawSurfaceView top_view;
    private int screen_width = 0;
    private int screen_height = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 隐藏标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 隐藏状态栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main); //mPrevie为摄像机预览图层
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = StaticMethod.checkSelfPermissionArray(this, new String[]{
                    Manifest.permission.CAMERA
            });
            if (permissions.length > 0) {
                ActivityCompat.requestPermissions(this, permissions, 1);
            }
        }
        mPreview = (SurfaceView) findViewById(R.id.surface_view);
        top_view = (DrawSurfaceView) findViewById(R.id.surface_view_top);
        mPreview.getHolder().addCallback(this);
        mPreview.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mCamera = Camera.open();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCamera.stopPreview();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCamera.release();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) { //当surfaceView建立时，在上面绑定预览显示界面
        try {
            mCamera.setPreviewDisplay(mPreview.getHolder());
            mCamera.setPreviewCallback(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Camera.Parameters params = mCamera.getParameters();
        List<Camera.Size> sizes = params.getSupportedPreviewSizes();
        Camera.Size selectedSize = getBestSupportPreviewSize(sizes, getScreenSize()); //设定摄像机预览界面尺寸
        params.setPreviewSize(selectedSize.width, selectedSize.height);
        mCamera.setParameters(params);
        mCamera.setDisplayOrientation(90);
        mCamera.startPreview();
    }

    /**
     * 寻找最大的预览图片尺寸(与屏幕分辨率适配）
     *
     * @param previewSizes 所有支持的预览图片大小
     * @return
     */
    public static Camera.Size getBestSupportPreviewSize(List<Camera.Size> previewSizes, Camera.Size screenSize) {
        double screenRatio = screenSize.width * 1.0 / screenSize.height;
        Camera.Size maxSize = previewSizes.get(0);
        for (Camera.Size size : previewSizes) {
            double sizeRatio = size.width * 1.0 / size.height;
            if (size.width < 2000 && sizeRatio > screenRatio - 0.1 && sizeRatio < screenRatio + 0.1)
                maxSize = (size.width > maxSize.width) ? size : maxSize;
        }
        return maxSize;
    }

    private Camera.Size getScreenSize() {
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        int width = metric.widthPixels; // 宽度（PX）
        int height = metric.heightPixels; // 高度（PX）
        screen_width = width;
        screen_height = height;
        return mCamera.new Size(height, width);
    }

    int count = 0;

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        //把data转换为bitmap
//        count++;
//        if (count < 2) return;
//        count = 0;
        Camera.Size size = mCamera.getParameters().getPreviewSize();
        try {
            YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compressToJpeg(new Rect(0, 0, size.width, size.height), 100, stream);
            final Bitmap inputImage = rotateBitmap(BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size()), 90);
            stream.close();
            // inputBitmap为获取到的Bitmap，这里对其进行后续处理
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    int[][] fingers = findFinger(inputImage);
                    inputImage.recycle();
                    if (fingers != null) {
                        top_view.drawCircleList(fingers);
                    }

                }
            }, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param bmp    要旋转的图片
     * @param degree 图片旋转的角度，负值为逆时针旋转，正值为顺时针旋转
     * @return 旋转好的图片
     */
    public static Bitmap rotateBitmap(Bitmap bmp, float degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree); //此处bitmap默认为RGBA_8888
        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
        return bmp;
    }

    @Override
    public void onBackPressed() {
        mCamera.stopPreview();
        super.onBackPressed();
    }

    private int[][] findFinger(Bitmap bmp) {
        int w = bmp.getWidth();
        int h = bmp.getHeight();
        int[] pixels = StaticMethod.getPixels(bmp, w, h);
        int[][] fingers = NDKloader.checkFinger(pixels, w, h);
        double s_w = screen_width / w;
        double s_h = screen_height / h;
        for (int i = 0; i < fingers.length; i++) {
            fingers[i][0] *= s_w * 1.1;
            fingers[i][1] *= s_h * 1.2;
        }
        return fingers;
    }

}
