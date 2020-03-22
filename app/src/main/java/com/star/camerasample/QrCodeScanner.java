package com.star.camerasample;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.star.camerasample.zxing.Result;
import com.star.camerasample.zxing.ZXingScannerView;

public class QrCodeScanner extends AppCompatActivity implements ZXingScannerView.ResultHandler {
    private ZXingScannerView mScannerView;
    public int item=0;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        // Programmatically initialize the scanner view
        mScannerView = new ZXingScannerView(this);

        // Set the scanner view as the content view
        setContentView(mScannerView);

    }

    @Override
    public void onResume() {
        super.onResume();
        // Register ourselves as a handler for scan results.
        mScannerView.setResultHandler(this);
        // Start camera on resume
        mScannerView.startCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Stop camera on pause
        mScannerView.stopCamera();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void handleResult(Result rawResult) {

        try {
            String qr_scan_result=rawResult.getText();
            Intent intent = new Intent();
            intent.putExtra("result_qr", rawResult.getText());
            Toast.makeText(getApplicationContext(),rawResult.getText(),Toast.LENGTH_LONG).show();
            setResult(RESULT_OK, intent);
            finish();

        } catch (Exception ex) {
            ex.printStackTrace();
        }


    }
}