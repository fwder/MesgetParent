package com.weclont.mesgetparent;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MapActivity extends AppCompatActivity {

    // 声明
    private WebView wb;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // 获取传入的经纬度
        String Longitude = getIntent().getStringExtra("Longitude");
        String Latitude = getIntent().getStringExtra("Latitude");

        // 获取WebViewID
        wb = findViewById(R.id.wb);
        // 加载网页 html 文件
        // 支持 JS 和 DOM
        wb.getSettings().setJavaScriptEnabled(true);
        wb.getSettings().setDomStorageEnabled(true);
        // 防止所有后打开默认浏览器
        wb.setWebViewClient(new MyWebViewClient());
        wb.setWebChromeClient(new MyWebChromeClient());
        wb.loadUrl("https://mesget.fwder.cn/map.php?longitude="+Longitude+"&latitude="+Latitude);
    }

    /**
     * 防止返回到之前的 Activity
     *
     * @param keyCode 按键
     * @param event   事件
     * @return true
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && wb.canGoBack()) {
            wb.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    static class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            view.loadUrl(request.getUrl().toString());
            return super.shouldOverrideUrlLoading(view, request);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            Log.d("WebViewClient", "Page started...");
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            Log.d("WebViewClient", "Page Finished...");
        }
    }

    /**
     * 添加所有记录和 Title
     */
    class MyWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            setTitle(title);
        }
    }
}
