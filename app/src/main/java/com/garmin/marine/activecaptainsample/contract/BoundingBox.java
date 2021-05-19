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

package com.garmin.marine.activecaptainsample.contract;

import com.google.gson.annotations.SerializedName;

public class BoundingBox {
    public static class Coordinate {
        @SerializedName("latitude")
        public double latitude;

        @SerializedName("longitude")
        public double longitude;

        public Coordinate(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    @SerializedName("northeastCorner")
    public Coordinate northeastCorner;

    @SerializedName("southwestCorner")
    public Coordinate southwestCorner;

    public BoundingBox(Coordinate southwestCorner, Coordinate northeastCorner) {
        this.southwestCorner = southwestCorner;
        this.northeastCorner = northeastCorner;
    }

    public BoundingBox(double minLat, double minLon, double maxLat, double maxLon) {
        southwestCorner = new Coordinate(minLat, minLon);
        northeastCorner = new Coordinate(maxLat, maxLon);
    }
}
