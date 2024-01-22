package com.example.pfldv3;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class
MainUI extends AppCompatActivity {
    private final static int CAMERA_REQUEST_CODE = 0x111;

    private Button droneBtn;
    private Button headBtn;
    private Button inferencenewBtn;

    private static final String TAG = "WelcomeActivity";
    public static final String FACE_LANDMARK_MODEL = "face_ldmks/rfb_landm_face_320_320_sim.opt";
    public static final String HEAD_LANDMARK_MODEL = "headpose/headpose_ld_ir_mobilenet_v2_112_112_normalize_sim.opt";
    public static String[] TNN_MODEL_FILES = {
            FACE_LANDMARK_MODEL,
            HEAD_LANDMARK_MODEL,
    };
    public void copyFilesFromAssets(Context context, String oldPath, String newPath) {
        try {
            String[] fileNames = context.getAssets().list(oldPath);
            if (fileNames.length > 0) {
                // directory
                File file = new File(newPath);
                if (!file.mkdir()) {
                    Log.d("mkdir", "can't make folder");
                }

                for (String fileName : fileNames) {
                    copyFilesFromAssets(context, oldPath + "/" + fileName,
                            newPath + "/" + fileName);
                }
            } else {
                // file
                InputStream is = context.getAssets().open(oldPath);
                FileOutputStream fos = new FileOutputStream(new File(newPath));
                byte[] buffer = new byte[1024];
                int byteCount;
                while ((byteCount = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, byteCount);
                }
                fos.flush();
                is.close();
                fos.close();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    void InitModelFiles() {
        String assetPath = "FaceTracking";
        String sdcardPath = Environment.getExternalStorageDirectory()
                + File.separator + assetPath;
        copyFilesFromAssets(this, assetPath, sdcardPath);
    }

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.MANAGE_EXTERNAL_STORAGE"};

    public static void verifyStoragePermissions(Activity activity) {
        try {
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_ui);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.CAMERA)) {
                    Toast.makeText(this, "Please grant camera permission first", Toast.LENGTH_SHORT).show();
                } else {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.CAMERA},
                            CAMERA_REQUEST_CODE);
                }
            }
        }
        verifyStoragePermissions(this);
        InitModelFiles();
        droneBtn = (Button) findViewById(R.id.Drone);
        OnClick onClick = new OnClick();
        droneBtn.setOnClickListener(onClick);
        headBtn.setOnClickListener(onClick);
        inferencenewBtn.setOnClickListener(onClick);
        copyModelFromAssetsToData();
    }

    class OnClick implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Intent intent = null;
            switch (v.getId()) {

                case R.id.Drone:
                    intent = new Intent(MainUI.this, ConnectActivity.class);
                    break;
                default:
                    break;
            }
            startActivity(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_REQUEST_CODE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                }
                return;
            }
        }
    }
    protected void copyModelFromAssetsToData() {
        Toast.makeText(this, "Copy model to data...", Toast.LENGTH_SHORT).show();
        try {
            for (String tnn_model : TNN_MODEL_FILES) {
                String tnnproto = tnn_model + ".tnnproto";
                String tnnmodel = tnn_model + ".tnnmodel";
                Log.w(TAG, "copy model:" + tnn_model);
                copyAssetFileToFiles(this, tnnproto);
                copyAssetFileToFiles(this, tnnmodel);
            }
            Toast.makeText(this, "Copy model Success", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Copy model Error", Toast.LENGTH_SHORT).show();
        }
    }
    public void copyAssetFileToFiles(Context context, String filename) throws IOException {
        InputStream is = context.getAssets().open(filename);
        byte[] buffer = new byte[is.available()];
        is.read(buffer);
        is.close();

        File file = new File(context.getFilesDir() + File.separator + filename);
        File parent = new File(file.getParent());
        if (!parent.exists()) {
            parent.mkdirs();
        }
        file.createNewFile();
        FileOutputStream os = new FileOutputStream(file);
        os.write(buffer);
        os.close();
    }
}