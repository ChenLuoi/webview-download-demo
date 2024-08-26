package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebMessage;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

interface WebAppInterface {

}

public class MainActivity extends AppCompatActivity {
    public static int REQUEST_PERMISSION_CODE = 123;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // webview config, allow cors in this demo project
        WebView webView = findViewById(R.id.webview);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);

        // load file in the assets
        webView.loadUrl("file:///android_asset/local.html");

        // register api on window obj of the web page
        webView.addJavascriptInterface(new WebAppInterface() {
            @JavascriptInterface
            public void saveDataToStorage(String taskId, String name, String data) {
                boolean success = true;
                try {
                    // decode file data
                    byte[] decodedBytes = Base64.decode(data, Base64.DEFAULT);

                    // ensure Download/Board exists
                    File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    File directory = new File(downloadDir.getAbsolutePath(), "Board1");
                    if (!directory.exists()) {
                        directory.mkdirs();
                    }

                    // write binary to specific file
                    File file = new File(directory, name);
                    while (file.exists()) {
                        file = new File(directory, "dup-" + file.getName());
                        Log.d(TAG, "saveDataToStorage: " + file.getAbsolutePath());
                    }
                    FileOutputStream os = new FileOutputStream(file);
                    os.write(decodedBytes);
                    os.close();
                    Toast.makeText(MainActivity.this, "文件保存成功：" + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    success = false;
                    e.printStackTrace();
                }
                boolean finalSuccess = success;
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        webView.postWebMessage(new WebMessage("{\"type\":\"callback\",\"key\":\"" + taskId + "\",\"success\":" + finalSuccess + "}"), Uri.parse("*"));
                    }
                });
            }
        }, "Android");

        // simple request permission to access the storage
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_CODE);
            }
        }
    }
}

