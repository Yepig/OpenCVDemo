package com.example.opencvdemo;


import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.example.function.Detection;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;


public class Camera extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private String TAG = "OpenCV_Test";
    //一个关键的，用于存储有效的照相机的索引。
    private static final String STATE_CAMERA_INDEX = "cameraIndex";
    //OpenCV的相机接口
    private CameraBridgeViewBase mCVCamera;
    //缓存相机每帧输入的数据
    private Mat mRgba, mTmp, mGray;
    //按钮组件
    private Button mButton;
    //当前处理状态
    private static int Cur_State = 0;

    private Size mSize0;
    private Mat mIntermediateMat;
    private MatOfInt mChannels[];
    private MatOfInt mHistSize;
    private int mHistSizeNum = 25;
    private Mat mMat0;
    private float[] mBuff;
    private MatOfFloat mRanges;
    private Point mP1;
    private Point mP2;
    private Scalar mColorsRGB[];
    private Scalar mColorsHue[];
    private Scalar mWhilte;
    private Mat mSepiaKernel;
    private int absoluteFaceSize = 0;


    private CascadeClassifier cascadeClassifier = null; //级联分类器



    //活动摄像头的索引
    private int mCameraIndex;
    //活动相机是前置
    //如果是这样，相机视图要显示
    private boolean mIsCameraFrontFacing=false;
    //这个数表示设备上相机的数量
    private int mNumCameras;
    // The camera view.
    private CameraBridgeViewBase mCameraView;
    //无论下一相机框架是哪一个都应该保存照片
    private boolean mIsPhotoPending;
    //如果一个菜单在进行，菜单交互应该被禁止
    private boolean mIsMenuLocked;


    private int click = 0;

    private Handler handler;







    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    initWindowSettings();

//        final Window window = getWindow();
//        window.addFlags(
//                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (savedInstanceState != null) {
            mCameraIndex = savedInstanceState.getInt(
                    STATE_CAMERA_INDEX, 0);

        } else {
            mCameraIndex = 0;

        }
        if (Build.VERSION.SDK_INT <=
                Build.VERSION_CODES.LOLLIPOP_MR1) {
            android.hardware.Camera.CameraInfo
                    cameraInfo = new android.hardware.Camera.CameraInfo();
            android.hardware.Camera.getCameraInfo(mCameraIndex, cameraInfo);

            mIsCameraFrontFacing =
                    (cameraInfo.facing ==
                            android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT);
            mNumCameras = android.hardware.Camera.getNumberOfCameras();
        } else { // pre-Gingerbread
// Assume there is only 1 camera and it is rear-facing.
            mIsCameraFrontFacing = false;
            mNumCameras = 2;
        }


        initializeOpenCVDependencies();

        setContentView(R.layout.activity_camera);

        mCVCamera = findViewById(R.id.camera_view);
        mCVCamera.setCvCameraViewListener(this);
        mCVCamera.enableFpsMeter(); //显示FPS
        mCVCamera.enableView();

    }

    // 初始化窗口设置, 包括全屏、横屏、常亮
    private void initWindowSettings() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }
