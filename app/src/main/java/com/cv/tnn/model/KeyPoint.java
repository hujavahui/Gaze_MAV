package com.cv.tnn.model;

public class KeyPoint {
    public float score;
    public Point point;

    public KeyPoint(float x, float y, float score) {
        this.point = new Point(x, y);
        this.score = score;
    }
};
