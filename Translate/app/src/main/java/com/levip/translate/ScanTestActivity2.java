package com.levip.translate;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;
import java.lang.reflect.Field;

public class ScanTestActivity2 extends AppCompatActivity {
    SurfaceView cameraPreview;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_test2);

        cameraPreview = (SurfaceView) findViewById(R.id.camera_preview);
        createCameraSource();
    }

    private void createCameraSource() {
        TextRecognizer textRecognizer = new TextRecognizer.Builder(this).build();
        final CameraSource cameraSource = new CameraSource.Builder(this, textRecognizer)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(1280, 1024).build();

        cameraPreview.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                try {
                    if (ActivityCompat.checkSelfPermission(ScanTestActivity2.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//                            ActivityCompat.requestPermissions(ScanTestActivity2.this,
//                                    new String[]{Manifest.permission.CAMERA},
//                                    1001);
                        return;
                    }
                    cameraSource.start(cameraPreview.getHolder());
                    cameraFocus(cameraSource, Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                cameraSource.stop();
            }
        });

        textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<TextBlock> detections) {
                final SparseArray<TextBlock> text = detections.getDetectedItems();
                if(text.size() != 0){
                    StringBuilder stringBuilder = new StringBuilder();
                    for(int i =0; i < text.size(); i++){
                        TextBlock item = text.valueAt(i);
                        stringBuilder.append(item.getValue());
                        stringBuilder.append("\n");
                    }
                    Intent intent = new Intent();
                    intent.putExtra("text", stringBuilder.toString());
                    setResult(CommonStatusCodes.SUCCESS, intent);
                    finish();
                }
            }
        });
    }

    /**********************************************************************************************
     * This function sets up the auto focus for the preview. it is called, and has to be called, after
     * the object is intiated and the preview/camera has been started.
     **********************************************************************************************/
    private static boolean cameraFocus(@NonNull CameraSource cameraSource, @NonNull String focusMode) {
        Field[] declaredFields = CameraSource.class.getDeclaredFields();

        for (Field field : declaredFields) {
            if (field.getType() == Camera.class) {
                field.setAccessible(true);
                try {
                    Camera camera = (Camera) field.get(cameraSource);
                    if (camera != null) {
                        Camera.Parameters params = camera.getParameters();
                        params.setFocusMode(focusMode);
                        camera.setParameters(params);
                        return true;
                    }

                    return false;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

                break;
            }
        }

        return false;
    }

}