//     通过OpenCV管理Android服务，异步初始化OpenCV
//    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
//        @Override
//        public void onManagerConnected(int status) {
//            switch (status) {
//                case LoaderCallbackInterface.SUCCESS:
//                    Log.i(TAG, "OpenCV loaded successfully");
//                    // Load native library after(!) OpenCV initialization
//                   // System.loadLibrary("detection_based_tracker");
//            }
//        }
//
//    };
    @Override
    public void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "OpenCV init error");
        } else {
            Log.d(TAG, " OpenCV used");
            Toast.makeText(this.getApplicationContext(), "OpenCV Libraries loaded...", Toast.LENGTH_LONG).show();

            initializeOpenCVDependencies();

          // mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCVCamera != null) {
            mCVCamera.disableView();
        }
    }

    private void initializeOpenCVDependencies() {
        try {
            InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface); //OpenCV的人脸模型文件： lbpcascade_frontalface_improved
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
            // 加载cascadeClassifier
            cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Error loading cascade", e);
        }
        // 显示
        // openCvCameraView.enableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mTmp = new Mat(height, width, CvType.CV_8UC4);

        mIntermediateMat = new Mat();
        mSize0 = new Size();
        mChannels = new MatOfInt[]{new MatOfInt(0), new MatOfInt(1), new MatOfInt(2)};
        mBuff = new float[mHistSizeNum];
        mHistSize = new MatOfInt(mHistSizeNum);
        mRanges = new MatOfFloat(0f, 256f);
        mMat0 = new Mat();
        mColorsRGB = new Scalar[]{new Scalar(200, 0, 0, 255), new Scalar(0, 200, 0, 255), new Scalar(0, 0, 200, 255)};
        mColorsHue = new Scalar[]{
                new Scalar(255, 0, 0, 255), new Scalar(255, 60, 0, 255), new Scalar(255, 120, 0, 255), new Scalar(255, 180, 0, 255), new Scalar(255, 240, 0, 255),
                new Scalar(215, 213, 0, 255), new Scalar(150, 255, 0, 255), new Scalar(85, 255, 0, 255), new Scalar(20, 255, 0, 255), new Scalar(0, 255, 30, 255),
                new Scalar(0, 255, 85, 255), new Scalar(0, 255, 150, 255), new Scalar(0, 255, 215, 255), new Scalar(0, 234, 255, 255), new Scalar(0, 170, 255, 255),
                new Scalar(0, 120, 255, 255), new Scalar(0, 60, 255, 255), new Scalar(0, 0, 255, 255), new Scalar(64, 0, 255, 255), new Scalar(120, 0, 255, 255),
                new Scalar(180, 0, 255, 255), new Scalar(255, 0, 255, 255), new Scalar(255, 0, 215, 255), new Scalar(255, 0, 85, 255), new Scalar(255, 0, 0, 255)
        };
        mWhilte = Scalar.all(255);
        mP1 = new Point();
        mP2 = new Point();

        // Fill sepia kernel
        mSepiaKernel = new Mat(4, 4, CvType.CV_32F);
        mSepiaKernel.put(0, 0, /* R */0.189f, 0.769f, 0.393f, 0f);
        mSepiaKernel.put(1, 0, /* G */0.168f, 0.686f, 0.349f, 0f);
        mSepiaKernel.put(2, 0, /* B */0.131f, 0.534f, 0.272f, 0f);
        mSepiaKernel.put(3, 0, /* A */0.000f, 0.000f, 0.000f, 1f);


    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCVCamera != null)
            mCVCamera.disableView();
    }
    private int colordodge(int A, int B) {
        return  Math.min(A+(A*B)/(255-B+1),255);
    }
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray(); //单通道灰度图
        Size sizeRgba = mRgba.size();
        int rows = (int) sizeRgba.height;
        int cols = (int) sizeRgba.width;
        Mat rgbaInnerWindow;

        int left = cols / 8;
        int top = rows / 8;

        int width = cols * 3 / 4;
        int height = rows * 3 / 4;


        switch (click) {
            case 1:
                //灰化处理
                Imgproc.cvtColor(inputFrame.gray(), mRgba, Imgproc.COLOR_GRAY2RGBA, 4);
                break;
            case 2:
                //Canny边缘检测
                mRgba = inputFrame.rgba();
                Imgproc.Canny(inputFrame.gray(), mTmp, 80, 100);
                Imgproc.cvtColor(mTmp, mRgba, Imgproc.COLOR_GRAY2RGBA, 4);
                break;

            case 3:
                //PIXELIZE像素化
                rgbaInnerWindow = mRgba.submat(top, top + height, left, left + width);
                Imgproc.resize(rgbaInnerWindow, mIntermediateMat, mSize0, 0.1, 0.1, Imgproc.INTER_NEAREST);
                Imgproc.resize(mIntermediateMat, rgbaInnerWindow, rgbaInnerWindow.size(), 0., 0., Imgproc.INTER_NEAREST);
                rgbaInnerWindow.release();
                break;
            case 4:
                initializeOpenCVDependencies();
                mRgba=faceDetect();
                break;
            case 5:
                //二值化滤镜
                Imgproc.cvtColor(mRgba,mRgba,Imgproc.COLOR_RGB2GRAY);
                Core.bitwise_not(mRgba,mRgba);
                Imgproc.threshold(mRgba,mRgba,100,255,Imgproc.THRESH_BINARY_INV);
                break;
            case 6:
                Bitmap bitmap=null;
                Bitmap bit = null;
                Bitmap bit1 = null;

                //灰度化
                Imgproc.cvtColor(mRgba, mRgba, Imgproc.COLOR_RGB2GRAY);
                //颜色取反
                Core.bitwise_not(mRgba,mTmp);
                //高斯模糊
                Imgproc.GaussianBlur(mTmp,mTmp,new Size(13,13),0,0);
                Utils.matToBitmap(mRgba, bit);
                Utils.matToBitmap(mTmp, bit1);
                for(int i = 0;i<bit.getWidth();i++){
                    for( int j = 0;j<bit.getHeight();j++){
                        int A = bit.getPixel(i,j);
                        int B = bit1.getPixel(i,j);
                        int CR = colordodge(Color.red(A),Color.red(B));
                        int CG = colordodge(Color.green(A),Color.red(B));
                        int CB = colordodge(Color.blue(A),Color.blue(B));
                        bitmap.setPixel(i,j,Color.rgb(CR,CG,CB));
                    }
                }
                Utils.bitmapToMat(bitmap,mRgba);
                break;
                default:
                //显示原图
                mRgba = inputFrame.rgba();
                break;
        }
        return mRgba;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.activity_camera, menu);
        if (mNumCameras < 2) {
            menu.removeItem(R.id.menu_next_camera);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
//        if (mIsMenuLocked) {
//            return true;
//        }
        switch (item.getItemId()) {

            case R.id.menu_next_camera:
                mIsMenuLocked = true;
                //另一个相机索引，重新创建活动
                //mCameraIndex++;
                mCVCamera.disableView();
                if (mIsCameraFrontFacing) {
                    mCVCamera.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
                    mIsCameraFrontFacing = false;
                }else {
                    mCVCamera.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT); //摄像头索引        -1/0：后置双摄     1：前置
                    mIsCameraFrontFacing = true;
                }
                mCVCamera.enableView();
               // recreate();
                return true;
            case R.id.menu_take_photo:
                mIsMenuLocked = true;
// Next frame, take the photo.
//
                takePhoto(mRgba);

                mIsPhotoPending = true;
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void takePhoto(final Mat rgba) {
        // Determine the path and metadata for the photo.
        //确定路径和照片元数据
        final long currentTimeMillis = System.currentTimeMillis();
        final String appName = getString(R.string.app_name);
        final String galleryPath =
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES).toString();
        final String albumPath = galleryPath + "/" + appName;
        final String photoPath = albumPath + "/" +
                currentTimeMillis + ".jpg";
        final ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, photoPath);
        values.put(MediaStore.Images.Media.MIME_TYPE,
                LabActivity.PHOTO_MIME_TYPE);
        values.put(MediaStore.Images.Media.TITLE, appName);
        values.put(MediaStore.Images.Media.DESCRIPTION, appName);
        values.put(MediaStore.Images.Media.DATE_TAKEN, currentTimeMillis);
        // Ensure that the album directory exists.
        //确保相片目录存在
        File album = new File(albumPath);
        if (!album.isDirectory() && !album.mkdirs()) {
            Log.e(TAG, "Failed to create album directory at " +
                    albumPath);
            Toast.makeText(Camera.this, "Failed to create album directory",
                    Toast.LENGTH_SHORT).show();
            onTakePhotoFailed();
            return;
        }
        // Try to create the photo.
        //尝试创建照片
        Imgproc.cvtColor(rgba, mRgba, Imgproc.COLOR_RGBA2BGR, 3);
        if (!Imgcodecs.imwrite(photoPath, mRgba)) {
            Log.e(TAG, "Failed to save photo to " + photoPath);//图片没有保存到
            Toast.makeText(Camera.this, "Failed to save photo",
                    Toast.LENGTH_SHORT).show();
            onTakePhotoFailed();
        }
        Log.d(TAG, "Photo saved successfully to " + photoPath);//图片保存成功
        Toast.makeText(Camera.this, "Photo saved successfully",
                Toast.LENGTH_SHORT).show();

        //尝试插入图片到MediaStore
        Uri uri;
        try {
            uri = getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (final Exception e) {
            Log.e(TAG, "Failed to insert photo into MediaStore");//无法插入图像到MediaStore
            e.printStackTrace();

            //由于插入失败，删除图片
            File photo = new File(photoPath);
            if (!photo.delete()) {
                Log.e(TAG, "Failed to delete non-inserted photo");
            }
            onTakePhotoFailed();
            return;
        }

        //在LabActivity打开照片
        final Intent intent = new Intent(this, LabActivity.class);
        intent.putExtra(LabActivity.EXTRA_PHOTO_URI, uri);
        intent.putExtra(LabActivity.EXTRA_PHOTO_DATA_PATH,
                photoPath);
        startActivity(intent);
    }

    private void onTakePhotoFailed() {
        mIsMenuLocked = false;
        final String errorMessage = getString(R.string.photo_error_message);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(Camera.this, errorMessage,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void style(View v) {
        if (click == 7) {
            click = 0;
        } else
            click++;
    }

    public Mat faceDetect(){

        if (absoluteFaceSize == 0) {
            int height1 = mGray.rows();
            if (Math.round(height1 * 0.2f) > 0) {
                absoluteFaceSize = Math.round(height1 * 0.2f);
            }
        }
        //检测并显示
        MatOfRect faces = new MatOfRect();
        if (cascadeClassifier != null) {
            cascadeClassifier.detectMultiScale(mGray, faces, 1.1, 2, 2, new Size(absoluteFaceSize, absoluteFaceSize), new Size());
        }
        Rect[] facesArray = faces.toArray();
        if (facesArray.length > 0) {
            for (int i = 0; i < facesArray.length; i++) {    //用框标记
                Imgproc.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0, 255), 3);
            }
        }
        return mRgba;
    }
}
