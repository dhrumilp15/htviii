package com.onionhasmayo.htv_iii;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceView;
import android.widget.Button;

public class CameraActivity extends AppCompatActivity {

    private SurfaceView cameraView;
    private Button photoButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        getSupportActionBar().hide();

        cameraView = findViewById(R.id.camera_view);
        photoButton = findViewById(R.id.take_photo_button);

        photoButton.setOnClickListener(view->takePicture());
    }

    private void takePicture(){

    }
}
