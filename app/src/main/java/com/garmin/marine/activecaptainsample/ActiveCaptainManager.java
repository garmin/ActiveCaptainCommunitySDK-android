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

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.format.DateUtils;
import android.util.Log;

import com.garmin.marine.activecaptaincommunitysdk.ActiveCaptainDatabase;
import com.garmin.marine.activecaptaincommunitysdk.DTO.LastUpdateInfoType;
import com.garmin.marine.activecaptaincommunitysdk.DTO.TileXY;
import com.garmin.marine.activecaptainsample.contract.BoundingBox;
import com.garmin.marine.activecaptainsample.contract.TileCoordinate;
import com.garmin.marine.activecaptainsample.contract.request.SyncStatusRequest;
import com.garmin.marine.activecaptainsample.contract.response.ExportResponse;
import com.garmin.marine.activecaptainsample.contract.response.GetUserResponse;
import com.garmin.marine.activecaptainsample.contract.response.SyncStatusResponse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class ActiveCaptainManager {
    private static class SingletonHolder {
        private static final ActiveCaptainManager INSTANCE = new ActiveCaptainManager();
    }

    private static final int SYNC_MAX_RESULT_COUNT = 100;
    private static final String JWT_KEY = "JWT";

    private static String basePath;
    private static SharedPreferences sharedPreferences;

    private final ActiveCaptainApiInterface apiInterface;
    private String captainName;
    private final ActiveCaptainDatabase database;
    private final ExportDownloader exportDownloader;
    private List<BoundingBox> boundingBoxes;

    private Runnable updateTask;
    private final Handler updateHandler;

    private enum SyncResult {
        SUCCESS,
        FAIL,
        EXPORT_REQUIRED
    }

    private ActiveCaptainManager() {
        if (basePath == null) {
            throw new IllegalArgumentException("basePath must not be null.");
        }

        if (sharedPreferences == null) {
            throw new IllegalArgumentException("sharedPreferences must not be null.");
        }

        database = new ActiveCaptainDatabase(new File(basePath, "active_captain.db"), ActiveCaptainConfiguration.languageCode);

        apiInterface = ActiveCaptainApiClient.getClient().create(ActiveCaptainApiInterface.class);
        boundingBoxes = new LinkedList<>();

        exportDownloader = new ExportDownloader(database, basePath);

        captainName = null;

        HandlerThread updateThread = new HandlerThread("UpdateThread");
        updateThread.start();
        updateHandler = new Handler(updateThread.getLooper());
        updateTask = () -> {
            updateData();
            updateHandler.postDelayed(updateTask, ActiveCaptainConfiguration.UPDATE_INTERVAL_MINS * DateUtils.MINUTE_IN_MILLIS);
        };
    }

    public void getAccessToken(String serviceUrl, String serviceTicket) {
        Call<String> call = apiInterface.getAccessToken(serviceUrl, serviceTicket);
        try {
            Response<String> response = call.execute();
            if (response.isSuccessful()) {
                setJwt(response.body());
            } else {
                Log.e("Error: ", "Failed to get access token, " + response.code() + " " + response.body());
            }

        } catch (IOException e) {
            Log.e("Error: ", "Failed to get access token, " + e.getMessage());
        }
    }

    public String getCaptainName() {
        if (captainName == null && getJwt() != null) {
            Call<GetUserResponse> call = apiInterface.getUser("Bearer " + getJwt());
            try {
                Response<GetUserResponse> response = call.execute();
                if (response.isSuccessful() && response.body() != null) {
                    captainName = response.body().CaptainName;
                }
            } catch (IOException e) {
                Log.e("Error: ", "Failed to get user, " + e.getMessage());
            }
        }

        return captainName;
    }

    public ActiveCaptainDatabase getDatabase() {
        return database;
    }

    public static ActiveCaptainManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public String getJwt() {
        return sharedPreferences.getString(JWT_KEY, null);
    }

    // Must be called before calling GetInstance() the first time.
    public static void init(String _basePath, SharedPreferences _sharedPreferences) {
        basePath = _basePath;
        sharedPreferences = _sharedPreferences;
    }

    public void refreshToken() {
        Call<String> call = apiInterface.getRefreshToken("Bearer " + getJwt());
        try {
            Response<String> response = call.execute();
            if (response.isSuccessful()) {
                setJwt(response.body());
            } else {
                Log.e("Error: ", "Failed to get access token, " + response.code() + " " + response.body());
            }

        } catch (IOException e) {
            Log.e("Error: ", "Failed to get access token, " + e.getMessage());
        }
    }

    public void reportMarkerViewed(long markerId) {
        Call<Void> call = apiInterface.reportMarkerViewed(markerId);
        try {
            call.execute();
            // If this call fails, we are not required to retry or queue for later.
        } catch (IOException e) {
            Log.e("Error: ", "Failed to report marker viewed, " + e.getMessage());
        }
    }

    public void setAutoUpdate(boolean enabled) {
        if (enabled) {
            updateTask.run();
        } else {
            updateHandler.removeCallbacks(updateTask);
        }
    }

    public void setBoundingBoxes(List<BoundingBox> boundingBoxes) {
        this.boundingBoxes = boundingBoxes;
    }

    public void setJwt(String jwt) {
        sharedPreferences.edit().putString(JWT_KEY, jwt).apply();
    }

    public void updateData() {
        Log.d("ActiveCaptainManager", "UpdateData called");

        if (boundingBoxes.isEmpty()) {
            return;
        }

        HashMap<TileXY, LastUpdateInfoType> lastUpdateInfos = new HashMap<>();
        for (BoundingBox boundingBox : boundingBoxes) {
            HashMap<TileXY, LastUpdateInfoType> bboxLastUpdateInfos = database.getTilesLastModifiedByBoundingBox(boundingBox.southwestCorner.latitude, boundingBox.southwestCorner.longitude, boundingBox.northeastCorner.latitude, boundingBox.northeastCorner.longitude);
            if (bboxLastUpdateInfos != null) {
                lastUpdateInfos.putAll(bboxLastUpdateInfos);
            }
        }

        List<SyncStatusRequest> tileRequests = new LinkedList<>();

        if (lastUpdateInfos.isEmpty()) {
            // Database not present, need to get tiles from API.
            Call<List<TileCoordinate>> tileCall = apiInterface.getTiles(boundingBoxes);
            try {
                Response<List<TileCoordinate>> response = tileCall.execute();
                if (response.isSuccessful() && response.body() != null) {
                    for (TileCoordinate tileCoordinate : response.body()) {
                        tileRequests.add(new SyncStatusRequest(tileCoordinate.tileX, tileCoordinate.tileY, null, null));
                    }
                }
            } catch (IOException e) {
                Log.e("Error: ", "Failed to get tiles for bounding box, " + e.getMessage());
            }
        } else {
            for (Map.Entry<TileXY, LastUpdateInfoType> entry : lastUpdateInfos.entrySet()) {
                tileRequests.add(new SyncStatusRequest(entry.getKey().tileX, entry.getKey().tileY, entry.getValue().markerLastUpdate, entry.getValue().reviewLastUpdate));
            }
        }

        Set<TileCoordinate> exportTileList = new HashSet<>();

        Call<List<SyncStatusResponse>> call = apiInterface.getSyncStatus(database.getVersion(), tileRequests);
        try {
            Response<List<SyncStatusResponse>> response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                for (SyncStatusResponse tileResponse : response.body()) {
                    TileCoordinate tileCoordinate = new TileCoordinate(tileResponse.TileX, tileResponse.TileY);

                    switch (tileResponse.PoiUpdateType) {
                        case Sync:
                            if (syncTileMarkers(tileCoordinate) == SyncResult.EXPORT_REQUIRED) {
                                exportTileList.add(tileCoordinate);
                            }
                            break;
                        case Export:
                            exportTileList.add(tileCoordinate);
                            break;
                        case Delete:
                            database.deleteTile(tileCoordinate.tileX, tileCoordinate.tileY);
                            break;
                    }

                    switch (tileResponse.ReviewUpdateType) {
                        case Sync:
                            if (syncTileReviews(tileCoordinate) == SyncResult.EXPORT_REQUIRED) {
                                exportTileList.add(tileCoordinate);
                            }
                            break;
                        case Export:
                            exportTileList.add(tileCoordinate);
                            break;
                        case Delete:
                            database.deleteTileReviews(tileCoordinate.tileX, tileCoordinate.tileY);
                            break;
                    }
                }
            }
        } catch (IOException e) {
            Log.e("Error: ", "Failed to get sync status response, " + e.getMessage());
        }

        if (!exportTileList.isEmpty()) {
            exportTiles(exportTileList);

            // Reinitialize translations, as they may have been updated.
            database.setLanguage(ActiveCaptainConfiguration.languageCode);
            Log.d("ActiveCaptainManager", "Update complete, exports installed.");
        } else {
            Log.d("ActiveCaptainManager", "Update complete, no exports.");
        }
    }

    public void voteForReview(long reviewId) {
        if (getJwt() != null) {
            Call<ResponseBody> call = apiInterface.voteForReview(reviewId, "Bearer " + getJwt());
            try {
                Response<ResponseBody> response = call.execute();
                if (response.isSuccessful() && response.body() != null) {
                    database.processVoteForReviewResponse(response.body().string());
                }
            } catch (IOException e) {
                Log.e("Error: ", "Failed to vote for review, " + e.getMessage());
            }
        }
    }

    private void exportTiles(Set<TileCoordinate> tileList) {
        List<TileCoordinate> tileRequests = new ArrayList<>();

        for (TileCoordinate tile : tileList) {
            tileRequests.add(new TileCoordinate(tile.tileX, tile.tileY));
        }

        Call<List<ExportResponse>> call = apiInterface.getExports(tileRequests);
        try {
            Response<List<ExportResponse>> response = call.execute();

            if (response.isSuccessful()) {
                exportDownloader.download(response.body());
            } else {
                Log.e("Error: ", "Failed to get export URLs, " + response.code() + " " + response.message());
            }
        } catch (IOException e) {
            Log.e("Error: ", "Failed to get export URLs, " + e.getMessage());
        }
    }

    private SyncResult syncTileMarkers(TileCoordinate tile) {
        SyncResult result = SyncResult.FAIL;

        String lastModifiedAfter = "";
        int resultCount = 0;

        do {
            LastUpdateInfoType lastUpdateInfo = database.getTileLastModified(tile.tileX, tile.tileY);

            if (lastModifiedAfter.equals(lastUpdateInfo.markerLastUpdate)) {
                // Sanity check -- if lastModifiedAfter would be the same for multiple calls, break
                // out of the loop.  The API would return the same markers.
                break;
            }

            lastModifiedAfter = lastUpdateInfo.markerLastUpdate;

            Call<ResponseBody> call = apiInterface.syncMarkers(tile.tileX, tile.tileY, lastModifiedAfter);

            try {
                Response<ResponseBody> response = call.execute();
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        resultCount = database.processSyncMarkersResponse(response.body().string(), tile.tileX, tile.tileY);
                        result = SyncResult.SUCCESS;
                    } catch (IOException e) {
                        Log.e("Error: ", "Failed to read marker sync response, " + response.code() + " " + response.message());
                    }
                } else if (response.code() == 303) {
                    result = SyncResult.EXPORT_REQUIRED;
                } else {
                    Log.e("Error: ", "Failed to sync markers, " + response.code() + " " + response.message());
                    result = SyncResult.FAIL;
                }
            } catch (IOException e) {
                Log.e("Error: ", "Failed to read marker sync response, " + e.getMessage());
                result = SyncResult.FAIL;
            }
        } while (result == SyncResult.SUCCESS && resultCount == SYNC_MAX_RESULT_COUNT);

        return result;
    }

    private SyncResult syncTileReviews(TileCoordinate tile) {
        SyncResult result = SyncResult.FAIL;

        String lastModifiedAfter = "";
        int resultCount = 0;

        do {
            LastUpdateInfoType lastUpdateInfo = database.getTileLastModified(tile.tileX, tile.tileY);

            if (lastModifiedAfter.equals(lastUpdateInfo.reviewLastUpdate)) {
                // Sanity check -- if lastModifiedAfter would be the same for multiple calls, break
                // out of the loop.  The API would return the same markers.
                break;
            }

            lastModifiedAfter = lastUpdateInfo.reviewLastUpdate;

            Call<ResponseBody> call = apiInterface.syncReviews(tile.tileX, tile.tileY, lastModifiedAfter);

            try {
                Response<ResponseBody> response = call.execute();
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        resultCount = database.processSyncReviewsResponse(response.body().string(), tile.tileX, tile.tileY);
                        result = SyncResult.SUCCESS;
                    } catch (IOException e) {
                        Log.e("Error: ", "Failed to read review sync response, " + response.code() + " " + response.message());
                    }
                } else if (response.code() == 303) {
                    result = SyncResult.EXPORT_REQUIRED;
                } else {
                    Log.e("Error: ", "Failed to sync reviews, " + response.code() + " " + response.message());
                }
            } catch (IOException e) {
                Log.e("Error: ", "Failed to read review sync response, " + e.getMessage());
            }
        } while (result == SyncResult.SUCCESS && resultCount == SYNC_MAX_RESULT_COUNT);

        return result;
    }
}
