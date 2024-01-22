package com.cv.tnn.model;


public class Skeleton {
    public static int[][] finger_skeleton = {{0, 1}, {2, 3}};
    public static int[][] person_skeleton = {{0, 2}, {2, 4}, {1, 3}, {3, 5}, {6, 8}, {8, 10},
            {7, 9}, {9, 11}, {0, 1}, {6, 7}, {0, 6}, {1, 7}};

    public static int[][] get_skeleton(int type) {
        if (type == 0) return finger_skeleton;
        if (type == 1) return person_skeleton;
        if (type == 2) return finger_skeleton;
        return finger_skeleton;
    }
};
