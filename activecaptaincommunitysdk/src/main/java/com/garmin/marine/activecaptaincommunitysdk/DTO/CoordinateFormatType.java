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

public enum CoordinateFormatType {
    /**
     * Decimal degrees: 0.0000&deg;N, 0.0000&deg;E
     */
    DEC_DEG,

    /**
     * Degrees/minutes: 00&deg;000000.000'N, 00&deg;000000.000'E
     */
    DEG_MIN,

    /**
     * Degrees/minutes/seconds: 00&deg;00'00.0000&quot;N, 00&deg;00'00.0000&quot;E
     */
    DEG_MIN_SEC
}
