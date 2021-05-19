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

public class ActiveCaptainConfiguration {
    static final String API_BASE_URL = "https://activecaptain-stage.garmin.com/community/thirdparty/";
    static final String API_KEY = "STAGE_API_KEY_HERE";
    static final String SSO_URL = "https://ssotest.garmin.com/sso/embed?clientId=ACTIVE_CAPTAIN_WEB&locale=en_US";
    static final int MARKER_MAX_SEARCH_RESULTS = 100;
    static final int REVIEW_LIST_PAGE_SIZE = 10;
    static final int UPDATE_INTERVAL_MINS = 15;  // in minutes, must be >= 15
    static final String WEBVIEW_BASE_URL = "https://activecaptain-stage.garmin.com";
    static final boolean WEBVIEW_DEBUG = true;

    static String languageCode = "en_US";
}
