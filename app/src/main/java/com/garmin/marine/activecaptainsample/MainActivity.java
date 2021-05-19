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
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuInflater;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewAssetLoader.AssetsPathHandler;

import com.garmin.marine.activecaptaincommunitysdk.DTO.AcdbUrlAction;
import com.garmin.marine.activecaptainsample.contract.BoundingBox;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private enum LaunchActivity {
        WEBVIEW_ACTIVITY(1);

        LaunchActivity(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        private final int value;
    }

    private static final String IMAGE_ASSETS_BASE_URL = "https://appassets.androidplatform.net/assets/acdb";

    private long markerId = 533549;
    private WebView mWebView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new AssetsPathHandler(this))
                .build();

        mWebView = (WebView) findViewById(R.id.main_webview);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl());
            }

            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(Uri.parse(url));
            }

            private boolean handleUrl(final Uri uri) {
                AcdbUrlAction action = ActiveCaptainManager.getInstance().getDatabase().parseAcdbUrl(uri.toString(), ActiveCaptainManager.getInstance().getCaptainName(), ActiveCaptainConfiguration.REVIEW_LIST_PAGE_SIZE);
                if (action != null) {
                    if (uri.getScheme().equals("acdb")) {
                        switch (action.action) {
                            case EDIT:
                                // Fallthrough is intentional.
                            case REPORT_REVIEW: {
                                String jwt = ActiveCaptainManager.getInstance().getJwt();
                                if (jwt != null && !jwt.isEmpty()) {
                                    Intent intent = new Intent(getApplicationContext(), WebViewActivity.class);
                                    intent.putExtra(WebViewActivity.WEBVIEW_URL, action.content);
                                    intent.putExtra(WebViewActivity.JWT, jwt);
                                    startActivityForResult(intent, LaunchActivity.WEBVIEW_ACTIVITY.getValue());

                                    return true;
                                }
                                break;
                            }
                            default:
                                break;
                        }
                    }
                }

                return false;
            }

            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return handleIntercept(request.getUrl());
            }

            @SuppressWarnings("deprecation")
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                return handleIntercept(Uri.parse(url));
            }

            private WebResourceResponse handleIntercept(final Uri uri) {
                WebResourceResponse response = null;

                if (uri.getScheme().equals("acdb")) {
                    AcdbUrlAction action = ActiveCaptainManager.getInstance().getDatabase().parseAcdbUrl(uri.toString(), ActiveCaptainManager.getInstance().getCaptainName(), ActiveCaptainConfiguration.REVIEW_LIST_PAGE_SIZE);
                    if (action != null) {
                        switch (action.action) {
                            case SHOW_SUMMARY:
                                ActiveCaptainManager.getInstance().reportMarkerViewed(markerId);
                                // Fallthrough is intentional.
                            case SEE_ALL:
                                // Fallthrough is intentional.
                            case SHOW_PHOTOS: {
                                InputStream inputStream = new ByteArrayInputStream(action.content.getBytes(StandardCharsets.UTF_8));
                                response = new WebResourceResponse("text/html", "utf-8", inputStream);
                                break;
                            }
                            case VOTE_REVIEW: {
                                ActiveCaptainManager.getInstance().voteForReview(Long.parseLong(action.content));
                                reloadContent();
                                break;
                            }
                            default:
                                break;
                        }
                    }
                } else if (uri.toString().startsWith(IMAGE_ASSETS_BASE_URL)) {
                    response = assetLoader.shouldInterceptRequest(uri);
                }

                return response;
            }
        });

        ActiveCaptainManager.getInstance().getDatabase().setImagePrefix(getImageBasePath());

        Intent intent = getIntent();

        if (intent.hasExtra(SearchResultsActivity.MARKER_ID)) {
            markerId = intent.getLongExtra(SearchResultsActivity.MARKER_ID, markerId);
            reloadContent();
        } else {
            List<BoundingBox> boundingBoxes = new ArrayList<>();
            boundingBoxes.add(new BoundingBox(17.0, -171.0, 72.0, -64.0));
            ActiveCaptainManager.getInstance().setBoundingBoxes(boundingBoxes);

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());
            executor.execute(() -> {
                if (intent.hasExtra(LoginActivity.SERVICE_URL) && intent.hasExtra(LoginActivity.SERVICE_TICKET)) {
                    String serviceUrl = intent.getStringExtra(LoginActivity.SERVICE_URL);
                    String serviceTicket = intent.getStringExtra(LoginActivity.SERVICE_TICKET);

                    if (serviceUrl != null && !serviceUrl.isEmpty() && serviceTicket != null && !serviceTicket.isEmpty()) {
                        ActiveCaptainManager.getInstance().getAccessToken(serviceUrl, serviceTicket);
                    }
                }

                ActiveCaptainManager.getInstance().setAutoUpdate(true);

                handler.post(this::reloadContent);
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();

        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        return true;
    }

    private void setHtml(String html) {
        mWebView.loadDataWithBaseURL(null, html, "text/html; charset=utf-8", "UTF-8", null);
    }

    private String getImageBasePath() {
        Map<Integer, String> baseImageUrls = new HashMap<Integer, String>() {{
            put(1, "/img/mdpi/");
            put(2, "/img/hdpi/");
            put(3, "/img/xhdpi/");
            put(4, "/img/xxhdpi/");
            put(5, "/img/xxxhdpi/");
        }};

        String baseImageUrl = IMAGE_ASSETS_BASE_URL;

        if (baseImageUrls.containsKey((int) getResources().getDisplayMetrics().density)) {
            baseImageUrl += baseImageUrls.get((int) getResources().getDisplayMetrics().density);
        } else {
            baseImageUrl += baseImageUrls.get(1);
        }

        return baseImageUrl;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        reloadContent();
    }

    private void reloadContent() {
        mWebView.post(() -> mWebView.loadUrl("acdb://summary/" + markerId));
    }
}