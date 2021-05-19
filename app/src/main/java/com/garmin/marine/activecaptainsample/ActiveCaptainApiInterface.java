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

import com.garmin.marine.activecaptainsample.contract.BoundingBox;
import com.garmin.marine.activecaptainsample.contract.TileCoordinate;
import com.garmin.marine.activecaptainsample.contract.request.*;
import com.garmin.marine.activecaptainsample.contract.response.*;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ActiveCaptainApiInterface {
    @GET("api/v1/authentication/access-token")
    Call<String> getAccessToken(@Query("serviceUrl") String serviceUrl, @Query("serviceTicket") String serviceTicket);

    @POST("api/v2/authentication/refresh-token")
    Call<String> getRefreshToken(@Header("Authorization") String authHeader);

    @POST("api/v2/points-of-interest/tiles")
    Call<List<TileCoordinate>> getTiles(@Body List<BoundingBox> request);

    @POST("api/v2/points-of-interest/export")
    Call<List<ExportResponse>> getExports(@Body List<TileCoordinate> request);

    @POST("api/v2.1/points-of-interest/sync-status")
    Call<List<SyncStatusResponse>> getSyncStatus(@Query("databaseVersion") String databaseVersion, @Body List<SyncStatusRequest> request);

    @GET("api/v1/user")
    Call<GetUserResponse> getUser(@Header("Authorization") String authHeader);

    @POST("api/v2/points-of-interest/{id}/view")
    Call<Void> reportMarkerViewed(@Path("id") long id);

    @GET("api/v2/points-of-interest/sync")
    Call<ResponseBody> syncMarkers(@Query("tileX") int tileX, @Query("tileY") int tileY, @Query("lastModifiedAfter") String lastModifiedAfter);

    @GET("api/v2/reviews/sync")
    Call<ResponseBody> syncReviews(@Query("tileX") int tileX, @Query("tileY") int tileY, @Query("lastModifiedAfter") String lastModifiedAfter);

    @POST("api/v2/reviews/{id}/votes")
    Call<ResponseBody> voteForReview(@Path("id") long id, @Header("Authorization") String authHeader);
}
