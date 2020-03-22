package com.star.camerasample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextureView mTextureView;
    private Button captureButton;
    private static final SparseIntArray ORIENTATION = new SparseIntArray();

    static {
        ORIENTATION.append(Surface.ROTATION_0, 90);
        ORIENTATION.append(Surface.ROTATION_90, 0);
        ORIENTATION.append(Surface.ROTATION_180, 270);
        ORIENTATION.append(Surface.ROTATION_270, 180);
    }

    private String cameraId;
    private CameraDevice camDevice;
    private CameraCaptureSession camCapSession;
    private CaptureRequest capRequest;
    private CaptureRequest.Builder capRequestBuilder;
    private Size imgDimentions;
    private ImageReader imgReader;
    private File picFile;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private static final int camPermissionReqCode = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextureView = findViewById(R.id.texture_view);
        captureButton = findViewById(R.id.capture_btn);

        mTextureView.setSurfaceTextureListener(textureListener);

        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {

                    Intent intent = new Intent(MainActivity.this,QrCodeScanner.class);
                    startActivity(intent);

                    //takePicture();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private void takePicture() throws CameraAccessException {

        if (camDevice == null) {
            return;
        }

        CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        CameraCharacteristics camCharacteristics = camManager.getCameraCharacteristics(camDevice.getId());
        Size[] jpegSize = null;
        jpegSize = camCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);

        int picWidth = 640;
        int picHeight = 480;

        if (jpegSize != null && jpegSize.length > 0) {
            picWidth = jpegSize[0].getWidth();
            picHeight = jpegSize[0].getHeight();
        }

        final ImageReader imgReader = ImageReader.newInstance(picWidth, picHeight, ImageFormat.JPEG, 1);
        List<Surface> outputSurface = new ArrayList<>(2);
        outputSurface.add(imgReader.getSurface());
        outputSurface.add(new Surface(mTextureView.getSurfaceTexture()));
        final CaptureRequest.Builder capBuilder = camDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        capBuilder.addTarget(imgReader.getSurface());
        capBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        int picRotation = getWindowManager().getDefaultDisplay().getRotation();
        capBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATION.get(picRotation));
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        picFile = new File(Environment.getExternalStorageDirectory() + "/" + "IMG" + timeStamp + ".jpg");

        ImageReader.OnImageAvailableListener imgReaderListener = new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader imageReader) {

                Image image = null;
                image = imgReader.acquireLatestImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.capacity()];
                buffer.get(bytes);
                try {
                    save(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (image != null) {
                        image.close();
                    }
                }

            }
        };

        imgReader.setOnImageAvailableListener(imgReaderListener, backgroundHandler);

        final CameraCaptureSession.CaptureCallback capListenerCallBack = new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);

                Toast.makeText(MainActivity.this, "Image saved successfully", Toast.LENGTH_SHORT).show();
                try {
                    createCameraPreview();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        };

        camDevice.createCaptureSession(outputSurface, new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {

                try {
                    cameraCaptureSession.capture(capBuilder.build(), capListenerCallBack, backgroundHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }


            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

            }
        }, backgroundHandler);

    }

    private void save(byte[] bytes) throws IOException {

        OutputStream outputStream = null;
        outputStream = new FileOutputStream(picFile);
        outputStream.write(bytes);
        outputStream.close();

    }

    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            try {
                openTheCamera();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    private final CameraDevice.StateCallback camStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            camDevice = cameraDevice;
            try {
                createCameraPreview();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            camDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {
            camDevice.close();
            camDevice = null;
        }
    };

    private void createCameraPreview() throws CameraAccessException {

        SurfaceTexture surTexure = mTextureView.getSurfaceTexture();
        surTexure.setDefaultBufferSize(imgDimentions.getWidth(), imgDimentions.getHeight());
        Surface mSurface = new Surface(surTexure);
        capRequestBuilder = camDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        capRequestBuilder.addTarget(mSurface);

        camDevice.createCaptureSession(Arrays.asList(mSurface), new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {

                if (camDevice == null) {
                    return;
                }

                camCapSession = cameraCaptureSession;
                try {
                    updatePreview();
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                Toast.makeText(MainActivity.this, "Configuration cannot be completed!", Toast.LENGTH_SHORT).show();

            }
        }, null);

    }

    private void updatePreview() throws CameraAccessException {

        if (camDevice == null) {
            return;
        }

        capRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        camCapSession.setRepeatingRequest(capRequestBuilder.build(), null, backgroundHandler);

    }

    private void openTheCamera() throws CameraAccessException {

        CameraManager camManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        cameraId = camManager.getCameraIdList() [0];
        CameraCharacteristics camCharacteristics = camManager.getCameraCharacteristics(cameraId);
        StreamConfigurationMap streamMap = camCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        imgDimentions = streamMap.getOutputSizes(SurfaceTexture.class) [0];

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, camPermissionReqCode);
            return;

        }

        camManager.openCamera(cameraId, camStateCallback, null);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == camPermissionReqCode) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED){
                Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
            }
        }

    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("Camera Background");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() throws InterruptedException {
        backgroundThread.quitSafely();
        backgroundThread.join();
        backgroundThread = null;
        backgroundHandler = null;
    }

    @Override
    protected void onResume() {
        super.onResume();

        startBackgroundThread();

        if (mTextureView.isAvailable()) {
            try {
                openTheCamera();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        } else {
            mTextureView.setSurfaceTextureListener(textureListener);
        }

    }

    @Override
    protected void onPause() {

        try {
            stopBackgroundThread();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        super.onPause();
    }

}