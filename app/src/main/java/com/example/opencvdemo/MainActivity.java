package com.example.opencvdemo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;


public class MainActivity extends AppCompatActivity {
   private String TAG = "OpenCV";
 //   private int click=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //iniLoadOpenCV();
        OpenCVLoader.initDebug();

    }

    public void ClickBtn0(View v) {

//        ImageView iv = (ImageView)this.findViewById(R.id.sample_img);
//        Bitmap bitmap = ((BitmapDrawable) ContextCompat.getDrawable(this, R.drawable.lena)).getBitmap();
//        if (bitmap == null)
//            Toast.makeText(this.getApplicationContext(), "bmp==null", Toast.LENGTH_LONG).show();
//        else Toast.makeText(this.getApplicationContext(), "bmp loaded", Toast.LENGTH_LONG).show();
//
//        click++;
//        if(click%2!=0) iv.setImageBitmap(bitmap);
//        else {
//            Mat src = new Mat();
//            Mat dst = new Mat();
//            Utils.bitmapToMat(bitmap, src);
//            Imgproc.cvtColor(src, dst, Imgproc.COLOR_BGRA2GRAY);
//            Utils.matToBitmap(dst, bitmap);
//
//            iv.setImageBitmap(bitmap);
//            src.release();
//            dst.release();
//        }
    }


    public void Camera(View v) {
        Intent intent =new Intent(MainActivity.this,Camera.class);
        startActivity(intent);

    }

//    private void iniLoadOpenCV() {
//        boolean success = OpenCVLoader.initDebug();
//        if (success) {
//            Log.i(MAIN_TAG, "OpenCV Libraries loaded...");
//            Toast.makeText(this.getApplicationContext(), "OpenCV Libraries loaded...", Toast.LENGTH_LONG).show();
//        } else {
//            Toast.makeText(this.getApplicationContext(), "WARNING: Could not load OpenCV Libraries!", Toast.LENGTH_LONG).show();
//        }
//    }



    //     通过OpenCV管理Android服务，异步初始化OpenCV
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:

                    Log.i(TAG, "OpenCV loaded successfully");
                    // Load native library after(!) OpenCV initialization
                   // System.loadLibrary("detection_based_tracker");
            }
        }

    };
    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "OpenCV init error");
        } else {
            Log.d(TAG, " OpenCV used");
            Toast.makeText(this.getApplicationContext(), "OpenCV Libraries loaded...", Toast.LENGTH_LONG).show();

            //initializeOpenCVDependencies();

          //  mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

}
