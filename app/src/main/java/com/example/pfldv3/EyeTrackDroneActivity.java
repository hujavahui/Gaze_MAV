package com.example.pfldv3;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.camera.core.UseCase;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.app.Service;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.pfldv3.internal.utils.ModuleVerificationUtil;

import java.util.Timer;
import java.util.TimerTask;

import com.example.pfldv3.internal.utils.OnScreenJoystick;
import com.example.pfldv3.internal.utils.ToastUtils;
import com.example.pfldv3.internal.view.BaseCameraView;

import dji.common.flightcontroller.Attitude;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.remotecontroller.HardwareState;
import dji.sdk.flightcontroller.FlightController;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.sdk.remotecontroller.RemoteController;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ops.TransformToGrayscaleOp;

import wseemann.media.FFmpegMediaMetadataRetriever;

import com.cv.tnn.model.KeyPoint;
import com.example.pfldv3.R;

import com.cv.tnn.model.FrameInfo;
import com.cv.tnn.model.Detector;
import com.cv.tnn.model.DrawImage;
import com.cv.tnn.model.Skeleton;
import com.jaygoo.widget.OnRangeChangedListener;
import com.jaygoo.widget.RangeSeekBar;

public class EyeTrackDroneActivity extends AppCompatActivity implements View.OnClickListener {
    public static boolean USE_GPU = false; // OPENGL
    public static int MODEL_ID = 0;    //
    public static int NUM_THREAD = 3;  //
    public static CameraX.LensFacing CAMERA_ID = CameraX.LensFacing.FRONT;
    private static final int REQUEST_CAMERA = 1;
    private static final String TAG = "dm-MainActivity";
    private static String[] PERMISSIONS_CAMERA = {
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private ImageView resultImageView;
    //    private Button drone_forward;
    private float score_threshold = 0.5f;
    private float iou_threshold = 0.5f;
    protected long videoCurFrameLoc = 0;
    private AtomicBoolean detectCamera = new AtomicBoolean(false);
    private AtomicBoolean detectPhoto = new AtomicBoolean(false);
    private AtomicBoolean detectVideo = new AtomicBoolean(false);
    private int width;
    private int height;
    protected Bitmap mutableBitmap;
    ExecutorService detectService = Executors.newSingleThreadExecutor();
    private Bitmap bitmap_lefteye, bitmap_righteye;
    private double eyesizeRatio = 0.3;
    private int eyesize, eyesize_x_l, eyesize_x_r, eyesize_y;
    private boolean startinference = false;
    public File filestate = new File("/storage/emulated/0/EyeTrack/State/" + "state.txt");
    protected SurfaceView SurfaceDrawPoint = null;
    protected SurfaceHolder SurfaceHolderDrawPoint = null;
    private Paint mPain_Point;
    public KeyPoint kp1, kp2;
    public float yaw, pitch, roll;//

    MappedByteBuffer tfliteModel;
    Interpreter tflite;
    float[][] out = new float[1][2];
    Map<Integer, Object> outputs = new HashMap<Integer, Object>();
    TensorImage tImage1 = new TensorImage(DataType.FLOAT32);
    TensorImage tImage2 = new TensorImage(DataType.FLOAT32);
    ImageProcessor imageProcessor;
    Object[] inputs = new Object[4];
    float[][] head_pos = new float[1][3];
    float[][] head_pose = new float[1][3];
    int gaze_point_x, gaze_point_y;
    int gaze_point_x_draw, gaze_point_y_draw;

    int newWidth = 64;
    int newHeight = 64;
    float scaleWidth = 0;
    float scaleHeight = 0;
    int width_bitmap_lefteye;
    int height_bitmap_lefteye;

    boolean eyeTrackIsEnabled;
    boolean camIsStart;
    boolean remotecontrolclicked;

    private ImageButton btnTakeOff;
    private ImageButton btnAutoLand;
    private TextView satelliteState;
    private TextView batteryState;
    private TextView moveState;
    private ImageView batteryImage;
    private BaseCameraView cameraView;
    private Button StartEyeTrack;

    private Timer sendEyeTrackDataTimer;
    private EyeTrackDroneActivity.SendEyeTrackDataTask sendEyeTrackDataTask;

    private float pitch_eye;
    private float roll_eye;
    private float yaw_eye;
    private float throttle;
    public float face_pos1x, face_pos1y, face_pos2x, face_pos2y;

    private FlightController flightController;
    private RemoteController remoteController;

    private int eyepoint2mid_distance = 0;
    private float YawControlSpeed = 0.012f;
    private float PitchControlSpeed = 0.016f;
    private float RollControlSpeed = 0;
    private float ThrottleControlSpeed = -0.000375f;

    private RangeSeekBar pitch_bar;
    private float pitch_bar_value;
    private float velocity_x, velocity_y, velocity_z;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eye_track_drone);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_CAMERA, REQUEST_CAMERA);
            finish();
        }
        initModel();
        initUI();
        setUpListeners();
        if (!detectCamera.get()) {
            startCamera();
            camIsStart = true;
        }
        if (startinference == false && camIsStart) {
            startinference = true;
        } else {
            startinference = false;
        }
    }

    private void initUI() {
        btnTakeOff = (ImageButton) findViewById(R.id.btn_take_off);
        btnAutoLand = (ImageButton) findViewById(R.id.btn_auto_land);
        StartEyeTrack = (Button) findViewById(R.id.EyeTrack);
//        drone_forward = (Button) findViewById(R.id.drone_forward);
        cameraView = (BaseCameraView) findViewById(R.id.camera_view);
        moveState = (TextView) findViewById(R.id.movestateView);
        satelliteState = (TextView) findViewById(R.id.satellite);
        batteryState = (TextView) findViewById(R.id.battery);
        batteryImage = (ImageView) findViewById(R.id.battery_image);
        resultImageView = findViewById(R.id.imageView);
        SurfaceDrawPoint = findViewById(R.id.surfacedrawpoint);
        SurfaceDrawPoint.setZOrderOnTop(true);
        SurfaceHolderDrawPoint = SurfaceDrawPoint.getHolder();
        SurfaceHolderDrawPoint.setFormat(PixelFormat.TRANSPARENT);
        pitch_bar = findViewById(R.id.sb_vertical_6);
        pitch_bar.setProgress(50f);

        mPain_Point = new Paint();
        mPain_Point.setColor(Color.rgb(57, 138, 243));
        mPain_Point.setStrokeWidth(6);
        mPain_Point.setStyle(Paint.Style.FILL);

        btnTakeOff.setOnClickListener(this);
        btnAutoLand.setOnClickListener(this);
        cameraView.setOnClickListener(this);

        flightController = ModuleVerificationUtil.getFlightController();
        if (flightController != null) {
            flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
            flightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
            flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
            flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
        } else {
            ToastUtils.setResultToToast("Can not find flightController!");
        }
        // 初始禁用眼动
        eyeTrackIsEnabled = false;
        camIsStart = false;
        flightController.setVirtualStickModeEnabled(false, null);

        StartEyeTrack.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (!eyeTrackIsEnabled && camIsStart) {
                            eyeTrackIsEnabled = true;
                            flightController.setVirtualStickModeEnabled(true, djiError -> flightController.setVirtualStickAdvancedModeEnabled(true));
                            StartEyeTrack.setBackgroundColor(Color.parseColor("#ff00ff"));
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        eyeTrackIsEnabled = false;
                        yaw_eye = 0;
                        throttle = 0;
                        pitch_eye = 0;
                        StartEyeTrack.setBackgroundColor(Color.parseColor("#4d4dff"));
                        flightController.setVirtualStickModeEnabled(false, null);
                        break;

                    default:
                        break;
                }
                return false;
            }
        });

        pitch_bar.setOnRangeChangedListener(new OnRangeChangedListener() {
            @Override
            public void onRangeChanged(RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser) {
                //leftValue is left seekbar value, rightValue is right seekbar value
                if (isFromUser) {
                    pitch_bar_value = pitch_bar.getLeftSeekBar().getProgress();
                    System.out.println(pitch_bar_value);
                    if (eyeTrackIsEnabled) {
                        pitch_eye = -(pitch_bar_value - 50) * PitchControlSpeed;
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {
                //start tracking touch
                Vibrator vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);//
                vibrator.vibrate(new long[]{0, 200}, -1);
            }

            @Override
            public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {
                //stop tracking touch
                pitch_bar.setProgress(50f);
                pitch_eye = 0;
            }
        });
    }

    protected void initModel() {
        String root = this.getFilesDir() + File.separator;
        String face_model = MainUI.TNN_MODEL_FILES[0];
        String head_model = MainUI.TNN_MODEL_FILES[1];
        Detector.init(face_model, head_model, root, MODEL_ID, NUM_THREAD, USE_GPU);
        try {
            tfliteModel = FileUtil.loadMappedFile(getApplicationContext().getApplicationContext(), "model_big.tflite");
            tflite = new Interpreter(tfliteModel);
        } catch (IOException e) {
            Log.e("MODEL", "Error reading Model", e);
        }
        imageProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeOp(64, 64, ResizeOp.ResizeMethod.BILINEAR))
                        .add(new NormalizeOp(0, 255))
                        .build();
    }

    private void setUpListeners() {
        MApplication.getAircraftInstance()
                .getFlightController().setStateCallback(new FlightControllerState.Callback() {
            @Override
            public void onUpdate(@NonNull FlightControllerState flightControllerState) { //10HZ///////////
                velocity_z = flightControllerState.getVelocityZ();
                velocity_x = flightControllerState.getVelocityX();
                velocity_y = flightControllerState.getVelocityY();

                moveState.setText(velocity_z + "\n" + velocity_x + "\n" + velocity_y);

                try {
                    FileOutputStream label = new FileOutputStream(filestate, true);
                    String content = "$"
                            + velocity_x + '$' + velocity_y + '$' + velocity_z + '$'
                            + '\n';
                    label.write(content.getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                }
            }
        });

        if (null == sendEyeTrackDataTimer) {
            sendEyeTrackDataTask = new EyeTrackDroneActivity.SendEyeTrackDataTask();
            sendEyeTrackDataTimer = new Timer();
            sendEyeTrackDataTimer.schedule(sendEyeTrackDataTask, 0, 200);
        }
        MApplication.getProductInstance().getBattery().setStateCallback(djiBatteryState -> {
            int battery = djiBatteryState.getChargeRemainingInPercent();
            batteryState.setText(battery + "%");
            if (battery < 25) {
                batteryState.setTextColor(0xffd40000);
                batteryImage.setImageResource(R.drawable.battery_low);
            } else if (battery < 50) {
                batteryState.setTextColor(0xffaa4400);
                batteryImage.setImageResource(R.drawable.battery_mid);
            } else if (battery < 75) {
                batteryState.setTextColor(0xffffcc00);
                batteryImage.setImageResource(R.drawable.battery_high);
            } else {
                batteryState.setTextColor(0xff00a900);
                batteryImage.setImageResource(R.drawable.battery_good);
            }
        });
    }

    private void startCamera() {
        CameraX.unbindAll();
        // 1. preview
        PreviewConfig previewConfig = new PreviewConfig.Builder()
                .setLensFacing(CAMERA_ID)
                //.setTargetAspectRatio(Rational.NEGATIVE_INFINITY)  //
                .setTargetResolution(new Size(480, 640))  //
                .build();
        Preview preview = new Preview(previewConfig);
        preview.setOnPreviewOutputUpdateListener(new Preview.OnPreviewOutputUpdateListener() {
            @Override
            public void onUpdated(Preview.PreviewOutput output) {
            }
        });
        EyeTrackDroneActivity.DetectAnalyzer detectAnalyzer = new EyeTrackDroneActivity.DetectAnalyzer();
        CameraX.bindToLifecycle((LifecycleOwner) this, preview, gainAnalyzer(detectAnalyzer));
    }

    private UseCase gainAnalyzer(EyeTrackDroneActivity.DetectAnalyzer detectAnalyzer) {
        ImageAnalysisConfig.Builder analysisConfigBuilder = new ImageAnalysisConfig.Builder()
                .setLensFacing(CAMERA_ID);
        analysisConfigBuilder.setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE);
        analysisConfigBuilder.setTargetResolution(new Size(480, 640));  //
        ImageAnalysisConfig config = analysisConfigBuilder.build();
        ImageAnalysis analysis = new ImageAnalysis(config);
        analysis.setAnalyzer(detectAnalyzer);
        return analysis;
    }

    private class DetectAnalyzer implements ImageAnalysis.Analyzer {
        @Override
        public void analyze(ImageProxy image, final int rotationDegrees) {
            Log.w(TAG, "analyze：" + String.valueOf(rotationDegrees));
            runByCamera(image, rotationDegrees);
        }
    }

    private void runByCamera(ImageProxy image, final int rotationDegrees) {
        if (detectCamera.get() || detectPhoto.get() || detectVideo.get()) {
            return;
        }
        detectCamera.set(true);
        final Bitmap bitmapsrc = imageToBitmap(image);  //
        if (detectService == null) {
            detectCamera.set(false);
            return;
        }
        Matrix m = new Matrix();
        m.postScale(-1, 1);
        detectService.execute(new Runnable() {
            @Override
            public void run() {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotationDegrees);
                width = bitmapsrc.getWidth();
                height = bitmapsrc.getHeight();
                Bitmap bitmap = Bitmap.createBitmap(bitmapsrc, 0, 0, width, height, matrix, false);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, m, false);
                detectAndDrawAndCrop(bitmap);
                showResultOnUI();
            }
        });
    }

    private Bitmap imageToBitmap(ImageProxy image) {
        byte[] nv21 = imagetToNV21(image);
        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, out);
        byte[] imageBytes = out.toByteArray();
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    protected Bitmap detectAndDrawAndCrop(Bitmap bitmap) {
        bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        FrameInfo[] result = null;
        //maskBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        result = Detector.detect(bitmap, score_threshold, iou_threshold, bitmap);

        if (result.length == 1) {
            FrameInfo info = result[0];
            kp1 = info.getKeyPoints().get(0);
            kp2 = info.getKeyPoints().get(1);
            roll = info.getHeadPos().get(0).roll;
            yaw = info.getHeadPos().get(0).yaw;
            pitch = info.getHeadPos().get(0).pitch;

            face_pos1y = info.getRect().top;
            face_pos1x = info.getRect().left;
            face_pos2x = info.getRect().right;
            face_pos2y = info.getRect().bottom;

            eyesize_x_l = (int) (kp1.point.x - eyesize - 5);
            eyesize_x_r = (int) (kp2.point.x - eyesize + 5);
            eyesize_y = (int) (kp1.point.y - (int) (eyesize / 2));

            if (eyesize_x_l > 20 && eyesize_y > 20 && eyesize_x_r + 2 * eyesize < 640) {
                eyesize = (int) (eyesizeRatio * Math.sqrt((kp1.point.x - kp2.point.x) * (kp1.point.x - kp2.point.x) +
                        (kp1.point.y - kp2.point.y) * (kp1.point.y - kp2.point.y)));
                bitmap_lefteye = Bitmap.createBitmap(bitmap, eyesize_x_l, eyesize_y, 2 * eyesize, eyesize, null, true);
                bitmap_righteye = Bitmap.createBitmap(bitmap, eyesize_x_r, eyesize_y, 2 * eyesize, eyesize, null, true);
                if (startinference) {
                    inference();
                }
            }
        }
        if (result == null) {
            detectCamera.set(false);
            mutableBitmap = bitmap;
            startinference = false;
            return bitmap;
        }
//        mutableBitmap = DrawImage.drawResult(bitmap, result, Skeleton.get_skeleton(MODEL_ID));
        mutableBitmap = bitmap;
        return mutableBitmap;
    }

    public void inference() {
        width_bitmap_lefteye = bitmap_lefteye.getWidth();
        height_bitmap_lefteye = bitmap_lefteye.getHeight();

        scaleWidth = ((float) newWidth) / width_bitmap_lefteye;
        scaleHeight = ((float) newHeight) / height_bitmap_lefteye;

        Matrix matrix_resize = new Matrix();
        matrix_resize.postScale(scaleWidth, scaleHeight);
        Matrix matrix = new Matrix(); //
        matrix.postScale(-1, 1); //
        bitmap_righteye = Bitmap.createBitmap(bitmap_righteye, 0, 0, width_bitmap_lefteye, height_bitmap_lefteye, matrix, true);
        bitmap_righteye = Bitmap.createBitmap(bitmap_righteye, 0, 0, width_bitmap_lefteye, height_bitmap_lefteye, matrix_resize, true);
        bitmap_lefteye = Bitmap.createBitmap(bitmap_lefteye, 0, 0, width_bitmap_lefteye, height_bitmap_lefteye, matrix_resize, true);

        head_pos[0][0] = roll;
        head_pos[0][1] = yaw;
        head_pos[0][2] = pitch;

        head_pose[0][0] = (face_pos1x + face_pos2x) / 2;
        head_pose[0][1] = (face_pos1y + face_pos2y) / 2;
        head_pose[0][2] = (face_pos2x - face_pos1x);

        if (null != tflite) {
            System.out.println("inference...............");
            tImage1.load(bitmap_lefteye);
            tImage1 = imageProcessor.process(tImage1);
            tImage2.load(bitmap_righteye);
            tImage2 = imageProcessor.process(tImage2);
            inputs[0] = tImage1.getBuffer();
            inputs[1] = tImage2.getBuffer();
            inputs[2] = head_pos;
            inputs[3] = head_pose;

            outputs.put(0, out);
            tflite.runForMultipleInputsOutputs(inputs, outputs);

            gaze_point_x = (int) (out[0][0]);
            gaze_point_y = (int) (out[0][1]);

            gaze_point_y = gaze_point_y + 800;
            gaze_point_x = gaze_point_x + 1280;

            if (gaze_point_x > 2560) {
                gaze_point_x = 2560;
            } else if (gaze_point_x < 15) {
                gaze_point_x = 15;
            }
            if (gaze_point_y > 1560) {
                gaze_point_y = 1560;
            } else if (gaze_point_y < 15) {
                gaze_point_y = 15;
            }

            draw_point();

            if (eyeTrackIsEnabled) {
                Drone_Control(gaze_point_x, gaze_point_y, head_pos[0][0]);
            }
        }
    }

    public void Drone_Control(int x, int y, float head_roll) {
        if (head_roll < -20) {
            throttle = yaw_eye = 0;
            roll_eye = -0.6f;
        } else if (head_roll > 20) {
            throttle = yaw_eye = 0;
            roll_eye = 0.6f;
        } else {
            roll_eye = 0;
            EyeTrack_Control(x, y);
        }
    }

    public void EyeTrack_Control(int x, int y) {
        eyepoint2mid_distance = (int) (Math.sqrt((x - 1280) * (x - 1280) + (y - 800) * (y - 800)));
//        pitch_eye = 0.5f;
        if (eyepoint2mid_distance < 800) {
            yaw_eye = throttle = roll_eye = 0;
        } else {
            yaw_eye = ((x - 1280) * YawControlSpeed);
            throttle = ((y - 800) * ThrottleControlSpeed);

            if (yaw_eye > 15) {
                yaw_eye = 15;
            } else if (yaw_eye < -15) {
                yaw_eye = -15;
            }
            if (throttle < -0.3f) {
                throttle = -0.3f;
            } else if (throttle > 0.3f) {
                throttle = 0.3f;
            }
        }
    }

    public void draw_point() {
        Canvas canvas = SurfaceHolderDrawPoint.lockCanvas();
        try {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            canvas.drawCircle(gaze_point_x, gaze_point_y, 20, mPain_Point);

        } catch (Exception e) {
        } finally {
            if (canvas != null && SurfaceHolderDrawPoint != null) {
                SurfaceHolderDrawPoint.unlockCanvasAndPost(canvas);
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (flightController == null) {
            return;
        }
        switch (v.getId()) {
            //
            case R.id.btn_take_off:
                flightController.startTakeoff(null);
                break;
            //
            case R.id.btn_auto_land:
                flightController.startLanding(null);
                flightController.confirmLanding(null);
                break;

            default:
                break;
        }
    }

    private class SendEyeTrackDataTask extends TimerTask {
        @Override
        public void run() {
            if (ModuleVerificationUtil.isFlightControllerAvailable()) {
                MApplication.getAircraftInstance()
                        .getFlightController()
                        .sendVirtualStickFlightControlData(new FlightControlData(
                                        roll_eye,
                                        pitch_eye,
                                        yaw_eye,
                                        throttle),
                                djiError -> {
                                });
            }
        }
    }

    protected void showResultOnUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                detectCamera.set(false);
                resultImageView.setImageBitmap(mutableBitmap);
                width = mutableBitmap.getWidth();
                height = mutableBitmap.getHeight();
            }
        });
    }

    private byte[] imagetToNV21(ImageProxy image) {
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        ImageProxy.PlaneProxy y = planes[0];
        ImageProxy.PlaneProxy u = planes[1];
        ImageProxy.PlaneProxy v = planes[2];
        ByteBuffer yBuffer = y.getBuffer();
        ByteBuffer uBuffer = u.getBuffer();
        ByteBuffer vBuffer = v.getBuffer();
        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();
        byte[] nv21 = new byte[ySize + uSize + vSize];
        // U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);
        return nv21;
    }
}