package com.linorz.finger;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by linorz on 16-11-15.
 */

public class DrawSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    protected SurfaceHolder holder;
    private int mWidth;
    private int mHeight;
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            clearDraw();
            int[][] points = (int[][]) message.obj;
            //锁定canvas
            Canvas canvas = holder.lockCanvas();
            canvas.drawColor(Color.TRANSPARENT);//这里是绘制背景
            for (int i = 0; i < points.length; i++) {
                for (int j = 0; j < points[i].length; j++) {
                    Paint p = new Paint(); //笔触
                    p.setAntiAlias(true); //反锯齿
                    if (i == 0) p.setColor(Color.RED);
                    else p.setColor(Color.GREEN);
                    p.setStyle(Paint.Style.FILL_AND_STROKE);
                    canvas.drawCircle(points[i][0], points[i][1], 40, p);
                }
            }
            //释放canvas对象，并发送到SurfaceView
            holder.unlockCanvasAndPost(canvas);
            return false;
        }
    });

    public DrawSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        holder = this.getHolder();
        holder.addCallback(this);
        holder.setFormat(PixelFormat.TRANSPARENT);
        setZOrderOnTop(true);
    }

    public void surfaceChanged(SurfaceHolder arg0, int arg1, int w, int h) {
        mWidth = w;
        mHeight = h;
    }

    public void surfaceCreated(SurfaceHolder arg0) {

    }

    public void surfaceDestroyed(SurfaceHolder arg0) {

    }

    void clearDraw() {
        Canvas canvas = holder.lockCanvas();
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        holder.unlockCanvasAndPost(canvas);
    }

    public void drawCircle(final float x, final float y, final int color) {
        clearDraw();
        new Thread(new Runnable() {
            @Override
            public void run() {
                //锁定canvas
                Canvas canvas = holder.lockCanvas();
                canvas.drawColor(Color.TRANSPARENT);//这里是绘制背景
                Paint p = new Paint(); //笔触
                p.setAntiAlias(true); //反锯齿
                p.setColor(color);
                p.setStyle(Paint.Style.FILL_AND_STROKE);
                canvas.drawCircle(x, y, 10, p);
                //释放canvas对象，并发送到SurfaceView
                holder.unlockCanvasAndPost(canvas);
            }
        }).start();
    }

    public void drawCircleList(final int[][] points) {
        Message message = new Message();
        message.what = 1;
        message.obj = points;

        handler.sendMessageDelayed(message, 0);
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//
//            }
//        }).start();
    }

    /**
     * 在预览摄像头上划线
     *
     * @param cornerPoints 得到的4个角点
     */
    public void drawLine(int[] cornerPoints) { //绘画
        Canvas canvas = holder.lockCanvas();
        canvas.drawColor(Color.TRANSPARENT);
        Paint paint = new Paint(); //清屏
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawPaint(paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        if (cornerPoints == null) {
            Log.d("wcj", "返回值为空");
            holder.unlockCanvasAndPost(canvas);
            return;
        }
        if (cornerPoints.length == 0) {
            Log.d("wcj", "返回值为空");
            holder.unlockCanvasAndPost(canvas);
            return;
        }
        paint.setAntiAlias(true);
        paint.setColor(Color.rgb(56, 210, 212));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(7);
        //============== //绘图操作 //===============
        holder.unlockCanvasAndPost(canvas);
    }
}

