package com.iceka.mywhatsappcamera;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraX;
import androidx.camera.core.FlashMode;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private TextureView mTextureView;
    private ImageView mImgCapture;
    private ImageView mImgSwitchCamera;
    private ImageView mImgFlash;

    private FlashMode flashType = FlashMode.AUTO;

    private CameraX.LensFacing mLensFacing = CameraX.LensFacing.BACK;
    private ImageCapture mImageCapture = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextureView = findViewById(R.id.textureview);
        mImgCapture = findViewById(R.id.img_capture);
        mImgSwitchCamera = findViewById(R.id.img_switch_camera);
        mImgFlash = findViewById(R.id.img_flash);

        if (allPermissionGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void startCamera() {
        bindCamera();

        mImgCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                File file = new File(Environment.getExternalStorageDirectory() + "/" + System.currentTimeMillis() + ".jpg");
                mImageCapture.takePicture(file, new ImageCapture.OnImageSavedListener() {
                    @Override
                    public void onImageSaved(@NonNull File file) {
                        Toast.makeText(MainActivity.this, "Picture saved at : " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(@NonNull ImageCapture.UseCaseError useCaseError, @NonNull String message, @Nullable Throwable cause) {
                        Toast.makeText(MainActivity.this, "Picture capture failed : " + message, Toast.LENGTH_SHORT).show();
                        if (cause != null) {
                            cause.printStackTrace();
                        }
                    }
                });
            }
        });

        mImgSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View view) {
                if (mLensFacing == CameraX.LensFacing.FRONT) {
                    mLensFacing = CameraX.LensFacing.BACK;
                } else {
                    mLensFacing = CameraX.LensFacing.FRONT;
                }
                try {
                    CameraX.getCameraWithLensFacing(mLensFacing);
                    CameraX.unbindAll();
                    bindCamera();
                } catch (CameraInfoUnavailableException e) {
                    e.printStackTrace();
                }
            }
        });

        mImgFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flashToggle();
            }
        });
    }

    private void bindCamera() {
        CameraX.unbindAll();

        Rational aspectRatio = new Rational(mTextureView.getWidth(), mTextureView.getHeight());
        Size screenSize = new Size(mTextureView.getWidth(), mTextureView.getHeight());

        PreviewConfig previewConfig = new PreviewConfig.Builder()
                .setTargetAspectRatio(aspectRatio)
                .setTargetResolution(screenSize)
                .setLensFacing(mLensFacing)
                .build();

        Preview preview = new Preview(previewConfig);
        preview.setOnPreviewOutputUpdateListener(new Preview.OnPreviewOutputUpdateListener() {
            @Override
            public void onUpdated(Preview.PreviewOutput output) {
                ViewGroup parent = (ViewGroup)mTextureView.getParent();
                parent.removeView(mTextureView);
                parent.addView(mTextureView, 0);
                mTextureView.setSurfaceTexture(output.getSurfaceTexture());
                updateTransform();
            }
        });

        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder()
                .setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                .setLensFacing(mLensFacing)
                .setFlashMode(flashType)
                .build();

        mImageCapture = new ImageCapture(imageCaptureConfig);



        CameraX.bindToLifecycle((LifecycleOwner)this, preview, mImageCapture);
    }

    private void flashToggle() {

        if (flashType == FlashMode.AUTO) {
            flashType = FlashMode.ON;
        } else if (flashType == FlashMode.ON) {
            flashType = FlashMode.OFF;
        } else if (flashType == FlashMode.OFF) {
            flashType = FlashMode.AUTO;
        }
        flashIconSettings();
    }

    private void flashIconSettings() {
        if (flashType == FlashMode.AUTO) {
            mImgFlash.setImageResource(R.drawable.ic_flash_auto_white_24dp);
        } else if (flashType == FlashMode.ON) {
            mImgFlash.setImageResource(R.drawable.ic_flash_on_black_24dp);
        } else {
            mImgFlash.setImageResource(R.drawable.ic_flash_off_black_24dp);
        }
        bindCamera();
    }

    private void refreshCamera() {
        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder()
                .setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                .setLensFacing(mLensFacing)
                .setFlashMode(flashType)
                .build();

        mImageCapture = new ImageCapture(imageCaptureConfig);
        CameraX.bindToLifecycle((LifecycleOwner)this,  mImageCapture);
    }

    private void updateTransform() {
        Matrix matrix = new Matrix();
        float w = mTextureView.getMeasuredWidth();
        float h = mTextureView.getMeasuredHeight();

        float cX = w / 2f;
        float cY = h / 2f;

        int rotationDgr;
        int rotation = (int)mTextureView.getRotation();

        switch (rotation) {
            case Surface.ROTATION_0:
                rotationDgr = 0;
                break;
            case Surface.ROTATION_90:
                rotationDgr = 90;
                break;
            case Surface.ROTATION_180:
                rotationDgr = 180;
                break;
            case Surface.ROTATION_270:
                rotationDgr = 270;
                break;
            default:
                return;
        }
        matrix.postRotate((float)rotationDgr, cX, cY);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by user.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean allPermissionGranted() {
        for (String permissions : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permissions) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
