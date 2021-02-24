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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.brillio.testapp.BuildConfig;

import static android.content.Context.MODE_PRIVATE;

public class PrefsHelper {

    private static final String PREFS_NAME = "mastercard";
    public static final String KEY_REG_MSG = "reg_message";
    public static final String KEY_BTN_LABEL = "btn_label";
    public static final String KEY_CHROME = "chrome";
    public static final String KEY_EDGE = "edge";
    public static final String KEY_FIREFOX = "firefox";
    public static final String KEY_FINGERPRINT_PERMISSION = "fingerprint_permission";
    static final String PAYMENT_RESPONSE_DETAILS = "paymentResponseDetails";
    static final String ENCRYPTED_PAYMENT_RESPONSE = "encryptedPaymentResponse";

    private PrefsHelper() {
    }

    private static String getDefCred() {
        String walletID = BuildConfig.WalletId;
        return "{\"details\":{\"cardNumber\":\"5238795670876343\",\"cvv\":\"977\",\"expiry\":\"09/24\",\"transactionSource\":\"mcpba\",\"paymentType\":\"mcpba\",\"accountNumber\":\"**** **** **** 9118\",\"walletID\":\"" + walletID + "\"}}";
    }

    public static void setencryptedPaymentResponse(String encryptedPaymentResponse, final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putString(ENCRYPTED_PAYMENT_RESPONSE, encryptedPaymentResponse)
                .apply();
    }

    public static String getencryptedPaymentResponse(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getString(ENCRYPTED_PAYMENT_RESPONSE, "");
    }

    public static void setPaymentResonseDetails(String paymentResponseDetails, final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putString(PAYMENT_RESPONSE_DETAILS, paymentResponseDetails)
                .apply();
    }

    public static String getPaymentResonseDetails(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getString(PAYMENT_RESPONSE_DETAILS, getDefCred());
    }

    public static void saveString(Context context, String value, String key) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static String getString(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(key, "");
    }

    public static void saveBoolean(Context context, boolean value, String key) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static boolean getBoolean(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(key, false);
    }

    public static boolean isContains(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.contains(key);
    }

    public static SharedPreferences getInstance(Context context) {
        return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }
}
