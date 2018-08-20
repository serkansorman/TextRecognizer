package com.example.serkan.textrecognizer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;
import java.util.Locale;

/**
 *  @author Serkan Sorman
 */

public class MainActivity extends AppCompatActivity {

    private SurfaceView mCameraView;
    private TextView mTextView;
    private CameraSource mCameraSource;
    private TextToSpeech textToSpeech;
    private TextRecognizer textRecognizer;
    private CameraPermission cameraPermission;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraPermission = new CameraPermission(this);
        mCameraView = findViewById(R.id.cameraView);
        mTextView = findViewById(R.id.text_view);

        // Init TextToSpeech and set language
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.US);
                }
            }
        });


        startCamera();
    }


    /**
     * Starts camera source after camera permission is granted
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == cameraPermission.getCameraPermissionID()) {

            if (cameraPermission.checkHasCameraPermission()) {

                Log.i("onRequestResult", "Permission has been granted");
                try {
                    mCameraSource.start(mCameraView.getHolder());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }


    /**
     * Gets TextBlock from TextRecognizer, set Text to TextView
     * and Speaks it if listen button is clicked
     */
    private void setDataToTextView(){

        textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
            @Override
            public void receiveDetections(Detector.Detections<TextBlock> detections) {
                final SparseArray<TextBlock> items = detections.getDetectedItems();
                if (items.size() != 0 ){
                    mTextView.post(new Runnable() {
                        @Override
                        public void run() {

                            //Gets strings from TextBlock and adds to StringBuilder
                            final StringBuilder stringBuilder = new StringBuilder();
                            for(int i=0; i<items.size(); i++)
                                stringBuilder.append(items.valueAt(i).getValue());

                            //Set Text to screen and speaks it if button clicked
                            mTextView.setText(stringBuilder.toString());
                            findViewById(R.id.voice).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Log.i("OnClickListener","Text is reading");
                                    textToSpeech.speak(stringBuilder.toString(), TextToSpeech.QUEUE_FLUSH, null);
                                }
                            });
                        }
                    });
                }
            }
            @Override
            public void release() {
            }
        });
    }

    /**
     * Init camera source with needed properties,
     * then set camera view to surface view.
     */
    private void startCamera() {

        textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

        if (textRecognizer.isOperational()) {

            mCameraSource = new CameraSource.Builder(getApplicationContext(),textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(1280, 1024)
                    .setAutoFocusEnabled(true)
                    .setRequestedFps(2.0f)
                    .build();

            //If permission is granted cameraSource started and passed it to surfaceView
            mCameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    if(cameraPermission.checkHasCameraPermission()){

                        try {
                            mCameraSource.start(mCameraView.getHolder());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                    else {

                        Log.i("surfaceCreated","Permission request sent");
                        cameraPermission.requestCameraPermission();
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    mCameraSource.stop();
                }
            });

            setDataToTextView();

        }
    }

    @Override
    public void onPause(){
        if(textToSpeech !=null){
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onPause();
    }
}