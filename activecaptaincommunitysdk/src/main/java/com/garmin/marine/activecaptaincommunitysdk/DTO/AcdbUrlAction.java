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

public class AcdbUrlAction {
    public enum ActionType
    {
        /**
         * Unknown or invalid action.
         */
        UNKNOWN,

        /**
         * Edit a marker section or review.
         */
        EDIT,

        /**
         * Report review as violating terms and conditions.
         */
        REPORT_REVIEW,

        /**
         * See all details for a given section.
         */
        SEE_ALL,

        /**
         * View business photos.
         */
        SHOW_PHOTOS,

        /**
         * View marker summary page.
         */
        SHOW_SUMMARY,

        /**
         * Vote for a review as helpful.
         */
        VOTE_REVIEW
    };

    public AcdbUrlAction(ActionType action, String content) {
        this.action = action;
        this.content = content;
    }

    /**
     * Action parsed from the acdb:// URL.
     */
    public ActionType action;

    /**
     * Content generated when parsing an acdb:// URL.  Value will depend on action type.
     * <p>
     * If action is SEE_ALL, SEE_PHOTOS, or SHOW_SUMMARY, content will be rendered HTML.
     * <p>
     * If action is EDIT or REPORT_REVIEW, content will be a webview URL.  The base website URL
     * ("https://activecaptain.garmin.com/") must be prepended to this URL.  Version and locale
     * must be appended as a query string.  apikey and Authorization (user's JWT) headers must be
     * set.
     * <p>
     * If action is VOTE_REVIEW, content will contain a review ID.  Must be parsed as a long integer
     * (64-bit).
     */
    public String content;
}
