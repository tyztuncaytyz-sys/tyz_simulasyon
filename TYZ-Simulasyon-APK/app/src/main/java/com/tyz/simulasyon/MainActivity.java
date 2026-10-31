package com.tyz.simulasyon;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.*;
import android.widget.Toast;
import java.io.*;

public class MainActivity extends Activity {
    private WebView web;
    private ValueCallback<Uri[]> chooser;
    private static final int REQ = 11;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        web.addJavascriptInterface(new Object() {
            @JavascriptInterface public void save(String name, String text) { saveFile(name, text); }
        }, "AndroidSave");
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (chooser != null) chooser.onReceiveValue(null);
                chooser = cb;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                startActivityForResult(Intent.createChooser(i, "G-kod dosyası seç"), REQ);
                return true;
            }
        });
        web.loadUrl("file:///android_asset/index.html");
    }

    private void saveFile(String name, String text) {
        try {
            String safe = name.replaceAll("[\\\\/:*?\"<>|]", "_");
            OutputStream os;
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues v = new ContentValues();
                v.put(MediaStore.Downloads.DISPLAY_NAME, safe);
                v.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
                v.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri u = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                os = getContentResolver().openOutputStream(u);
            } else {
                File d = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                d.mkdirs();
                os = new FileOutputStream(new File(d, safe));
            }
            os.write(text.getBytes("UTF-8")); os.close();
            runOnUiThread(() -> Toast.makeText(this, "İndirilenler klasörüne kaydedildi: " + safe, Toast.LENGTH_LONG).show());
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(this, "Kaydedilemedi: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    @Override protected void onActivityResult(int req, int res, Intent d) {
        if (req == REQ && chooser != null) {
            chooser.onReceiveValue(res == RESULT_OK && d != null && d.getData() != null ? new Uri[]{d.getData()} : null);
            chooser = null;
        }
    }

    @Override public void onBackPressed() { if (web.canGoBack()) web.goBack(); else super.onBackPressed(); }
}
