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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class LoginActivity extends AppCompatActivity {
    public static final String SERVICE_URL = "com.garmin.marine.activecaptainsample.SERVICE_URL";
    public static final String SERVICE_TICKET = "com.garmin.marine.activecaptainsample.SERVICE_TICKET";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        SharedPreferences sharedPreferences;
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            sharedPreferences = EncryptedSharedPreferences.create(
                    "secret_shared_prefs",
                    masterKeyAlias,
                    getApplicationContext(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }

        // Must be initialized before ActiveCaptainManager.GetInstance() is called the first time.
        String basePath = getApplicationContext().getExternalFilesDir(null).getPath();
        ActiveCaptainManager.init(basePath, sharedPreferences);

        if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        WebView webview = (WebView) findViewById(R.id.login_webview);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.setWebViewClient(new WebViewClient() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl().toString());
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(Uri.parse(url).toString());
            }

            private boolean handleUrl(String url) {
                final String oauthUrlParameterTicket = "ticket=";

                if (url.contains(oauthUrlParameterTicket)) {
                    String serviceUrl = url.substring(0, url.indexOf("?"));
                    String serviceTicket = url.substring(url.indexOf(oauthUrlParameterTicket)).substring(oauthUrlParameterTicket.length());

                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.putExtra(SERVICE_URL, serviceUrl);
                    intent.putExtra(SERVICE_TICKET, serviceTicket);
                    startActivity(intent);
                    finish();

                    return true;
                } else {
                    return false;
                }

            }
        });

        webview.loadUrl(ActiveCaptainConfiguration.SSO_URL);
    }
}