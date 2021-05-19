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

package com.garmin.marine.activecaptaincommunitysdk.DTO;

public class SearchMarker {
    long markerId;
    String name;
    MarkerType markerType;
    double latitude;
    double longitude;
    MapIconType mapIcon;

    public SearchMarker(long markerId, String name, MarkerType markerType, double latitude, double longitude, MapIconType mapIcon) {
        this.markerId = markerId;
        this.name = name;
        this.markerType = markerType;
        this.latitude = latitude;
        this.longitude = longitude;
        this.mapIcon = mapIcon;
    }

    public long getId() {
        return markerId;
    }

    public String getName() {
        return name;
    }

    public MapIconType getMapIcon() {
        return mapIcon;
    }
}
