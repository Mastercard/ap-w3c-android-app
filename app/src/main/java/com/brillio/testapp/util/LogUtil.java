/*
 * Copyright (c) 2021 Mastercard
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.brillio.testapp.util;

import android.util.Log;

import com.brillio.testapp.BuildConfig;

public class LogUtil {

    private LogUtil() {
    }

    /**
     * Prints the debug / verbose message in logcat
     *
     * @param logTag
     * @param message
     */
    public static void debug(String logTag, String message) {
        if (BuildConfig.ENABLE_LOGGING) {
            Log.d(logTag, message);
        }
    }

    /**
     * Prints the error message in logcat
     *
     * @param logTag  Tag for referance
     * @param message Message text to show
     */
    public static void error(String logTag, String message) {
        if (BuildConfig.ENABLE_LOGGING) {
            Log.e(logTag, message);
        }
    }

    public static void logLargeString(String logTag, String str) {
        if (str.length() > 3000) {
            Log.i(logTag, str.substring(0, 3000));
            logLargeString(logTag, str.substring(3000));
        } else {
            Log.i(logTag, str); // continuation
        }
    }
}
