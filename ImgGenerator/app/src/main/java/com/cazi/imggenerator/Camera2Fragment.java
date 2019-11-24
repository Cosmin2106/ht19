package com.cazi.imggenerator;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Camera2Fragment extends Fragment implements MainActivity.OnStartProcessing {

    private static boolean RUNNING = false;
    private int photoCnt = 0;
    private int FPS;
    private Timer timer;
    private StorageReference sRf;
    private long waitTime = System.currentTimeMillis();
    private Handler handler;
    private HandlerThread thread;
    private TextureView textureView;
    private TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            setupCamera(width, height);
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }
    };

    private String cameraId;
    private CameraDevice cameraDevice;
    private CameraDevice.StateCallback cameraDeviceCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            cameraDevice = null;
        }
    };

    private CaptureRequest captureRequest;
    private CaptureRequest.Builder captureRequestBuilder;
    private CameraCaptureSession captureSession;
    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
        }
    };

    private ImageReader imageReader;
    private ImageReader.OnImageAvailableListener imageReaderListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image rawImage = reader.acquireNextImage();
            if (rawImage != null && RUNNING) {
                sendPictureToCloud(rawImage);
                rawImage.close();
            } else if (rawImage != null) {
                rawImage.close();
            }

            fpsCount();
        }
    };
    private Size imageSize;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_receiver2, container, false);

        textureView = rootView.findViewById(R.id.textureView2);
        textureView.setSurfaceTextureListener(textureListener);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        startThread();
        MainActivity.setOnStartProcessingListener(this);
        sRf = FirebaseStorage.getInstance().getReference();

        if (textureView.isAvailable()) {
            setupCamera(textureView.getWidth(), textureView.getHeight());
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        closeCamera();
        closeThread();
    }

    private void sendPictureToCloud(Image rawImage) {
        RUNNING = false;

        ByteBuffer imgBuffer = rawImage.getPlanes()[0].getBuffer();
        byte[] imgBytes = new byte[imgBuffer.capacity()];
        imgBuffer.get(imgBytes);

        Bitmap imgBitmap = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length, null);
        int width = imgBitmap.getWidth();
        int height = imgBitmap.getHeight();
        int maxSize = 200;

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        imgBitmap = Bitmap.createScaledBitmap(imgBitmap, width, height, true);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        imgBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = sRf.child("images/" + photoCnt++ + "_ss.JPEG").putBytes(data);
        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Log.d("Hack19", "upload SUCCESS");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("Hack19", "FAIL: " + e.toString());
            }
        });
    }

    private void setupCamera(int width, int height) {
        CameraManager cameraManager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String id : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                if (characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT)
                    continue;
                cameraId = id;

                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                Size[] outputSizes = map.getOutputSizes(SurfaceTexture.class);
                imageSize = getOptimalSize(outputSizes, width, height);
                imageReader = ImageReader.newInstance(imageSize.getWidth(), imageSize.getHeight(), ImageFormat.JPEG, 1);
                imageReader.setOnImageAvailableListener(imageReaderListener, handler);
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
        CameraManager cameraManager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            cameraManager.openCamera(cameraId, cameraDeviceCallback, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startPreview() {
        SurfaceTexture surfaceTexture = textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(imageSize.getWidth(), imageSize.getHeight());

        Surface previewSurface = new Surface(surfaceTexture);
        Surface renderSurface = imageReader.getSurface();
        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_STATE_ACTIVE_SCAN);
            captureRequestBuilder.addTarget(previewSurface);
            captureRequestBuilder.addTarget(renderSurface);

            cameraDevice.createCaptureSession(Arrays.asList(previewSurface, renderSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            if (cameraDevice == null)
                                return;
                            try {
                                captureRequest = captureRequestBuilder.build();
                                captureSession = session;
                                captureSession.setRepeatingRequest(captureRequest, captureCallback, handler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Toast.makeText(getContext(), "Failed to configure camera preview.", Toast.LENGTH_LONG).show();
                        }
                    }, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader !=  null) {
            imageReader.close();
            imageReader = null;
        }
    }

    private void startThread() {
        thread = new HandlerThread("Camera2Thread");
        thread.start();
        handler = new Handler(thread.getLooper());
    }

    private void closeThread() {
        thread.quitSafely();
        try {
            thread.join();
            thread = null;
            handler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Size getOptimalSize(Size[] mapSizes, int width, int height) {
        List<Size> bestSizes = new ArrayList<>();
        for (Size size: mapSizes) {
            if (width > height) {
                if (size.getWidth() > width && size.getHeight() > height)
                    bestSizes.add(size);
            }
            else {
                if (size.getWidth() > height && size.getHeight() > width)
                    bestSizes.add(size);
            }
        }

        if (bestSizes.size() > 0)
            return Collections.min(bestSizes, new SizeComparator());
        return mapSizes[0];
    }

    @Override
    public void startProcessing() {
        RUNNING = true;
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                RUNNING = true;
            }
        }, 0, 1000);
    }

    @Override
    public void stopProcessing() {
        Log.d("Hack19", "STOP PROCESSING");
        if (timer != null)
        RUNNING = false;
    }

    private class SizeComparator implements Comparator<Size> {

        @Override
        public int compare(Size o1, Size o2) {
            return Long.signum(o1.getWidth() * o1.getHeight() - o2.getWidth() * o2.getHeight());
        }
    }

    private void fpsCount() {
        FPS++;
        long currentTime = System.currentTimeMillis();
        if ((currentTime - waitTime) / 1000 > 1) {
//            Log.d("transLight", "FPS: " + FPS + " (camera2)");
            waitTime = System.currentTimeMillis();
            FPS = 0;
        }
    }
}
