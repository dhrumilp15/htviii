package com.onionhasmayo.htv_iii.camera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;

import com.onionhasmayo.htv_iii.R;

import java.util.Arrays;
import java.util.Collections;

public class CameraActivity extends AppCompatActivity {

    private TextureView textureView;
    private FloatingActionButton photoButton;

    private final int CAMERA_REQUEST_CODE = 6969;
    private final int facing = CameraCharacteristics.LENS_FACING_BACK;

    private CameraManager camMan;
    private CameraDevice camera;
    private String camId;
    private TextureView.SurfaceTextureListener surfaceTextureListener;
    private Size size;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private CameraDevice.StateCallback stateCallback= new CameraDevice.StateCallback(){

        @Override
        public void onOpened(CameraDevice camera) {
            CameraActivity.this.camera = camera;
            createPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            CameraActivity.this.camera =null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            CameraActivity.this.camera =null;
        }
    };
    private CameraCaptureSession captureSession;
    private CaptureRequest.Builder captureRequestBuilder;
    private CaptureRequest captureRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        getSupportActionBar().hide();

        textureView = findViewById(R.id.camera_view);
        photoButton = findViewById(R.id.take_photo_button);

        photoButton.setOnClickListener(view -> takePicture());

        ActivityCompat.requestPermissions(CameraActivity.this,new String[]{Manifest.permission.CAMERA} ,CAMERA_REQUEST_CODE);

        camMan = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        surfaceTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                setupCamera();
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        };
    }

    @Override
    protected void onResume(){
        super.onResume();
        openBackgroundThread();

        if(textureView.isAvailable()){
            setupCamera();
            openCamera();
        }else{
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    @Override
    protected void onStop(){
        super.onStop();
        closeCamera();
        closeBackgroundThread();
    }

    private void closeCamera(){
        if(captureSession!=null){
            captureSession.close();
            captureSession=null;
        }
        if(camera !=null){
            camera.close();
            camera=null;
        }
    }

    private void closeBackgroundThread(){
        if(backgroundThread!=null){
            backgroundThread.quitSafely();
            backgroundThread=null;
            backgroundHandler=null;
        }
    }

    private void setupCamera(){
        try {
            for(String camId : camMan.getCameraIdList()){
                CameraCharacteristics characteristics = camMan.getCameraCharacteristics(camId);
                if(characteristics.get(CameraCharacteristics.LENS_FACING)==facing){
                    StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                    size = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),wm.getDefaultDisplay().getWidth(),wm.getDefaultDisplay().getHeight());

                    this.camId=camId;
                    return;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera(){
        if(ActivityCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED){
            try {
                camMan.openCamera(camId,stateCallback,backgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void openBackgroundThread(){
        backgroundThread = new HandlerThread("CAMERA_BACKGROUND_THREAD");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void createPreview(){
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(size.getWidth(),size.getHeight());
        Surface surface = new Surface(surfaceTexture);
        try {
            captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);

            camera.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(CameraCaptureSession session) {
                    if (camera == null) return;
                    captureRequest = captureRequestBuilder.build();
                    captureSession = session;
                    try {
                        captureSession.setRepeatingRequest(captureRequest, null, backgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            }, backgroundHandler);
        }
        catch(CameraAccessException e){
            e.printStackTrace();
        }
    }

    private void takePicture() {
        //TODO: implement
    }

    private Size chooseOptimalSize(Size[] outputSizes, int width, int height) {
        double preferredRatio = height / (double) width;
        Size currentOptimalSize = outputSizes[0];
        double currentOptimalRatio = currentOptimalSize.getWidth() / (double) currentOptimalSize.getHeight();
        for (Size currentSize : outputSizes) {
            double currentRatio = currentSize.getWidth() / (double) currentSize.getHeight();
            if (Math.abs(preferredRatio - currentRatio) <
                    Math.abs(preferredRatio - currentOptimalRatio)) {
                currentOptimalSize = currentSize;
                currentOptimalRatio = currentRatio;
            }
        }
        return currentOptimalSize;
    }

}
