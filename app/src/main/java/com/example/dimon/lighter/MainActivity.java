package com.example.dimon.lighter;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.List;

/**
 * Main activity, where light of the camera enables and disables
 *
 * @author dimon
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String DEBUG_TAG = "LighterDebugTag";


    ConstraintLayout    parentLayout;
    Button              onOffButton;
    boolean             mIsLightOn = false;
    boolean             mIsFlashlightAvailable;
    Camera              mCamera;
    Camera.Parameters   mCameraParameters;


    /**
     * light is always off
     * when activity are created in a first time
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mIsLightOn = false;

        parentLayout = findViewById(R.id.parentLayout);
        onOffButton = findViewById(R.id.lighterButton);
        onOffButton.setOnClickListener(this);
    }

    /**
     * the method checks if the camera and flash feature is available
     * and if it is then begins to use it
     * when activity are resumed
     */
    @Override
    protected void onResume() {
        super.onResume();

        // every time when the activity resumes it checks if flashlight is available
        // this is how we prevent such situations when user changes app's permissions
        // when activity aren't destroyed
        onOffButton.setEnabled(false);
        mIsFlashlightAvailable = getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        updateFormContent();

        if (mIsFlashlightAvailable) {
            try{
                releaseCamera();

                // Exception can be thrown there if camera is unavailable
                mCamera = Camera.open();

                onOffButton.setEnabled(true);
            }
            catch (Exception exc){
                // IDK how to catch exception if there's no permission for camera
                // so I'll catch all of the exceptions
                // TODO : improve this part of code in way of catching described type of exception

                if (BuildConfig.DEBUG) {
                    Log.d(DEBUG_TAG, exc.getMessage() + "\n" + exc.getStackTrace());
                }

                mIsFlashlightAvailable = false;
            }
            updateFormContent();
        }
    }

    /**
     * releases camera and disables the flashlight when activity pauses
     */
    @Override
    protected void onPause() {
        super.onPause();

        if(mIsLightOn) mIsLightOn = false;
        releaseCamera();
    }

    /**
     * called when user clicks on "OnOffButton"
     * if flashlight feature is available switches it
     * according to it's current state
     */
    @Override
    public void onClick(View v) {
        if (mIsFlashlightAvailable) {
            mIsLightOn = !mIsLightOn;
            setFlashlightState(mIsLightOn);
            updateFormContent();
        }
    }

    /**
     * enables/disables flashlight state
     *
     * Warning: UI thread doesn't wait for completion of this thread
     * so in this activity UI changes even if the thread isn't finished to work
     * TODO : fix described above
     *
     * @param isLightSetsOn says to turn the light on or off
     */
    protected void setFlashlightState(final boolean isLightSetsOn)
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(mCamera != null) {
                    mCameraParameters = mCamera.getParameters();

                    if (mCameraParameters != null) {
                        List supportedModes = mCameraParameters.getSupportedFlashModes();

                        if (isLightSetsOn) {
                            if (supportedModes.contains(Camera.Parameters.FLASH_MODE_TORCH))
                                mCameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                            else if (supportedModes.contains(Camera.Parameters.FLASH_MODE_ON))
                                mCameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                            else
                                mCamera = null;
                        } else {
                            mCameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        }

                        if (mCamera != null) {
                            mCamera.setParameters(mCameraParameters);
                            if (isLightSetsOn)
                                mCamera.startPreview();
                            else
                                mCamera.stopPreview();

                        }
                    }
                }
            }
        }).start();
    }

    /**
     * Changes layout's style.
     * Uses variables mIsFlashlightAvailable and mIsLightOn.
     * If the flashlight is not available, uses style for unavailable button.
     * If light is on, sets light style of the layout
     * Otherwise uses dark style.
     * Also it changes text of the button
     */
    protected void updateFormContent() {
        int backgroundDrawableId, buttonDrawableId;
        int textId;

        if (mIsFlashlightAvailable) {
            if (mIsLightOn) {
                backgroundDrawableId = R.drawable.main_background_light;
                buttonDrawableId = R.drawable.btn_light_on_normal;
                textId = R.string.button_light_off;
            } else {
                backgroundDrawableId = R.drawable.main_background_dark;
                buttonDrawableId = R.drawable.btn_light_off_normal;
                textId = R.string.button_light_on;
            }
        } else {
            // if camera feature is unavailable
            backgroundDrawableId = R.drawable.main_background_dark;
            buttonDrawableId = R.drawable.btn_light_unavailable_normal;
            textId = R.string.button_unavailable; 
        }

        Drawable buttonDrawable = getResources().getDrawable(buttonDrawableId);
        Drawable backgroundDrawable = getResources().getDrawable(backgroundDrawableId);

        int sdk = Build.VERSION.SDK_INT;
        if (sdk < Build.VERSION_CODES.JELLY_BEAN) {
            onOffButton.setBackgroundDrawable(buttonDrawable);
            parentLayout.setBackgroundDrawable(backgroundDrawable);
        } else {
            onOffButton.setBackground(buttonDrawable);
            parentLayout.setBackground(backgroundDrawable);
        }

        onOffButton.setText(textId);
    }

    /**
     * releases camera so other apps can use it
     */
    protected void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

}


