package com.example.dimon.lighter;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.Constraints;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.security.Policy;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    String debugTag = "LighterDebugTag";

    android.hardware.Camera camera;
    android.hardware.Camera.Parameters parameters;

    ConstraintLayout constraintLayout;

    Button switchButton;
    boolean isOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();

        constraintLayout = findViewById(R.id.parentLayout);
        switchButton = findViewById(R.id.lighterButton);

        boolean isFlashlightAvailable = getApplicationContext().getPackageManager().
                hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        if (isFlashlightAvailable) {
            try{
                releaseCamera();
                camera = Camera.open();
            }
            catch (Exception exc){
                Log.d(debugTag, "At camera open action: " +
                        exc.getMessage() + "\nStacktrace: " + exc.getStackTrace());
                Toast.makeText(getApplicationContext(),"At camera open action: " +
                        exc.getMessage(), Toast.LENGTH_LONG).show();
                isFlashlightAvailable = false;
                notifyCameraUnavailable();
            }
            if(isFlashlightAvailable) {
                switchButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        isOn = !isOn;
                        setFlashlightState(isOn);
                        setFormContent(isOn);
                    }
                });
            }
        }
        else{
            notifyCameraUnavailable();
        }
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    public void setFlashlightState(final boolean isEnable)
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(camera != null) {
                    parameters = camera.getParameters();

                    if (parameters != null) {
                        List supportedModes = parameters.getSupportedFlashModes();

                        if (isEnable) {
                            if (supportedModes.contains(Camera.Parameters.FLASH_MODE_TORCH))
                                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                            else if (supportedModes.contains(Camera.Parameters.FLASH_MODE_ON))
                                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                            else
                                camera = null;
                        } else {
                            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        }

                        if (camera != null) {
                            camera.setParameters(parameters);
                            if (isEnable)
                                camera.startPreview();
                            else
                                camera.stopPreview();
                        }
                    }
                }
            }
        }).start();
    }


    public void notifyCameraUnavailable()
    {
        switchButton.setBackground(getResources().getDrawable(
                R.drawable.round_button_red));
        switchButton.setText(R.string.btnUnavailable);
    }

    public void setFormContent(boolean isEnable){
        Drawable backgroundDrawable, buttonDrawable;
        int textId;
        if(isEnable){
            backgroundDrawable = getResources().getDrawable(R.drawable.lighter_background_light);
            buttonDrawable = getResources().getDrawable(R.drawable.round_button_light);
            textId = R.string.btnLightOff;
        }
        else{
            backgroundDrawable = getResources().getDrawable(R.drawable.lighter_background);
            buttonDrawable = getResources().getDrawable(R.drawable.round_button);
            textId = R.string.btnLightOn;
        }

        int sdk = android.os.Build.VERSION.SDK_INT;
        if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            switchButton.setBackgroundDrawable(buttonDrawable);
            constraintLayout.setBackgroundDrawable(backgroundDrawable);
        } else {
            switchButton.setBackground(buttonDrawable);
            constraintLayout.setBackground(backgroundDrawable);
        }

        switchButton.setText(textId);
    }

    @Override
    protected void onStop() {
        super.onStop();
        releaseCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(isOn)
        {
            isOn = false;
            setFormContent(isOn);
        }
        releaseCamera();
    }
}
