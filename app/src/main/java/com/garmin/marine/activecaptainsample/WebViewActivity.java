/*------------------------------------------------------------------------------
Copyright 2021 Garmin Ltd. or its subsidiaries.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
------------------------------------------------------------------------------*/

package com.garmin.marine.activecaptainsample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class WebViewActivity extends AppCompatActivity {
    public static final int FILE_CHOOSER_RESULT_CODE = 3;
    public static final int ASK_STORAGE_PERMISSION = 4;
    public static final int MARKER_DELETED_RESULT = 10;
    public static final int MARKER_UPDATE_ERROR_RESULT = 11;

    public static final String JWT = "com.garmin.marine.activecaptainsample.JWT";
    public static final String WEBVIEW_URL = "com.garmin.marine.activecaptainsample.WEBVIEW_URL";

    private static class WebViewEvent {
        public boolean applyResponse;
        public boolean finishActivity;
        public int resultCode;

        public WebViewEvent(boolean applyResponse, boolean finishActivity, int resultCode) {
            this.applyResponse = applyResponse;
            this.finishActivity = finishActivity;
            this.resultCode = resultCode;
        }
    }

    private static class WebUploadFields {
        public ValueCallback<Uri[]> filePathCallback;
        public WebChromeClient.FileChooserParams fileChooserParams;

        public WebUploadFields(ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
            this.filePathCallback = filePathCallback;
            this.fileChooserParams = fileChooserParams;
        }
    }

    private WebUploadFields webUploadFields = null;

    private final HashMap<String, WebViewEvent> webViewEvents = new HashMap<String, WebViewEvent>() {{
        put("actionComplete", new WebViewEvent(false, false, Activity.RESULT_OK));
        put("DELETE", new WebViewEvent(true, true, MARKER_DELETED_RESULT));
        put("EDITPROFILE", new WebViewEvent(false, false, Activity.RESULT_OK));
        put("ERROR", new WebViewEvent(false, true, MARKER_UPDATE_ERROR_RESULT));
        put("REVIEWDELETE", new WebViewEvent(true, true, Activity.RESULT_OK));
        put("REVIEWFLAGGED", new WebViewEvent(true, true, Activity.RESULT_OK));
        put("REVIEWSUCCESS", new WebViewEvent(true, true, Activity.RESULT_OK));
        put("SUCCESS", new WebViewEvent(true, true, Activity.RESULT_OK));
    }};

    private class JSInterface {
        @JavascriptInterface
        public void postMessage(String content) {
            try {
                JSONObject result = new JSONObject(content);

                String resultType = result.getString("resultType");
                WebViewEvent event = webViewEvents.get(resultType);

                String jwt = result.getString("jwt");
                if (!jwt.isEmpty()) {
                    ActiveCaptainManager.getInstance().setJwt(jwt);
                }

                if (event != null) {
                    if (event.applyResponse) {
                        ActiveCaptainManager.getInstance().getDatabase().processWebViewResponse(content);
                    }

                    setResult(event.resultCode);

                    if (event.finishActivity) {
                        finish();
                    }
                } else {
                    finish();
                }
            } catch (JSONException e) {
                finish();
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        Intent intent = getIntent();
        String jwt = intent.getStringExtra(JWT);
        String url = intent.getStringExtra(WEBVIEW_URL);

        Map<String, String> headers = new HashMap<String, String>() {
            {
                put("apikey", ActiveCaptainConfiguration.API_KEY);
                put("Authorization", "Bearer " + jwt);
            }
        };

        WebView webView = (WebView) findViewById(R.id.edit_webview);
        webView.getSettings().setJavaScriptEnabled(true);
        JSInterface jsInterface = new JSInterface();

        for (String name : webViewEvents.keySet()) {
            webView.addJavascriptInterface(jsInterface, name);
        }
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                webUploadFields = new WebUploadFields(filePathCallback, fileChooserParams);
                pickImage();
                return true;
            }
        });

        String webViewUrl = ActiveCaptainConfiguration.WEBVIEW_BASE_URL + "/" + url + "?" + "version=v2.1&locale=" + ActiveCaptainConfiguration.languageCode;
        if (ActiveCaptainConfiguration.WEBVIEW_DEBUG) {
            webViewUrl += "&debug=true";
        }
        webView.loadUrl(webViewUrl, headers);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            webUploadFields.filePathCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            webUploadFields = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        if (requestCode == ASK_STORAGE_PERMISSION) {
            if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickImage();
            } else {
                webUploadFields = null;
            }
        }
    }

    private void pickImage() {
        if (webUploadFields == null || webUploadFields.filePathCallback == null) {
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");

            startActivityForResult(Intent.createChooser(intent, getString(R.string.file_browser)), FILE_CHOOSER_RESULT_CODE);
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getString(R.string.permissions_external))
                        .setPositiveButton("Yes", (dialog, id) -> ActivityCompat.requestPermissions(getParent(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, ASK_STORAGE_PERMISSION))
                        .setNegativeButton("No", (dialog, id) -> webUploadFields = null)
                        .setCancelable(false)
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, ASK_STORAGE_PERMISSION);
            }
        }
    }
}
