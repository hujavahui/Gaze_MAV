package com.cv.tnn.model;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import java.util.Locale;


public class DrawImage {

    private static final String TAG = "dm-DrawImage";
    static Paint mPointPaint = new Paint();
    static Paint mLinePaint = new Paint();
    static Paint mTextPaint = new Paint();

    public static void initPaint() {
        mLinePaint.setAlpha(200);
        mLinePaint.setStyle(Paint.Style.STROKE);

        mPointPaint.setStyle(Paint.Style.FILL);//填充
        mPointPaint.setAntiAlias(true);
        mPointPaint.setDither(true);
        mPointPaint.setColor(Color.RED);//设置画笔颜色
        mPointPaint.setStyle(Paint.Style.STROKE);
        mPointPaint.setStrokeJoin(Paint.Join.ROUND);
        mPointPaint.setStrokeCap(Paint.Cap.ROUND);

        mTextPaint.setStyle(Paint.Style.STROKE);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setColor(Color.BLUE);//设置画笔颜色
    }

    /***
     * 绘制框
     * @param bitmap
     * @param results
     * @return
     */
    public static Bitmap drawRects(Bitmap bitmap, FrameInfo[] results) {
        if (results == null || results.length <= 0) {
            return bitmap;
        }
        Canvas canvas = new Canvas(bitmap);
        mLinePaint.setStrokeWidth(4 * bitmap.getWidth() / 800.0f);
        mLinePaint.setTextSize(30 * bitmap.getWidth() / 800.0f);
        for (FrameInfo info : results) {
            mLinePaint.setColor(info.getColor());
            mLinePaint.setStyle(Paint.Style.FILL);
            canvas.drawText(info.getClassName() + String.format(Locale.CHINESE, " %.3f", info.getScore()), info.xmin + 3, info.ymin + 30 * bitmap.getWidth() / 1000.0f, mLinePaint);
            mLinePaint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(info.getRect(), mLinePaint);
        }
        return bitmap;
    }

    /***
     * 绘制点和文本
     * @param bitmap
     * @param results
     * @return
     */
    public static Bitmap drawPoints(Bitmap bitmap, FrameInfo[] results) {
        if (results == null || results.length <= 0) {
            return bitmap;
        }
        Canvas canvas = new Canvas(bitmap);
        mPointPaint.setStrokeWidth(20 * bitmap.getWidth() / 800.0f);
        mTextPaint.setStrokeWidth(1 * bitmap.getWidth() / 800.0f);
        for (int k = 0; k < results.length; k++) {
            FrameInfo info = results[k];
            int num_kp = info.getKeyPoints().size();
            for (int i = 0; i < num_kp; i++) {
                KeyPoint kp = info.getKeyPoints().get(i);
                String kp_info = String.format(Locale.CHINESE, "%d-%f \n", i, kp.score);
                canvas.drawPoint(kp.point.x, kp.point.y, mPointPaint);
                canvas.drawText(kp_info, kp.point.x + 5, kp.point.y, mTextPaint);
            }
        }
        return bitmap;
    }


    /***
     * 绘制连接线
     * @param bitmap
     * @param results
     * @param skeleton
     * @return
     */
    public static Bitmap drawLines(Bitmap bitmap, FrameInfo[] results, int[][] skeleton) {
        if (results == null || results.length <= 0) {
            return bitmap;
        }
        Canvas canvas = new Canvas(bitmap);
        mLinePaint.setStyle(Paint.Style.FILL);//填充
        mLinePaint.setAntiAlias(true);
        mLinePaint.setDither(true);
        mLinePaint.setColor(Color.GREEN);//设置画笔颜色
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeJoin(Paint.Join.ROUND);
        mLinePaint.setStrokeCap(Paint.Cap.ROUND);
        mLinePaint.setStrokeWidth(4);

        for (FrameInfo box : results) {
            int length = skeleton.length;
            for (int i = 0; i < length; i++) {
                KeyPoint p0 = box.getKeyPoints().get(skeleton[i][0]);
                KeyPoint p1 = box.getKeyPoints().get(skeleton[i][1]);
                if (p0.point.x > 0 && p0.point.y > 0 && p1.point.x > 0 && p1.point.y > 0) {
                    canvas.drawLine(p0.point.x, p0.point.y, p1.point.x, p1.point.y, mLinePaint);
                }
            }
        }
        return bitmap;
    }

    /***
     * 绘制FrameInfo结果
     * @param bitmap
     * @param results
     * @param skeleton
     * @return
     */
    public static Bitmap drawResult(Bitmap bitmap, FrameInfo[] results, int[][] skeleton) {
        initPaint();
        printResult(results);
        bitmap = drawRects(bitmap, results);
        //bitmap = drawLines(bitmap, results, skeleton);
        bitmap = drawPoints(bitmap, results);
        return bitmap;
    }

    /***
     * 打印结果信息
     * @param results
     */
    public static void printResult(FrameInfo[] results) {
        for (int k = 0; k < results.length; k++) {
            FrameInfo info = results[k];
            int num_kp = info.getKeyPoints().size();
            String box_info = String.format(Locale.CHINESE, "box:[%f,%f,%f，%f] label:%d,score:%f \n", info.xmin, info.ymin, info.xmax, info.ymax, info.label, info.score);
            Log.w(TAG, box_info);
            for (int i = 0; i < num_kp; i++) {
                KeyPoint kp = info.getKeyPoints().get(i);
                String kp_info = String.format(Locale.CHINESE, "ID:%d point:[%f,%f] score:%f \n", k, kp.point.x, kp.point.y, kp.score);
                Log.w(TAG, kp_info);
            }
        }
    }

    /***
     * 缩放图片
     * @param bitmap
     * @param resize_width
     * @param resize_height
     * @return
     */
    public static Bitmap resizeBitmap(Bitmap bitmap, int resize_width, int resize_height) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (resize_width <= 0 && resize_height <= 0) {
            return bitmap;
        } else if (resize_height <= 0) {
            resize_height = height * resize_width / width;
        } else if (resize_width <= 0) {
            resize_width = width * resize_height / height;
        }
        Bitmap dst = Bitmap.createScaledBitmap(bitmap, resize_width, resize_height, false);
        return dst;
    }
}
