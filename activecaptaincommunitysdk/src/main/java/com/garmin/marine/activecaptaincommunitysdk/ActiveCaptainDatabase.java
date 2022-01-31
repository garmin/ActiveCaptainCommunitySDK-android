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

package com.garmin.marine.activecaptaincommunitysdk;

import com.garmin.marine.activecaptaincommunitysdk.DTO.AcdbUrlAction;
import com.garmin.marine.activecaptaincommunitysdk.DTO.CoordinateFormatType;
import com.garmin.marine.activecaptaincommunitysdk.DTO.DateFormatType;
import com.garmin.marine.activecaptaincommunitysdk.DTO.DistanceUnit;
import com.garmin.marine.activecaptaincommunitysdk.DTO.LastUpdateInfoType;
import com.garmin.marine.activecaptaincommunitysdk.DTO.SearchMarker;
import com.garmin.marine.activecaptaincommunitysdk.DTO.TileXY;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class ActiveCaptainDatabase implements Closeable {
    private long ptrHolder = 0;

    private native void init(String databasePath, String language);
    private native void cleanup();

    /**
     * Delete the SQLite database.
     */
    public native void deleteDatabase();

    /**
     * Delete markers and reviews for the specified tile from the SQLite database.
     * @param tileX tile X coordinate, valid values are 0-15
     * @param tileY tile Y coordinate, valid values are 0-15
     */
    public native void deleteTile(int tileX, int tileY);

    /**
     * Delete reviews for the specified tile from the SQLite database.
     * @param tileX tile X coordinate, valid values are 0-15
     * @param tileY tile Y coordinate, valid values are 0-15
     */
    public native void deleteTileReviews(int tileX, int tileY);

    /**
     * Retrieve marker and review last modified values for the specified tile.
     * @param tileX tile X coordinate, valid values are 0-15
     * @param tileY tile Y coordinate, valid values are 0-15
     * @return LastUpdateInfoType with marker and review last modified values initialized.
     */
    public native LastUpdateInfoType getTileLastModified(int tileX, int tileY);

    /**
     * Retrieve tile coordinates and marker/review last modified values for tiles overlapped by the specified bounding box.
     * @param south longitude of southern edge of bounding box
     * @param west latitude of western edge of bounding box
     * @param north longitude of northern edge of bounding box
     * @param east latitude of eastern edge of bounding box
     * @return hash map of LastUpdateInfoType objects by TileXY
     */
    public native HashMap<TileXY, LastUpdateInfoType> getTilesLastModifiedByBoundingBox(double south, double west, double north, double east);

    /**
     * Get database version
     * @return String containing database version.  If SQLite database is present, will be 2.x.x.x.  If not, will be 0.0.0.0.
     */
    public native String getVersion();

    /**
     * Install specified tile in SQLite database.  May overwrite or merged into the existing database.
     * @param path path to the SQLite file to install
     * @param tileX tile X coordinate, valid values are 0-15
     * @param tileY tile Y coordinate, valid values are 0-15
     */
    public native void installTile(String path, int tileX, int tileY);

    /**
     * Search for markers in the given bounding box.
     * @param name name to search for, may be null or empty string
     * @param south longitude of southern edge of bounding box
     * @param west latitude of western edge of bounding box
     * @param north longitude of northern edge of bounding box
     * @param east latitude of eastern edge of bounding box
     * @param maxResultCount maximum number of results to return
     * @param escapeHtml HTML-escape the resulting POI names
     * @return Array of SearchMarkers in the given bounding box (matching name, if specified)
     */
    public native SearchMarker[] getSearchMarkers(String name, double south, double west, double north, double east, int maxResultCount, boolean escapeHtml);

    /**
     * Set content of HTML &lt;head&gt; tag to be used in rendered HTML.  If not called, default CSS will be used.
     * @param headContent content of HTML &lt;head&gt; tag, including CSS style values
     */
    public native void setHeadContent(String headContent);

    /**
     * Set prefix to be added to all icon URLs in rendered HTML.  If not called, no prefix will be added.
     * <p>
     * This is provided in case a prefix needs to be specified for icon images to be located
     * correctly.  On Android a special URL is used to indicate the icon will be be loaded from .aar
     * assets.
     * @param imagePrefix content of image prefix
     */
    public native void setImagePrefix(String imagePrefix);

    /**
     * Set language to use when rendering HTML.
     * <p>
     * American English will be used by default if no translation is available in the specified
     * language.
     * @param languageCode language code for the desired language.
     */
    public native void setLanguage(String languageCode);

    /**
     * Specify format to render coordinates in.
     * @param coordinateFormat desired coordinate format.
     */
    public native void setCoordinateFormat(CoordinateFormatType coordinateFormat);

    /**
     * Specify format to render dates in.
     * @param dateFormat desired coordinate format.
     */
    public native void setDateFormat(DateFormatType dateFormat);

    /**
     * Specify units to render distances in.
     * @param distanceUnit desired distance unit.
     */
    public native void setDistanceUnit(DistanceUnit distanceUnit);

    /**
     * Process response body from POST api/v2/points-of-interest endpoint.  Only call this if API call was successful.
     * @param json response body content
     * @return id of newly created marker
     */
    public native long processCreateMarkerResponse(String json);

    /**
     * Process response body from PUT api/v2/points-of-interest/{id}/location endpoint.  Only call this if API call was successful.
     * @param json response body content
     */
    public native void processMoveMarkerResponse(String json);

    /**
     * Process response body from GET api/v2/points-of-interest/sync endpoint.  Only call this if API call was successful.
     * @param json response body content
     * @param tileX tile X coordinate, valid values are 0-15
     * @param tileY tile Y coordinate, valid values are 0-15
     * @return number of markers processed
     */
    public native int processSyncMarkersResponse(String json, int tileX, int tileY);

    /**
     * Process response body from GET api/v2/reviews/sync endpoint.  Only call this if API call was successful.
     * @param json response body content
     * @param tileX tile X coordinate, valid values are 0-15
     * @param tileY tile Y coordinate, valid values are 0-15
     * @return number of reviews processed
     */
    public native int processSyncReviewsResponse(String json, int tileX, int tileY);

    /**
     * Process response body from POST api/v2/reviews/{id}/votes endpoint.  Only call this if API call was successful.
     * @param json response body content
     */
    public native void processVoteForReviewResponse(String json);

    /**
     * Process response content from a webview call.
     * @param json response body content
     */
    public native void processWebViewResponse(String json);

    /**
     * Parse an acdb:// URL.
     * <p>
     * To render a marker with given id, call this function with "acdb://summary/{id}".  Rendered
     * HTML will include acdb:// links to marker's photo list, section details, review list, etc.
     * @param url acdb:// URL the user selected.
     * @param captainName user's captain name
     * @param pageSize review list page size
     * @return AcdbUrlAction for the specified URL.  Content will be initialized based on the action type.
     */
    public native AcdbUrlAction parseAcdbUrl(String url, String captainName, int pageSize);

    static {
        System.loadLibrary("activecaptaincommunitysdk");
    }

    /**
     * Constructor, will initialize native code.
     * @param databaseFile path to SQLite database.  SQLite file may not exist until the first tile
     *                     has been downloaded and installed.
     * @param languageCode language to be used when rendering HTML
     */
    public ActiveCaptainDatabase(File databaseFile, String languageCode) {
        if (databaseFile == null)
        {
            throw new IllegalArgumentException("databaseFile must not be null.");
        }

        if (languageCode == null)
        {
            throw new IllegalArgumentException("language code must not be null.");
        }

        init(databaseFile.getPath(), languageCode);
    }

    /**
     * Close database and clean up dynamic memory used by native code.
     */
    @Override
    public void close() throws IOException {
        cleanup();
        this.ptrHolder = 0;
    }

    /**
     * Finalize, close database and clean up dynamic memory used by native code.
     */
    @Override
    public void finalize() {
        cleanup();
        this.ptrHolder = 0;
    }
}
