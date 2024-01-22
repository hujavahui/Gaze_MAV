package com.cv.tnn.model;

import android.graphics.Color;
import android.graphics.RectF;
import android.util.Log;

import java.util.Random;
import java.util.Vector;

import static androidx.camera.core.CameraXThreads.TAG;

public class FrameInfo {

    public float xmin, ymin, xmax, ymax;
    public int label;
    public float score;
    public Vector<KeyPoint> keypoints = new Vector<KeyPoint>();
    public Vector<HeadAngle> headangle = new Vector<HeadAngle>();
    public final String TAG = "FrameInfo";
    private static final String[] class_name = {"BACKGROUND", "face", "person"};

    /***
     * 添加检测框
     * @param x 检测框左上角x
     * @param y 检测框左上角y
     * @param w 检测框宽度width
     * @param h 检测框高度height
     * @param label 检测框的ID(label)
     * @param score 检测框的分数
     */
    public void addBox(float x, float y, float w, float h, int label, float score) {
        this.xmin = x;
        this.ymin = y;
        this.xmax = x + w;
        this.ymax = y + h;
        this.label = label;
        this.score = score;
        //String box_info = String.format("box:[%f,%f,%f，%f] label:%d,score:%f \n", xmin, ymin, xmax, ymax, label, score);
        //Log.w(TAG, box_info);
    }

    /***
     * 添加检测框
     * @param yaw 检测框左上角x
     * @param pitch 检测框左上角y
     * @param roll 检测框宽度width
     */
    public void addHeadPose(float yaw, float pitch, float roll) {
        HeadAngle ha = new HeadAngle(yaw, pitch, roll);
        this.headangle.add(ha);
        String info = String.format("HeadPose: yaw, pitch, roll=[%f,%f,%f]\n", yaw, pitch, roll);
        Log.w(TAG, info);
    }


    /***
     * 添加关键点坐标  就是landmarks
     * @param x 关键点坐标x
     * @param y 关键点坐标y
     * @param score 关键点分数
     */
    public void addKeyPoint(float x, float y, float score) {
        //String info = String.format("point:[%f,%f] score:%f \n", x, y, score);
        //Log.w(TAG, info);
        KeyPoint kp = new KeyPoint(x, y, score);
        this.keypoints.add(kp);
    }

    /***
     * 获得关键点
     * @return
     */
    public Vector<KeyPoint> getKeyPoints() {
        return keypoints;
    }

    /***
     * 获得头部姿态
     * @return
     */
    public Vector<HeadAngle> getHeadPos() {
        return headangle;
    }

    /***
     * 获得检测框
     * @return
     */
    public RectF getRect() {
        return new RectF(xmin, ymin, xmax, ymax);
    }

    /***
     * 获得label名称
     * @return
     */
    public String getClassName() {
        return class_name[label];
    }

    /***
     * 获得label ID
     * @return
     */
    public int getLabel() {
        return this.label;
    }

    /***
     * 获得检测框的分数
     * @return
     */
    public float getScore() {
        return score;
    }

    /***
     * 获得检测框的颜色，用于绘制图像
     * @return
     */
    public int getColor() {
        Random random = new Random(label);
        return Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256));
    }

}

