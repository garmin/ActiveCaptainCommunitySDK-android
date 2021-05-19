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

public enum DateFormatType {
    /**
     * Abbreviated month, abbreviation will be translated: 31-DEC-2019
     */
    MONTH_ABBR,

    /**
     * Day/month/year: 31/12/2019
     */
    DMY_SLASH,

    /**
     * Month/day/year: 12/31/2019
     */
    MDY_SLASH,

    /**
     * Day-month-year: 31-12-2019
     */
    DMY_DASH,

    /**
     * Month-day-year: 12-31-2019
     */
    MDY_DASH
}
