package com.example.mlkitinjava;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private PreviewView cameraPv;
    private TextView res , resInv;
    private AppCompatImageView imageView;

    private ProcessCameraProvider cameraProvider;
    private Preview previewUseCase;
    private CameraSelector cameraSelector;
    private ImageAnalysis analysingUseCase;

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createView();
        cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

        if(!isCameraPermissionGranted()){
            askCameraPermission();
        }
        askCameraPermission();
        startCamera();
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void startCamera() {
        initiateCameraProvider();
        bindPreview();
        bindAnalyser();
    }

    private void bindPreview() {

        if (cameraProvider==null){
            Log.d("ABHI", "bindPreview: cameraProvider null");
            return;
        }
        if (previewUseCase != null){
            cameraProvider.unbind(previewUseCase);
        }
        previewUseCase =new Preview.Builder()
                .setTargetRotation(cameraPv.getDisplay().getRotation())
                .build();
        previewUseCase.setSurfaceProvider(cameraPv.getSurfaceProvider());

        try {
            Camera camera = cameraProvider.bindToLifecycle(MainActivity.this , cameraSelector , previewUseCase);

        }
        catch ( IllegalStateException illegalStateException) {
            Log.e("ABHI",  "IllegalStateException");
        } catch (IllegalArgumentException illegalArgumentException) {
            Log.e("ABHI", "IllegalArgumentException");
        }

    }

    private void bindAnalyser() {

        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build();

        BarcodeScanner barcodeScanner = BarcodeScanning.getClient(options);

        if (cameraProvider == null){
            Log.d("ABHI", "bindAnalyser: cameraProvider null");
            return;
        }

        if (analysingUseCase!= null){
            cameraProvider.unbind(analysingUseCase);
        }

        analysingUseCase = new ImageAnalysis.Builder()
                .setTargetRotation(Surface.ROTATION_0)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        Executor cameraExecutor = Executors.newSingleThreadExecutor();

        analysingUseCase.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                Log.d("ABHI", "analyze: called");
                Bitmap bitmap = BitmapUtils.getBitmap(image);

//                Bitmap bitmap1 = changeColor(bitmap , Color.WHITE , Color.BLACK);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        imageView.setImageBitmap(bitmap);
                    }
                });
//                inverse(bitmap , imageView);
//                processImageProxy(barcodeScanner, image);
                processInverseImageProxy(barcodeScanner , image , bitmap);
//                image.close();
            }
        });

        try {
            Camera camera = cameraProvider.bindToLifecycle(this , cameraSelector , analysingUseCase);

        }
        catch ( IllegalStateException illegalStateException) {
            Log.e("ABHI",  "IllegalStateException analyse");
        } catch (IllegalArgumentException illegalArgumentException) {
            Log.e("ABHI", "IllegalArgumentException analyse");
        }

    }

    private Bitmap changeColor(Bitmap src, int colorToReplace, int colorThatWillReplace) {
//        int width = src.getWidth();
//        int height = src.getHeight();
//        int[] pixels = new int[width * height];
//        // get pixel array from source
//        src.getPixels(pixels, 0, width, 0, 0, width, height);
//
//        Bitmap bmOut = Bitmap.createBitmap(width, height, src.getConfig());
//
//        int A, R, G, B;
//        int pixel;

        // iteration through pixels
        for (int x = 0; x < src.getWidth(); ++x) {
            for (int y = 0; y < src.getHeight(); ++y) {
                int color = src.getPixel(x, y);
                int r = Color.red(color);
                int g = Color.green(color);
                int b = Color.blue(color);
                int avg = (r + g + b) / 3;
                int newColor = Color.argb(255, 255 - avg, 255 - avg, 255 - avg);
                src.setPixel(x, y, newColor);
            }
        }
//        bmOut.setPixels(pixels, 0, width, 0, 0, width, height);
        return src;
    }
    @SuppressLint("UnsafeOptInUsageError")
    private void processImageProxy(BarcodeScanner barcodeScanner, ImageProxy imageproxy) {

        InputImage inputImage = InputImage.fromMediaImage(imageproxy.getImage() , imageproxy.getImageInfo().getRotationDegrees());

        barcodeScanner.process(inputImage).addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
            @Override
            public void onSuccess(@NonNull List<Barcode> barcodes) {
                for(Barcode barcode : barcodes){
                    res.setText(barcode.getRawValue());
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("ABHI", "onFailure: barcode scan");
            }
        }).addOnCompleteListener(new OnCompleteListener<List<Barcode>>() {
            @Override
            public void onComplete(@NonNull Task<List<Barcode>> task) {
                imageproxy.close();
            }
        });

    }

    @SuppressLint("UnsafeOptInUsageError")
    private void processInverseImageProxy(BarcodeScanner barcodeScanner, ImageProxy imageproxy , Bitmap bitmap) {

//        InputImage inputImage = InputImage.fromMediaImage(imageproxy.getImage() , imageproxy.getImageInfo().getRotationDegrees());

        InputImage inputImage = InputImage.fromBitmap(bitmap , imageproxy.getImageInfo().getRotationDegrees());

        barcodeScanner.process(inputImage).addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
            @Override
            public void onSuccess(@NonNull List<Barcode> barcodes) {
                for(Barcode barcode : barcodes){
                    resInv.setText(barcode.getRawValue());
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("ABHI", "onFailure: barcode scan");
            }
        }).addOnCompleteListener(new OnCompleteListener<List<Barcode>>() {
            @Override
            public void onComplete(@NonNull Task<List<Barcode>> task) {
                imageproxy.close();
            }
        });

    }

    private void askCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},101);
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == 101){
            Log.d("ABHI", "onRequestPermissionsResult: Granted");
            startCamera();
        }
        else{
            Log.d("ABHI", "onRequestPermissionsResult: Not Granted");
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }



    private void createView() {
        res = findViewById(R.id.res);
        cameraPv = findViewById(R.id.camera_pv);
        imageView = findViewById(R.id.imagview);
        resInv = findViewById(R.id.resInverse);
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void initiateCameraProvider() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(getApplicationContext());
        future.addListener(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Log.d("ABHI", "run: listening future");
                            cameraProvider = future.get();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }, ContextCompat.getMainExecutor(getApplicationContext())
        );
    }

    private boolean isCameraPermissionGranted() {
        boolean ans = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        Log.d("ABHI", "isCameraPermissionGranted: " + ans);
        return ans;
    }



    private void inverse(Bitmap sourceBitmap , AppCompatImageView imageView){

        Bitmap resultBitmap = Bitmap.createBitmap(sourceBitmap, 0, 0,
                sourceBitmap.getWidth() - 1, sourceBitmap.getHeight() - 1);
        Paint p = new Paint();
        ColorFilter filter = new LightingColorFilter(Color.DKGRAY, 1);
        p.setColorFilter(filter);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageView.setImageBitmap(resultBitmap);

                Canvas canvas = new Canvas(resultBitmap);
                canvas.drawBitmap(resultBitmap, 0, 0, p);
            }
        });
    }

}